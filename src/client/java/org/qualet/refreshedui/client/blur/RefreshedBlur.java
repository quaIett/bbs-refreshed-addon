package org.qualet.refreshedui.client.blur;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import org.qualet.refreshedui.RefreshedUiAddon;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

/**
 * "Refreshed Blur": dual Kawase (Bjorge, ARM 2015) over the main framebuffer, standing in for the box blur
 * of BBS's {@code InterfaceBlur} (see {@code InterfaceBlurMixin}). The screen is downsampled through a chain
 * of half-size targets with a 5-tap filter and upsampled back with an 8-tap one, so almost all the work
 * happens at a fraction of the resolution — a smoother, Gaussian-like result for far fewer texture reads.
 *
 * <p>MC 1.21.11: runs in the GUI renderer's blur slot (BBS's {@code InterfaceBlur.render}), when the layers
 * under the marked one are already composited. The passes are driven directly through the GPU device rather
 * than a {@code PostEffectProcessor}: a processor bakes its uniform values when it is built, and the tap
 * offset changes with the radius setting and every frame of an overlay's close animation. The two pipelines
 * are built once; the offset lives in a small mapped uniform buffer and each level's sizes in a static one.</p>
 */
public final class RefreshedBlur
{
    private static final RenderPipeline DOWN = pipeline("kawase_down");
    private static final RenderPipeline UP = pipeline("kawase_up");

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
    /** SamplerInfo (OutSize, InSize) of down[i]: chain[i] into chain[i + 1], and up[i]: chain[i + 1] into chain[i]. */
    private static GpuBuffer[] downInfo;
    private static GpuBuffer[] upInfo;
    /** KawaseConfig: the tap offset, rewritten every blurred frame. */
    private static GpuBuffer config;
    private static int levels;
    private static int width;
    private static int height;

    /** Set when the chain failed to build or draw, so a broken shader costs one stack trace and BBS's blur takes over. */
    private static boolean broken;

    /** Below this standard deviation the blur is invisible; drawing it would only cost passes. */
    private static final float MIN_SIGMA = 0.5F;

    /**
     * Multiplier on the blur strength (1 = full). Temporary fix for BBS removing the blur at once when an overlay
     * closes: {@code UIOverlayMixin} sets it to the overlay's visibility while the close animation plays, so the
     * blur thins out together with the panel and the dimming.
     */
    private static float strength = 1F;

    /** {@link #strength} as it was when the frame's blur layer was marked — the blur itself runs after render returns. */
    private static float markedStrength = 1F;

    private RefreshedBlur()
    {}

    public static void beginStrength(float value)
    {
        strength = Math.max(0F, Math.min(1F, value));
    }

    public static void endStrength()
    {
        strength = 1F;
    }

    /** Called where BBS marks the blur layer; the latest mark of the frame wins, as it does in BBS. */
    public static void mark()
    {
        markedStrength = strength;
    }

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
            if (!rebuild(main))
            {
                return false;
            }
        }

        /* The main target can be swapped out from under us (BBS widens that field); read it every frame */
        chain[0] = main;

        /* BBS's box of half-width R is 2R + 1 wide: sigma^2 = ((2R + 1)^2 - 1) / 12 */
        float target = (float) Math.sqrt(radius * (radius + 1) / 3D) * markedStrength;

        if (target < MIN_SIGMA)
        {
            /* Nothing visible left to draw, but the blur still counts as done: BBS must not draw its own */
            return true;
        }

        int n = pickLevels(target);
        float offset = pickOffset(n, target);

        try
        {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

            try (GpuBuffer.MappedView view = encoder.mapBuffer(config, false, true))
            {
                Std140Builder.intoBuffer(view.data()).putFloat(offset);
            }

            GpuSampler linear = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

            for (int i = 0; i < n; i++)
            {
                run(encoder, DOWN, chain[i], chain[i + 1], downInfo[i], linear);
            }

            for (int i = n - 1; i >= 0; i--)
            {
                run(encoder, UP, chain[i + 1], chain[i], upInfo[i], linear);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();

            close();
            broken = true;

            return false;
        }

        return true;
    }

    private static void run(CommandEncoder encoder, RenderPipeline pipeline, Framebuffer input, Framebuffer output, GpuBuffer info, GpuSampler sampler)
    {
        GpuTextureView in = input.getColorAttachmentView();
        GpuTextureView out = output.getColorAttachmentView();

        if (in == null || out == null)
        {
            return;
        }

        try (RenderPass pass = encoder.createRenderPass(() -> "Refreshed blur", out, OptionalInt.empty()))
        {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("SamplerInfo", info);
            pass.setUniform("KawaseConfig", config);
            pass.bindTexture("InSampler", in, sampler);
            pass.draw(0, 3);
        }
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
    private static boolean rebuild(Framebuffer main)
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
            downInfo = new GpuBuffer[count];
            upInfo = new GpuBuffer[count];
            chain[0] = main;

            w = main.textureWidth;
            h = main.textureHeight;

            for (int i = 1; i <= count; i++)
            {
                w /= 2;
                h /= 2;
                chain[i] = new SimpleFramebuffer("refreshedui_blur_" + i, w, h, false);
            }

            for (int i = 0; i < count; i++)
            {
                downInfo[i] = samplerInfo(chain[i + 1], chain[i]);
                upInfo[i] = samplerInfo(chain[i], chain[i + 1]);
            }

            config = RenderSystem.getDevice().createBuffer(() -> "Refreshed blur config", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, 16L);

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

    private static GpuBuffer samplerInfo(Framebuffer output, Framebuffer input)
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            ByteBuffer data = Std140Builder.onStack(stack, 16)
                .putVec2(output.textureWidth, output.textureHeight)
                .putVec2(input.textureWidth, input.textureHeight)
                .get();

            return RenderSystem.getDevice().createBuffer(() -> "Refreshed blur sampler info", GpuBuffer.USAGE_UNIFORM, data);
        }
    }

    private static RenderPipeline pipeline(String fragment)
    {
        return RenderPipeline.builder(RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET)
            .withLocation(Identifier.of("refreshedui", "pipeline/" + fragment))
            .withVertexShader(Identifier.ofVanilla("core/screenquad"))
            .withFragmentShader(Identifier.of("refreshedui", "post/" + fragment))
            .withSampler("InSampler")
            .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
            .withUniform("KawaseConfig", UniformType.UNIFORM_BUFFER)
            .build();
    }

    private static void close()
    {
        if (downInfo != null)
        {
            for (int i = 0; i < downInfo.length; i++)
            {
                if (downInfo[i] != null) downInfo[i].close();
                if (upInfo[i] != null) upInfo[i].close();
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

        if (config != null)
        {
            config.close();
        }

        chain = null;
        downInfo = null;
        upInfo = null;
        config = null;
        levels = 0;
    }
}
