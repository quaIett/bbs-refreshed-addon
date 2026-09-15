package org.qualet.refreshedui.client.anim;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import mchorse.bbs_mod.ui.framework.UIContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

/**
 * Off-screen compositing for the {@link OverlayReveal} fade: the overlay panel is drawn at full opacity
 * into a copy of the screen, and that copy is then blended back over the screen at the reveal's
 * visibility — the panel fades as one flat picture, the way CSS {@code opacity} fades a whole element.
 *
 * <p>Fading every draw call separately (through the shader colour) is wrong for a panel, which is
 * layers: the rounded frame's border sits <em>under</em> its fill, the icon strip over the panel, hover
 * boxes over that, text over everything. At half alpha each layer shows through the one above it — the
 * primary-coloured border tints the whole panel, icons (already faded once through their vertex colour)
 * fade twice and pop in late. A snapshot has no layers left to leak.</p>
 *
 * <p>The copy is taken of the screen as it is when the panel starts rendering (dimmed backdrop and blur
 * included), so what the panel draws composes over exactly what lies under it; blending the finished
 * copy back at alpha {@code a} yields {@code screen * (1 - a) + copy * a} and leaves the rest of the
 * screen untouched since it is identical in both. The blend runs on constant factors — the copy's own
 * alpha channel is whatever the UI left in it and takes no part.</p>
 *
 * <p>One snapshot target is kept, sized to the main framebuffer (depth included, for the 3D previews
 * some panels hold) and rebuilt on resize. Should it fail to build, {@link #begin} keeps returning false
 * and the caller falls back to the shader-colour fade.</p>
 */
public final class OverlaySnapshot
{
    private static Framebuffer snapshot;
    private static int width;
    private static int height;

    /** Set when the target failed to build, so a broken driver costs one stack trace, not one per frame. */
    private static boolean broken;
    /** True between {@link #begin} and {@link #end}; a nested capture is refused (it draws into the open one). */
    private static boolean capturing;

    private OverlaySnapshot()
    {}

    /**
     * Start capturing: everything drawn until {@link #end} lands in a copy of the screen instead of on
     * it. Returns false (and captures nothing) when the snapshot is unavailable or one is already open.
     */
    public static boolean begin(UIContext context)
    {
        if (broken || capturing)
        {
            return false;
        }

        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();

        if (snapshot == null || width != main.textureWidth || height != main.textureHeight)
        {
            if (!rebuild(main))
            {
                return false;
            }
        }

        /* Whatever is still pending belongs to the screen, not to the panel */
        context.batcher.flush();

        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);

        if (scissor)
        {
            GlStateManager._disableScissorTest();
        }

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, snapshot.fbo);
        GlStateManager._glBlitFrameBuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);

        if (scissor)
        {
            GlStateManager._enableScissorTest();
        }

        snapshot.beginWrite(true);
        capturing = true;

        return true;
    }

    /** Stop capturing and blend the copy over the screen at {@code alpha} (0 = screen as it was, 1 = copy). */
    public static void end(UIContext context, float alpha)
    {
        if (!capturing)
        {
            return;
        }

        capturing = false;

        /* The panel's last quads and text go into the copy too */
        context.batcher.flush();

        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();

        main.beginWrite(true);

        /* Framebuffer.draw swaps in its own pixel-space projection and resets depth state; put them back */
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorter sorting = RenderSystem.getVertexSorting();
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);

        if (scissor)
        {
            GlStateManager._disableScissorTest();
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL14.GL_CONSTANT_ALPHA, GL14.GL_ONE_MINUS_CONSTANT_ALPHA);
        GL14.glBlendColor(1F, 1F, 1F, Math.max(0F, Math.min(1F, alpha)));

        snapshot.draw(width, height, false);

        GL14.glBlendColor(0F, 0F, 0F, 0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setProjectionMatrix(projection, sorting);
        RenderSystem.depthMask(depthMask);

        if (depthTest)
        {
            RenderSystem.enableDepthTest();
        }
        else
        {
            RenderSystem.disableDepthTest();
        }

        if (scissor)
        {
            GlStateManager._enableScissorTest();
        }
    }

    /** (Re)create the target at the main framebuffer's size. Leaves main bound for writing either way. */
    private static boolean rebuild(Framebuffer main)
    {
        if (snapshot != null)
        {
            snapshot.delete();
            snapshot = null;
        }

        try
        {
            snapshot = new SimpleFramebuffer(main.textureWidth, main.textureHeight, true, MinecraftClient.IS_SYSTEM_MAC);
            width = main.textureWidth;
            height = main.textureHeight;

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();

            snapshot = null;
            broken = true;

            return false;
        }
        finally
        {
            /* Building a framebuffer clears it, which ends with the window (not main) bound */
            main.beginWrite(true);
        }
    }
}
