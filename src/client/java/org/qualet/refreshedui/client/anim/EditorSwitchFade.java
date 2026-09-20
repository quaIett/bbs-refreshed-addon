package org.qualet.refreshedui.client.anim;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.UIFilmPreview;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fc;
import org.qualet.refreshedui.client.batcher.GuiTexturedMesh;

/**
 * Switching the Film panel between the camera editor and the replay editor fades instead of cutting.
 *
 * <p>BBS swaps the editors within one frame: {@code showPanel} hides one editor and shows the other, and
 * when each editor is bound to its own dock layout it rebuilds the whole tree, so every panel may land
 * somewhere else. Here the frame that is on screen at the moment of the switch is copied first, and the
 * switch then happens at once — input already goes to the new editor. For {@link #DURATION_MS} the copy
 * is laid over the dock and fades out, uncovering the new editor and layout underneath.</p>
 *
 * <p>The copy is taken straight from the main framebuffer: {@code showPanel} is only ever called from
 * input and key handlers, between frames, when that framebuffer still holds the last finished frame. A
 * call from inside a frame, or while the panel was not on screen a moment ago, just switches as before.
 * On 1.21.11 that copy is a GPU texture-to-texture copy, and it is laid back over the dock as recorded
 * quads rather than an immediate blend — see {@link #drawCopy}.</p>
 *
 * <p>The preview never fades: it is the live world. Its picture is drawn through {@link #drawPreview}
 * gliding from where the old frame had it to where the new editor puts it, and the copy leaves a hole
 * exactly where the live picture is, so the picture stays fully opaque the whole way. The picture keeps
 * the world's scale rather than being stretched: its height follows the glide and its width keeps the
 * new aspect. Switching editors can change that aspect — the camera editor previews at the export
 * aspect, the replay editor fills the panel — and a stretched frame would squash the world for a moment;
 * this way the parts that only one of the two frames shows fade in or out at the sides instead.</p>
 */
public final class EditorSwitchFade
{
    public static final long DURATION_MS = 240L;
    /** The last frame shows the panel only if the panel drew this recently. */
    private static final long RECENT_FRAME_MS = 250L;

    /** Copy of the frame from just before the switch; kept between switches and rebuilt on resize. */
    private static Framebuffer snapshot;
    /** Set when the copy failed to build, so a broken driver costs one stack trace, not one per switch. */
    private static boolean broken;

    /** Panel whose switch is fading, null when none is. */
    private static UIFilmPanel owner;
    private static long start;

    /* Where the live preview picture started from: the drawn picture and the panel area clipping it */
    private static boolean glide;
    private static float fromX, fromY, fromW, fromH;
    private static float fromClipX1, fromClipY1, fromClipX2, fromClipY2;

    /* Where the picture was drawn in the last frame, for a switch that interrupts a running one */
    private static boolean drawn;
    private static float drawnX, drawnY, drawnW, drawnH;
    private static float drawnClipX1, drawnClipY1, drawnClipX2, drawnClipY2;

    /* The live picture's rectangle this frame: the copy is not laid over it */
    private static boolean hole;
    private static float holeX1, holeY1, holeX2, holeY2;

    /** Panel currently inside its render, null between frames. */
    private static UIFilmPanel rendering;
    private static UIFilmPanel lastPanel;
    private static long lastFrame;

    private EditorSwitchFade()
    {}

    /** Film panel render starts. */
    public static void beginFrame(UIFilmPanel panel)
    {
        rendering = panel;
        lastPanel = panel;
        lastFrame = Tween.now();
        hole = false;
    }

    /** Film panel render ends: lay the fading copy over the dock, around the live preview picture. */
    public static void endFrame(UIContext context, UIFilmPanel panel)
    {
        rendering = null;

        if (owner != panel)
        {
            return;
        }

        float progress = progress();

        if (progress >= 1F)
        {
            finish();
        }
        else
        {
            drawCopy(context, panel.dock.area, 1F - progress);
        }

        hole = false;
    }

    /**
     * {@code showPanel} is about to switch {@code panel} to the other editor: copy the frame on screen and
     * start the fade. Does nothing (the switch just cuts) when animations are off or the last frame does
     * not show this panel.
     */
    public static void capture(UIFilmPanel panel)
    {
        if (!Animations.enabled() || rendering != null || panel != lastPanel
            || Tween.now() - lastFrame > RECENT_FRAME_MS || panel.getData() == null)
        {
            finish();

            return;
        }

        UIFilmPreview preview = panel.preview;

        if (owner == panel && drawn && progress() < 1F)
        {
            /* A switch in the middle of a fade picks the picture up where it is on screen right now */
            glide = true;
            fromX = drawnX;
            fromY = drawnY;
            fromW = drawnW;
            fromH = drawnH;
            fromClipX1 = drawnClipX1;
            fromClipY1 = drawnClipY1;
            fromClipX2 = drawnClipX2;
            fromClipY2 = drawnClipY2;
        }
        else
        {
            Area viewport = preview.getViewport();

            glide = preview.canBeSeen() && viewport.w >= 2 && viewport.h >= 2;
            fromX = viewport.x;
            fromY = viewport.y;
            fromW = viewport.w;
            fromH = viewport.h;
            fromClipX1 = preview.area.x;
            fromClipY1 = preview.area.y;
            fromClipX2 = preview.area.ex();
            fromClipY2 = preview.area.ey();
        }

        if (!copyScreen())
        {
            finish();

            return;
        }

        owner = panel;
        start = Tween.now();
        drawn = false;
    }

    /**
     * Draw the preview's world picture for this frame. Returns false when no switch is fading, and the
     * caller draws the picture at its own place as usual. The arguments are the ones the preview would
     * have drawn with: its final rectangle and the texture region.
     */
    public static boolean drawPreview(UIFilmPreview preview, Batcher2D batcher, int texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        if (owner == null || owner.preview != preview || !glide || w <= 0F || h <= 0F)
        {
            return false;
        }

        float p = progress();

        if (p >= 1F)
        {
            return false;
        }

        /* The world keeps its scale: the height follows the glide, the width keeps the picture's aspect */
        float gy = lerp(fromY, y, p);
        float gh = lerp(fromH, h, p);
        float gw = gh * (w / h);
        float gx = lerp(fromX + fromW / 2F, x + w / 2F, p) - gw / 2F;

        Area area = preview.area;
        float cx1 = lerp(fromClipX1, area.x, p);
        float cy1 = lerp(fromClipY1, area.y, p);
        float cx2 = lerp(fromClipX2, area.ex(), p);
        float cy2 = lerp(fromClipY2, area.ey(), p);

        drawn = true;
        drawnX = gx;
        drawnY = gy;
        drawnW = gw;
        drawnH = gh;
        drawnClipX1 = cx1;
        drawnClipY1 = cy1;
        drawnClipX2 = cx2;
        drawnClipY2 = cy2;

        float x1 = Math.max(gx, cx1);
        float y1 = Math.max(gy, cy1);
        float x2 = Math.min(gx + gw, cx2);
        float y2 = Math.min(gy + gh, cy2);

        if (x2 <= x1 || y2 <= y1)
        {
            hole = false;

            return true;
        }

        /* Crop through the texture coordinates rather than a scissor, which would snap to whole pixels */
        float su = (u2 - u1) / gw;
        float sv = (v2 - v1) / gh;

        batcher.texturedBox(texture, color, x1, y1, x2 - x1, y2 - y1,
            u1 + (x1 - gx) * su, v1 + (y1 - gy) * sv, u1 + (x2 - gx) * su, v1 + (y2 - gy) * sv, textureW, textureH);

        hole = true;
        holeX1 = x1;
        holeY1 = y1;
        holeX2 = x2;
        holeY2 = y2;

        return true;
    }

    /** Eased progress of the running fade, 1 when none is running. */
    private static float progress()
    {
        if (owner == null)
        {
            return 1F;
        }

        float t = (Tween.now() - start) / (float) DURATION_MS;

        return t >= 1F ? 1F : Easings.outCubic(t);
    }

    private static void finish()
    {
        owner = null;
        glide = false;
        drawn = false;
        hole = false;
    }

    private static float lerp(float a, float b, float t)
    {
        return a + (b - a) * t;
    }

    /**
     * Blend the copy over the dock at {@code alpha}, leaving out the live preview picture.
     *
     * <p>1.21.11 records the interface and composites it after the screen's render returns, so the copy
     * cannot be blended in place with a constant-alpha blend the way the other branches do. It is
     * recorded as ordinary textured quads instead, with the fade carried in their vertex colour. That
     * multiplies the copy's own alpha channel, which is fine here: the picture comes out of the main
     * framebuffer, whose alpha the game keeps at 1.</p>
     */
    private static void drawCopy(UIContext context, Area dock, float alpha)
    {
        if (snapshot == null || alpha <= 0F)
        {
            return;
        }

        DrawContext draw = context.batcher.getContext();
        Matrix3x2fc matrix = draw.getMatrices();
        GuiTexturedMesh mesh = new GuiTexturedMesh();
        int color = Colors.setA(Colors.WHITE, Math.min(1F, alpha));

        float x1 = dock.x;
        float y1 = dock.y;
        float x2 = dock.ex();
        float y2 = dock.ey();

        if (!hole)
        {
            copyQuad(mesh, matrix, x1, y1, x2, y2, color);
        }
        else
        {
            float hx1 = clamp(holeX1, x1, x2);
            float hy1 = clamp(holeY1, y1, y2);
            float hx2 = clamp(holeX2, x1, x2);
            float hy2 = clamp(holeY2, y1, y2);

            copyQuad(mesh, matrix, x1, y1, x2, hy1, color);
            copyQuad(mesh, matrix, x1, hy2, x2, y2, color);
            copyQuad(mesh, matrix, x1, hy1, hx1, hy2, color);
            copyQuad(mesh, matrix, hx2, hy1, x2, hy2, color);
        }

        /* NEAREST: the copy is laid back over the screen at the scale it was taken at. */
        mesh.draw(draw, RenderPipelines.GUI_TEXTURED, snapshot.getColorAttachmentView(),
            RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
    }

    /** The part of the copy under the interface rectangle (x1, y1)-(x2, y2), recorded at that rectangle. */
    private static void copyQuad(GuiTexturedMesh mesh, Matrix3x2fc matrix, float x1, float y1, float x2, float y2, int color)
    {
        if (x2 <= x1 || y2 <= y1)
        {
            return;
        }

        float scale = (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
        float w = snapshot.textureWidth;
        float h = snapshot.textureHeight;

        float u1 = x1 * scale / w;
        float u2 = x2 * scale / w;
        /* Framebuffer rows run bottom-up */
        float v1 = 1F - y1 * scale / h;
        float v2 = 1F - y2 * scale / h;

        mesh.vertex(matrix, x1, y1).texture(u1, v1).color(color);
        mesh.vertex(matrix, x1, y2).texture(u1, v2).color(color);
        mesh.vertex(matrix, x2, y2).texture(u2, v2).color(color);
        mesh.vertex(matrix, x2, y1).texture(u2, v1).color(color);
    }

    private static float clamp(float value, float min, float max)
    {
        return value < min ? min : (value > max ? max : value);
    }

    /** Copy the main framebuffer (the last finished frame) into {@link #snapshot}. */
    private static boolean copyScreen()
    {
        if (broken)
        {
            return false;
        }

        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();

        try
        {
            int w = main.textureWidth;
            int h = main.textureHeight;

            if (w <= 0 || h <= 0)
            {
                return false;
            }

            if (snapshot == null || snapshot.textureWidth != w || snapshot.textureHeight != h)
            {
                if (snapshot != null)
                {
                    snapshot.delete();
                    snapshot = null;
                }

                snapshot = new SimpleFramebuffer("refreshedui_editor_switch", w, h, false);
            }

            RenderSystem.getDevice().createCommandEncoder()
                .copyTextureToTexture(main.getColorAttachment(), snapshot.getColorAttachment(), 0, 0, 0, 0, 0, w, h);

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();

            if (snapshot != null)
            {
                snapshot.delete();
                snapshot = null;
            }

            broken = true;

            return false;
        }
    }
}
