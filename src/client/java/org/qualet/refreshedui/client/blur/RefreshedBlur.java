package org.qualet.refreshedui.client.blur;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.qualet.refreshedui.RefreshedUiAddon;

/**
 * "Refreshed Blur": dual Kawase (Bjorge, ARM 2015) over the main framebuffer, standing in for the box blur
 * of BBS's {@code InterfaceBlur} (see {@code InterfaceBlurMixin}). The screen is downsampled through a chain
 * of half-size targets with a 5-tap filter and upsampled back with an 8-tap one, so almost all the work
 * happens at a fraction of the resolution — a smoother, Gaussian-like result for far fewer texture reads.
 *
 * <p>Passes are vanilla {@link PostEffectPass}es. Two 1.20.4 quirks shape this class:</p>
 * <ul>
 *     <li>post programs are looked up as {@code shaders/program/<name>.json} with no namespace support, so
 *     ours live in the {@code minecraft} namespace under a {@code refreshedui_} prefix;</li>
 *     <li>passes never set a texture filter and framebuffers default to {@code GL_NEAREST}, which would turn
 *     Kawase's in-between taps into blocks — the chain is linear, and so is {@code main} for the one pass
 *     that reads it.</li>
 * </ul>
 */
public final class RefreshedBlur
{
    private static final String DOWN = "refreshedui_kawase_down";
    private static final String UP = "refreshedui_kawase_up";

    private static final int MAX_LEVELS = 5;
    /** A level smaller than this on either side is not worth a pass (and would smear the edges). */
    private static final int MIN_SIZE = 16;

    /** Offsets the {@link #SIGMA} table was sampled at. */
    private static final float[] OFFSETS = {0.5F, 1.0F, 1.5F, 2.0F, 2.5F, 3.0F};
    /** Offsets past this start to show the tap pattern; a stronger blur takes one more level instead. */
    private static final float MAX_CLEAN_OFFSET = 1.5F;
    private static final float MIN_OFFSET = 0.25F;

    /**
     * Blur strength per level count (rows, 1..5) and offset (columns, {@link #OFFSETS}): the standard deviation
     * along one axis of the whole chain's impulse response, in full-resolution pixels. Simulated with bilinear
     * sampling and averaged over two impulse phases.
     */
    private static final float[][] SIGMA = {
        {0.96F, 1.44F, 2.01F, 2.61F, 3.14F, 3.71F},
        {2.14F, 3.23F, 4.50F, 5.85F, 7.04F, 8.35F},
        {4.39F, 6.61F, 9.33F, 11.98F, 14.34F, 17.02F},
        {8.83F, 13.31F, 18.95F, 24.10F, 28.75F, 34.08F},
        {17.68F, 26.65F, 38.17F, 48.27F, 57.48F, 68.07F},
    };

    /** chain[0] is main (not owned); chain[i] is main downsampled i times. */
    private static Framebuffer[] chain;
    /** down[i]: chain[i] into chain[i + 1]; up[i]: chain[i + 1] into chain[i]. */
    private static PostEffectPass[] down;
    private static PostEffectPass[] up;
    private static int levels;
    private static int width;
    private static int height;

    /** Set when the chain failed to build, so a broken shader costs one stack trace and BBS's blur takes over. */
    private static boolean broken;

    private RefreshedBlur()
    {}

    public static boolean enabled()
    {
        return !broken && RefreshedUiAddon.refreshedBlur != null && RefreshedUiAddon.refreshedBlur.get();
    }

    /**
     * Blur the main framebuffer as strongly as BBS's box blur of {@code radius} would (matched by standard
     * deviation). Returns false when nothing was drawn, so the caller can fall back to BBS's own blur.
     */
    public static boolean render(int radius)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer main = mc.getFramebuffer();

        if (chain == null || main.textureWidth != width || main.textureHeight != height)
        {
            if (!rebuild(mc, main))
            {
                return false;
            }
        }

        /* BBS's box of half-width R is 2R + 1 wide: sigma^2 = ((2R + 1)^2 - 1) / 12 */
        float target = (float) Math.sqrt(radius * (radius + 1) / 3D);
        int n = pickLevels(target);
        float offset = pickOffset(n, target);

        int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int mainFilter = main.texFilter;

        try
        {
            /* Blur only changes color: the passes clear their output, and that must not wipe main's depth */
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            main.setTexFilter(GL11.GL_LINEAR);

            for (int i = 0; i < n; i++)
            {
                run(down[i], offset);

                if (i == 0)
                {
                    main.setTexFilter(mainFilter);
                }
            }

            for (int i = n - 1; i >= 0; i--)
            {
                run(up[i], offset);
            }
        }
        finally
        {
            if (main.texFilter != mainFilter)
            {
                main.setTexFilter(mainFilter);
            }

            /* PostEffectPass leaves GL_LEQUAL behind, but BBS paints its UI with GL_ALWAYS */
            main.beginWrite(true);
            RenderSystem.depthMask(depthMask);
            RenderSystem.depthFunc(depthFunction);

            if (depthTest)
            {
                RenderSystem.enableDepthTest();
            }
            else
            {
                RenderSystem.disableDepthTest();
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        }

        return true;
    }

    private static void run(PostEffectPass pass, float offset)
    {
        GlUniform uniform = pass.getProgram().getUniformByName("Offset");

        if (uniform != null)
        {
            uniform.set(offset);
        }

        pass.render(0F);
    }

    /** The fewest levels that reach {@code target} without pushing the offset past {@link #MAX_CLEAN_OFFSET}. */
    private static int pickLevels(float target)
    {
        for (int n = 1; n < levels; n++)
        {
            if (sigma(n, MAX_CLEAN_OFFSET) >= target)
            {
                return n;
            }
        }

        return levels;
    }

    /** Invert the (piecewise linear) sigma row of {@code n} levels; extrapolates past either end of the table. */
    private static float pickOffset(int n, float target)
    {
        float[] row = SIGMA[n - 1];
        int last = OFFSETS.length - 1;
        int i = 0;

        while (i < last - 1 && row[i + 1] < target)
        {
            i++;
        }

        float t = (target - row[i]) / (row[i + 1] - row[i]);
        float offset = OFFSETS[i] + t * (OFFSETS[i + 1] - OFFSETS[i]);

        return Math.max(MIN_OFFSET, Math.min(OFFSETS[last], offset));
    }

    private static float sigma(int n, float offset)
    {
        float[] row = SIGMA[n - 1];

        for (int i = 0; i < OFFSETS.length - 1; i++)
        {
            if (offset <= OFFSETS[i + 1])
            {
                float t = (offset - OFFSETS[i]) / (OFFSETS[i + 1] - OFFSETS[i]);

                return row[i] + t * (row[i + 1] - row[i]);
            }
        }

        return row[row.length - 1];
    }

    /** The chain holds targets sized off the screen, so a resized window means a new one. */
    private static boolean rebuild(MinecraftClient mc, Framebuffer main)
    {
        close();

        try
        {
            int w = main.textureWidth;
            int h = main.textureHeight;
            int count = 0;

            while (count < MAX_LEVELS && w / 2 >= MIN_SIZE && h / 2 >= MIN_SIZE)
            {
                w /= 2;
                h /= 2;
                count++;
            }

            if (count == 0)
            {
                return false;
            }

            chain = new Framebuffer[count + 1];
            down = new PostEffectPass[count];
            up = new PostEffectPass[count];
            chain[0] = main;

            w = main.textureWidth;
            h = main.textureHeight;

            for (int i = 1; i <= count; i++)
            {
                w /= 2;
                h /= 2;

                Framebuffer target = new SimpleFramebuffer(w, h, false, MinecraftClient.IS_SYSTEM_MAC);

                target.setTexFilter(GL11.GL_LINEAR);
                chain[i] = target;
            }

            for (int i = 0; i < count; i++)
            {
                down[i] = pass(mc, DOWN, chain[i], chain[i + 1]);
                up[i] = pass(mc, UP, chain[i + 1], chain[i]);
            }

            levels = count;
            width = main.textureWidth;
            height = main.textureHeight;

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();

            close();
            broken = true;

            return false;
        }
    }

    private static PostEffectPass pass(MinecraftClient mc, String program, Framebuffer input, Framebuffer output) throws Exception
    {
        PostEffectPass pass = new PostEffectPass(mc.getResourceManager(), program, input, output);

        pass.setProjectionMatrix(new Matrix4f().setOrtho(0F, output.textureWidth, 0F, output.textureHeight, 0.1F, 1000F));

        return pass;
    }

    private static void close()
    {
        if (down != null)
        {
            for (int i = 0; i < down.length; i++)
            {
                if (down[i] != null) down[i].close();
                if (up[i] != null) up[i].close();
            }
        }

        if (chain != null)
        {
            /* chain[0] is main, which belongs to the game */
            for (int i = 1; i < chain.length; i++)
            {
                if (chain[i] != null) chain[i].delete();
            }
        }

        chain = null;
        down = null;
        up = null;
        levels = 0;
    }
}
