package org.qualet.refreshedui.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.PixelArt;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.resources.Pixels;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix3x2fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.qualet.refreshedui.client.batcher.CheckerMesh;
import org.qualet.refreshedui.client.batcher.GuiTexturedMesh;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.nio.ByteBuffer;

/**
 * Adds the addon's rounded-rectangle primitives to BBS's {@link Batcher2D} as {@code @Unique}
 * members, exposed through {@link IRoundedBatcher}.
 *
 * <p>Ported verbatim from bbs-fs {@code master-refreshed} (commits ce3cb96e, 1a43104b). The base
 * mod stays clean; consumers cast {@code ((IRoundedBatcher) batcher)}.</p>
 *
 * <h2>Rounded rectangles</h2>
 * Anti-aliased rounded corners without a custom shader: a 64x64 RGBA alpha mask holds one
 * quarter-circle (white RGB, alpha = sub-pixel coverage). A rounded rect is drawn as a 9-slice
 * (TL/top/TR | L/center/R | BL/bottom/BR) in a single POSITION_TEXTURE_COLOR mesh: the four
 * corner cells sample the mask quadrant (UV mirrored per corner so the arc faces outward), and
 * the five edge/center cells sample UV(1,1) — the mask's fully-opaque inner texel — so the body
 * is solid and corner-to-edge joins share the exact same sampler position (no seams).
 *
 * <h2>MC 1.21.11</h2>
 * The GUI is recorded and composited after {@code Screen.render}, so the slices are no longer drawn
 * through a {@code BufferBuilder}: each primitive records one {@link GuiTexturedMesh} (four vertices per
 * cell, vanilla's quad winding) and submits it into the deferred GUI with {@code GUI_TEXTURED}.
 */
@Mixin(Batcher2D.class)
public abstract class Batcher2DMixin implements IRoundedBatcher
{
    @Shadow
    private DrawContext context;

    @Shadow
    public abstract void box(float x1, float y1, float x2, float y2, int color);

    @Shadow
    public abstract void gradientHBox(float x1, float y1, float x2, float y2, int leftColor, int rightColor);

    @Shadow
    public abstract void texturedArea(Texture texture, int color, float x, float y, float w, float h, float u, float v, float tileW, float tileH, int tw, int th);

    @Unique
    private static final float ROUNDED_RECT_MIN_RADIUS = 0.5F;
    @Unique
    private static final int ROUNDED_RECT_MASK_SIZE = 64;
    @Unique
    private static volatile Texture roundedRectMask;

    @Unique
    private static float clampRoundedRectRadius(float w, float h, float radius)
    {
        return Math.min(radius, Math.min(w * 0.5F, h * 0.5F));
    }

    @Unique
    private Matrix3x2fc refreshedui$matrix()
    {
        return this.context.getMatrices();
    }

    @Unique
    private void refreshedui$draw(GuiTexturedMesh mesh, Texture texture)
    {
        mesh.draw(this.context, RenderPipelines.GUI_TEXTURED, texture);
    }

    @Unique
    private Texture getRoundedRectMask()
    {
        Texture cached = roundedRectMask;

        if (cached != null && cached.isValid())
        {
            return cached;
        }

        synchronized (Batcher2D.class)
        {
            cached = roundedRectMask;

            if (cached != null && cached.isValid())
            {
                return cached;
            }

            cached = buildRoundedRectMask(ROUNDED_RECT_MASK_SIZE);
            roundedRectMask = cached;

            return cached;
        }
    }

    @Unique
    private static Texture roundedRectMaskInverted;

    /* Separate texture instead of an inverted blend func: the GUI pipelines carry their own blend state. */
    @Unique
    private Texture getRoundedRectMaskInverted()
    {
        synchronized (Batcher2D.class)
        {
            Texture cached = roundedRectMaskInverted;

            if (cached == null || !cached.isValid())
            {
                cached = buildRoundedRectMask(ROUNDED_RECT_MASK_SIZE, true);
                roundedRectMaskInverted = cached;
            }

            return cached;
        }
    }

    /* Single-quadrant mask: outer arc tip (UV 0,0) -> alpha 0, inner corner texel (UV 1,1) -> alpha 1.
     * Clamp-to-edge is mandatory: edge/center cells sample UV exactly 1.0 and would wrap to alpha 0 under
     * repeat, turning the body semi-transparent (the adopted sampler clamps; the wrap is kept for GL too). */
    @Unique
    private static Texture buildRoundedRectMask(int size)
    {
        return buildRoundedRectMask(size, false);
    }

    /** {@code inverted}: alpha {@code 1 - mask} — opaque OUTSIDE the arc, used to paint only beyond a curve. */
    @Unique
    private static Texture buildRoundedRectMask(int size, boolean inverted)
    {
        Pixels pixels = Pixels.fromSize(size, size);
        ByteBuffer buf = pixels.getBuffer();
        float maxR = size - 1;

        buf.position(0);

        for (int y = 0; y < size; y++)
        {
            for (int x = 0; x < size; x++)
            {
                float dx = maxR - x;
                float dy = maxR - y;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0F, Math.min(1F, maxR - d + 0.5F));
                int alpha = Math.round((inverted ? 1F - a : a) * 255F);

                buf.put((byte) 255);
                buf.put((byte) 255);
                buf.put((byte) 255);
                buf.put((byte) alpha);
            }
        }

        buf.position(0);

        Texture texture = Texture.textureFromPixels(pixels, GL11.GL_LINEAR);

        /* textureFromPixels leaves the target unbound; re-bind before setting the wrap mode or
         * glTexParameteri would target whatever is currently bound and ours stays GL_REPEAT. */
        texture.bind();
        texture.setWrap(GL12.GL_CLAMP_TO_EDGE);
        texture.unbind();
        texture.setClearable(false);

        return texture;
    }

    /* One cell as a quad in vanilla's winding (TL, BL, BR, TR). UVs are passed per corner so callers can
     * mirror the mask quadrant: uXY/vXY, X = 0 left / 1 right, Y = 0 top / 1 bottom. */
    @Unique
    private static void emitMaskQuad(VertexConsumer b, Matrix3x2fc m,
        float x0, float y0, float x1, float y1,
        float u00, float v00, float u10, float v10, float u11, float v11, float u01, float v01,
        int color)
    {
        b.vertex(m, x0, y0).texture(u00, v00).color(color);
        b.vertex(m, x0, y1).texture(u01, v01).color(color);
        b.vertex(m, x1, y1).texture(u11, v11).color(color);
        b.vertex(m, x1, y0).texture(u10, v10).color(color);
    }

    /* Emit one rounded-rect silhouette as up to 9 mask-sampled quads. r must be clamped and >= ROUNDED_RECT_MIN_RADIUS. */
    @Unique
    private static void emitRoundedSliceMask(VertexConsumer b, Matrix3x2fc m,
        float x, float y, float w, float h, float r, int color)
    {
        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;
        float xa = x0 + r;
        float xb = x1 - r;
        float ya = y0 + r;
        float yb = y1 - r;
        boolean hasMidW = xb > xa;
        boolean hasMidH = yb > ya;

        /* Corners: outer corner of each cell -> (0,0); inner corner -> (1,1), U/V mirrored per side. */
        emitMaskQuad(b, m, x0, y0, xa, ya, 0F, 0F, 1F, 0F, 1F, 1F, 0F, 1F, color); /* TL */
        emitMaskQuad(b, m, xb, y0, x1, ya, 1F, 0F, 0F, 0F, 0F, 1F, 1F, 1F, color); /* TR */
        emitMaskQuad(b, m, xb, yb, x1, y1, 1F, 1F, 0F, 1F, 0F, 0F, 1F, 0F, color); /* BR */
        emitMaskQuad(b, m, x0, yb, xa, y1, 0F, 1F, 1F, 1F, 1F, 0F, 0F, 0F, color); /* BL */

        /* Edges + center: every vertex samples (1,1) -> opaque body. */
        if (hasMidW)
        {
            emitMaskQuad(b, m, xa, y0, xb, ya, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
            emitMaskQuad(b, m, xa, yb, xb, y1, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
        }
        if (hasMidH)
        {
            emitMaskQuad(b, m, x0, ya, xa, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
            emitMaskQuad(b, m, xb, ya, x1, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
        }
        if (hasMidW && hasMidH)
        {
            emitMaskQuad(b, m, xa, ya, xb, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
        }
    }

    /* 9-slice with per-side rounding. When a side is not rounded, xa/xb collapse to x0/x1 and the
     * top/bottom strips absorb the square corner region with UV=(1,1) (alpha 1). Lets callers draw
     * "half-pill" caps as one mesh without scissor + plain-box tricks. */
    @Unique
    private static void emitRoundedSliceMaskSides(VertexConsumer b, Matrix3x2fc m,
        float x, float y, float w, float h, float r, int color,
        boolean roundLeft, boolean roundRight)
    {
        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;
        float xa = roundLeft  ? x0 + r : x0;
        float xb = roundRight ? x1 - r : x1;
        float ya = y0 + r;
        float yb = y1 - r;
        boolean hasMidW = xb > xa;
        boolean hasMidH = yb > ya;

        if (roundLeft)
        {
            emitMaskQuad(b, m, x0, y0, xa, ya, 0F, 0F, 1F, 0F, 1F, 1F, 0F, 1F, color);
            emitMaskQuad(b, m, x0, yb, xa, y1, 0F, 1F, 1F, 1F, 1F, 0F, 0F, 0F, color);
        }
        if (roundRight)
        {
            emitMaskQuad(b, m, xb, y0, x1, ya, 1F, 0F, 0F, 0F, 0F, 1F, 1F, 1F, color);
            emitMaskQuad(b, m, xb, yb, x1, y1, 1F, 1F, 0F, 1F, 0F, 0F, 1F, 0F, color);
        }

        if (hasMidW)
        {
            emitMaskQuad(b, m, xa, y0, xb, ya, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
            emitMaskQuad(b, m, xa, yb, xb, y1, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
        }
        if (hasMidH)
        {
            if (roundLeft)
            {
                emitMaskQuad(b, m, x0, ya, xa, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
            }
            if (roundRight)
            {
                emitMaskQuad(b, m, xb, ya, x1, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
            }
        }
        if (hasMidW && hasMidH)
        {
            emitMaskQuad(b, m, xa, ya, xb, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
        }
    }

    /* 9-slice with per-CORNER rounding. Each corner cell samples the rounded mask when its flag is set,
     * else a solid (UV=1) quad -> square corner. Edges + center are always solid. Used to merge a stack of
     * selected rows into one block; correct for translucent fills (no double-alpha overdraw). */
    @Unique
    private static void emitRoundedSliceMaskCorners(VertexConsumer b, Matrix3x2fc m,
        float x, float y, float w, float h, float r, int color,
        boolean tl, boolean tr, boolean br, boolean bl)
    {
        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;
        float xa = x0 + r;
        float xb = x1 - r;
        float ya = y0 + r;
        float yb = y1 - r;
        boolean hasMidW = xb > xa;
        boolean hasMidH = yb > ya;

        if (tl) emitMaskQuad(b, m, x0, y0, xa, ya, 0F, 0F, 1F, 0F, 1F, 1F, 0F, 1F, color);
        else    emitMaskQuad(b, m, x0, y0, xa, ya, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
        if (tr) emitMaskQuad(b, m, xb, y0, x1, ya, 1F, 0F, 0F, 0F, 0F, 1F, 1F, 1F, color);
        else    emitMaskQuad(b, m, xb, y0, x1, ya, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
        if (br) emitMaskQuad(b, m, xb, yb, x1, y1, 1F, 1F, 0F, 1F, 0F, 0F, 1F, 0F, color);
        else    emitMaskQuad(b, m, xb, yb, x1, y1, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
        if (bl) emitMaskQuad(b, m, x0, yb, xa, y1, 0F, 1F, 1F, 1F, 1F, 0F, 0F, 0F, color);
        else    emitMaskQuad(b, m, x0, yb, xa, y1, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);

        if (hasMidW)
        {
            emitMaskQuad(b, m, xa, y0, xb, ya, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
            emitMaskQuad(b, m, xa, yb, xb, y1, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
        }
        if (hasMidH)
        {
            emitMaskQuad(b, m, x0, ya, xa, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
            emitMaskQuad(b, m, xb, ya, x1, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
        }
        if (hasMidW && hasMidH)
        {
            emitMaskQuad(b, m, xa, ya, xb, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color);
        }
    }

    /**
     * Filled rounded rectangle (single flat color). Radius is clamped to half the shorter side; if
     * the result is below a usable minimum it falls back to a plain {@link #box}.
     */
    @Override
    public void roundedBox(float x, float y, float w, float h, float radius, int color)
    {
        if (w <= 0F || h <= 0F)
        {
            return;
        }

        float r = clampRoundedRectRadius(w, h, radius);

        if (r < ROUNDED_RECT_MIN_RADIUS)
        {
            this.box(x, y, x + w, y + h, color);

            return;
        }

        GuiTexturedMesh mesh = new GuiTexturedMesh();

        emitRoundedSliceMask(mesh, this.refreshedui$matrix(), x, y, w, h, r, color);
        this.refreshedui$draw(mesh, this.getRoundedRectMask());
    }

    /* emitMaskQuad with everything above cutY dropped. Cells are axis-aligned and V only varies along Y, so
     * cropping the top edge just interpolates the top UVs toward the bottom ones. */
    @Unique
    private static void emitMaskQuadBelow(VertexConsumer b, Matrix3x2fc m,
        float x0, float y0, float x1, float y1,
        float u00, float v00, float u10, float v10, float u11, float v11, float u01, float v01,
        int color, float cutY)
    {
        if (cutY >= y1)
        {
            return;
        }

        if (cutY > y0)
        {
            float t = (cutY - y0) / (y1 - y0);

            u00 += (u01 - u00) * t;
            v00 += (v01 - v00) * t;
            u10 += (u11 - u10) * t;
            v10 += (v11 - v10) * t;
            y0 = cutY;
        }

        emitMaskQuad(b, m, x0, y0, x1, y1, u00, v00, u10, v10, u11, v11, u01, v01, color);
    }

    @Override
    public void roundedBoxBottomBand(float x, float y, float w, float h, float radius, float band, int color)
    {
        if (w <= 0F || h <= 0F || band <= 0F)
        {
            return;
        }

        float r = clampRoundedRectRadius(w, h, radius);
        float cut = y + h - Math.min(band, h);

        if (r < ROUNDED_RECT_MIN_RADIUS)
        {
            this.box(x, cut, x + w, y + h, color);

            return;
        }

        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;
        float xa = x0 + r;
        float xb = x1 - r;
        float ya = y0 + r;
        float yb = y1 - r;
        boolean hasMidW = xb > xa;
        boolean hasMidH = yb > ya;

        Matrix3x2fc m = this.refreshedui$matrix();
        GuiTexturedMesh mesh = new GuiTexturedMesh();

        /* Same cells/UVs as emitRoundedSliceMask, each cropped to the band. */
        emitMaskQuadBelow(mesh, m, x0, y0, xa, ya, 0F, 0F, 1F, 0F, 1F, 1F, 0F, 1F, color, cut);
        emitMaskQuadBelow(mesh, m, xb, y0, x1, ya, 1F, 0F, 0F, 0F, 0F, 1F, 1F, 1F, color, cut);
        emitMaskQuadBelow(mesh, m, xb, yb, x1, y1, 1F, 1F, 0F, 1F, 0F, 0F, 1F, 0F, color, cut);
        emitMaskQuadBelow(mesh, m, x0, yb, xa, y1, 0F, 1F, 1F, 1F, 1F, 0F, 0F, 0F, color, cut);

        if (hasMidW)
        {
            emitMaskQuadBelow(mesh, m, xa, y0, xb, ya, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color, cut);
            emitMaskQuadBelow(mesh, m, xa, yb, xb, y1, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color, cut);
        }
        if (hasMidH)
        {
            emitMaskQuadBelow(mesh, m, x0, ya, xa, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color, cut);
            emitMaskQuadBelow(mesh, m, xb, ya, x1, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color, cut);
        }
        if (hasMidW && hasMidH)
        {
            emitMaskQuadBelow(mesh, m, xa, ya, xb, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, color, cut);
        }

        this.refreshedui$draw(mesh, this.getRoundedRectMask());
    }

    /**
     * Rounded border with a rounded inset fill, both in one mesh. {@code inset} is the border thickness;
     * the inner radius follows the outer one minus the inset.
     *
     * <p>The border is laid out as a ring — the four arc cells plus straight strips of the inset's
     * thickness — rather than a whole rounded rect under the fill: a panel faded through {@code GuiAlpha}
     * would otherwise show its border colour through the translucent body. Only the small arc cells still
     * overlap the fill.</p>
     */
    @Override
    public void roundedFrame(float x, float y, float w, float h, float radius, float inset, int borderColor, int fillColor)
    {
        if (w <= 0F || h <= 0F)
        {
            return;
        }

        float outerR = clampRoundedRectRadius(w, h, radius);
        float innerX = x + inset;
        float innerY = y + inset;
        float innerW = w - inset * 2F;
        float innerH = h - inset * 2F;

        if (innerW <= 0F || innerH <= 0F || outerR < ROUNDED_RECT_MIN_RADIUS)
        {
            this.roundedBox(x, y, w, h, outerR, borderColor);

            return;
        }

        float innerR = clampRoundedRectRadius(innerW, innerH, Math.max(ROUNDED_RECT_MIN_RADIUS, outerR - inset));
        Matrix3x2fc m = this.refreshedui$matrix();
        GuiTexturedMesh mesh = new GuiTexturedMesh();

        if (outerR - inset < ROUNDED_RECT_MIN_RADIUS)
        {
            /* The inner arc would bulge past the strips; stack the two silhouettes instead */
            emitRoundedSliceMask(mesh, m, x, y, w, h, outerR, borderColor);
        }
        else
        {
            float x1 = x + w;
            float y1 = y + h;
            float xa = x + outerR;
            float xb = x1 - outerR;
            float ya = y + outerR;
            float yb = y1 - outerR;

            emitMaskQuad(mesh, m, x, y, xa, ya, 0F, 0F, 1F, 0F, 1F, 1F, 0F, 1F, borderColor);
            emitMaskQuad(mesh, m, xb, y, x1, ya, 1F, 0F, 0F, 0F, 0F, 1F, 1F, 1F, borderColor);
            emitMaskQuad(mesh, m, xb, yb, x1, y1, 1F, 1F, 0F, 1F, 0F, 0F, 1F, 0F, borderColor);
            emitMaskQuad(mesh, m, x, yb, xa, y1, 0F, 1F, 1F, 1F, 1F, 0F, 0F, 0F, borderColor);

            if (xb > xa)
            {
                emitMaskQuad(mesh, m, xa, y, xb, y + inset, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, borderColor);
                emitMaskQuad(mesh, m, xa, y1 - inset, xb, y1, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, borderColor);
            }
            if (yb > ya)
            {
                emitMaskQuad(mesh, m, x, ya, x + inset, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, borderColor);
                emitMaskQuad(mesh, m, x1 - inset, ya, x1, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, borderColor);
            }
        }

        emitRoundedSliceMask(mesh, m, innerX, innerY, innerW, innerH, innerR, fillColor);
        this.refreshedui$draw(mesh, this.getRoundedRectMask());
    }

    /**
     * 1px rounded outline over existing content. The straight edges are plain boxes; the curves use the
     * INVERTED rounded-rect mask (opaque outside the arc, transparent inside), so with the normal blend the fill
     * lands only beyond a silhouette's curve and existing pixels inside are kept. Pass 1 (inner rect, border
     * colour) paints the arc band of the ring; pass 2 (outer rect, outside colour) then cuts everything beyond
     * the outer curve, including the edge boxes' overhang and square children.
     */
    @Override
    public void roundedOutlineOver(float x, float y, float w, float h, float radius, int borderColor, int outsideColor)
    {
        if (w <= 2F || h <= 2F)
        {
            return;
        }

        this.box(x, y, x + w, y + 1F, borderColor);
        this.box(x, y + h - 1F, x + w, y + h, borderColor);
        this.box(x, y + 1F, x + 1F, y + h - 1F, borderColor);
        this.box(x + w - 1F, y + 1F, x + w, y + h - 1F, borderColor);

        float outerR = clampRoundedRectRadius(w, h, radius);

        if (outerR < ROUNDED_RECT_MIN_RADIUS)
        {
            return;
        }

        float innerR = clampRoundedRectRadius(w - 2F, h - 2F, Math.max(ROUNDED_RECT_MIN_RADIUS, outerR - 1F));
        Matrix3x2fc m = this.refreshedui$matrix();
        GuiTexturedMesh mesh = new GuiTexturedMesh();

        emitRoundedSliceMask(mesh, m, x + 1F, y + 1F, w - 2F, h - 2F, innerR, Colors.A100 | borderColor);
        emitRoundedSliceMask(mesh, m, x, y, w, h, outerR, Colors.A100 | outsideColor);

        this.refreshedui$draw(mesh, this.getRoundedRectMaskInverted());
    }

    /**
     * Like {@link #roundedBox} but only the left and/or right side is rounded — a half-pill cap whose
     * flat side meets a straight body. Single mesh, no scissor.
     */
    @Override
    public void roundedBoxSides(float x, float y, float w, float h, float radius, int color, boolean roundLeft, boolean roundRight)
    {
        if (w <= 0F || h <= 0F)
        {
            return;
        }

        if (!roundLeft && !roundRight)
        {
            this.box(x, y, x + w, y + h, color);

            return;
        }

        float r = clampRoundedRectRadius(w, h, radius);

        if (r < ROUNDED_RECT_MIN_RADIUS)
        {
            this.box(x, y, x + w, y + h, color);

            return;
        }

        GuiTexturedMesh mesh = new GuiTexturedMesh();

        emitRoundedSliceMaskSides(mesh, this.refreshedui$matrix(), x, y, w, h, r, color, roundLeft, roundRight);
        this.refreshedui$draw(mesh, this.getRoundedRectMask());
    }

    /**
     * Like {@link #roundedBox} but rounds only the flagged corners (others stay square). Single mesh,
     * correct for translucent fills — used to merge a vertical run of selected rows into one block.
     */
    @Override
    public void roundedBoxCorners(float x, float y, float w, float h, float radius, int color,
        boolean roundTopLeft, boolean roundTopRight, boolean roundBottomRight, boolean roundBottomLeft)
    {
        if (w <= 0F || h <= 0F)
        {
            return;
        }

        if (!roundTopLeft && !roundTopRight && !roundBottomRight && !roundBottomLeft)
        {
            this.box(x, y, x + w, y + h, color);

            return;
        }

        float r = clampRoundedRectRadius(w, h, radius);

        if (r < ROUNDED_RECT_MIN_RADIUS)
        {
            this.box(x, y, x + w, y + h, color);

            return;
        }

        GuiTexturedMesh mesh = new GuiTexturedMesh();

        emitRoundedSliceMaskCorners(mesh, this.refreshedui$matrix(), x, y, w, h, r, color, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
        this.refreshedui$draw(mesh, this.getRoundedRectMask());
    }

    /* === Color picker rounding (3.12) === horizontal alpha ramp + tiled checker swatch. */

    /* Per-vertex color variant of emitMaskQuad for gradients. UV layout identical. */
    @Unique
    private static void emitMaskQuadC(VertexConsumer b, Matrix3x2fc m,
        float x0, float y0, float x1, float y1,
        float u00, float v00, float u10, float v10, float u11, float v11, float u01, float v01,
        int c00, int c10, int c11, int c01)
    {
        b.vertex(m, x0, y0).texture(u00, v00).color(c00);
        b.vertex(m, x0, y1).texture(u01, v01).color(c01);
        b.vertex(m, x1, y1).texture(u11, v11).color(c11);
        b.vertex(m, x1, y0).texture(u10, v10).color(c10);
    }

    /* Horizontal alpha-ramp variant of emitRoundedSliceMask: color depends on x only, so we
     * precompute it at the four distinct x positions (x0, xa, xb, x1) and reuse per cell. */
    @Unique
    private static void emitRoundedSliceMaskGradH(VertexConsumer b, Matrix3x2fc m,
        float x, float y, float w, float h, float r,
        float regionX, float regionW, float cr, float cg, float cb, float endAlpha)
    {
        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;
        float xa = x0 + r;
        float xb = x1 - r;
        float ya = y0 + r;
        float yb = y1 - r;
        boolean hasMidW = xb > xa;
        boolean hasMidH = yb > ya;

        int cL  = horizontalAlphaArgb(x0, regionX, regionW, cr, cg, cb, endAlpha);
        int cLa = horizontalAlphaArgb(xa, regionX, regionW, cr, cg, cb, endAlpha);
        int cRa = horizontalAlphaArgb(xb, regionX, regionW, cr, cg, cb, endAlpha);
        int cR  = horizontalAlphaArgb(x1, regionX, regionW, cr, cg, cb, endAlpha);

        emitMaskQuadC(b, m, x0, y0, xa, ya, 0F, 0F, 1F, 0F, 1F, 1F, 0F, 1F, cL,  cLa, cLa, cL);
        emitMaskQuadC(b, m, xb, y0, x1, ya, 1F, 0F, 0F, 0F, 0F, 1F, 1F, 1F, cRa, cR,  cR,  cRa);
        emitMaskQuadC(b, m, xb, yb, x1, y1, 1F, 1F, 0F, 1F, 0F, 0F, 1F, 0F, cRa, cR,  cR,  cRa);
        emitMaskQuadC(b, m, x0, yb, xa, y1, 0F, 1F, 1F, 1F, 1F, 0F, 0F, 0F, cL,  cLa, cLa, cL);

        if (hasMidW)
        {
            emitMaskQuadC(b, m, xa, y0, xb, ya, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, cLa, cRa, cRa, cLa);
            emitMaskQuadC(b, m, xa, yb, xb, y1, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, cLa, cRa, cRa, cLa);
        }
        if (hasMidH)
        {
            emitMaskQuadC(b, m, x0, ya, xa, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, cL,  cLa, cLa, cL);
            emitMaskQuadC(b, m, xb, ya, x1, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, cRa, cR,  cR,  cRa);
        }
        if (hasMidW && hasMidH)
        {
            emitMaskQuadC(b, m, xa, ya, xb, yb, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, cLa, cRa, cRa, cLa);
        }
    }

    @Unique
    private static int horizontalAlphaArgb(float px, float regionX, float regionW, float cr, float cg, float cb, float endAlpha)
    {
        float nx = regionW <= 0F ? 0F : (px - regionX) / regionW;

        if (nx < 0F)
        {
            nx = 0F;
        }
        else if (nx > 1F)
        {
            nx = 1F;
        }

        float a = 1F + nx * (endAlpha - 1F);

        return Colors.COLOR.set(cr, cg, cb, a).getARGBColor();
    }

    /**
     * Filled rounded rectangle with horizontal alpha ramp (left {@code a=1}, right {@code a=endAlpha}),
     * same RGB throughout. Falls back to a plain horizontal-gradient box below the usable radius.
     */
    @Override
    public void roundedBoxHorizontalAlpha(float x, float y, float w, float h, float radius, float cr, float cg, float cb, float endAlpha)
    {
        if (w <= 0F || h <= 0F)
        {
            return;
        }

        float r = clampRoundedRectRadius(w, h, radius);

        if (r < ROUNDED_RECT_MIN_RADIUS)
        {
            int left = horizontalAlphaArgb(x, x, w, cr, cg, cb, endAlpha);
            int right = horizontalAlphaArgb(x + w, x, w, cr, cg, cb, endAlpha);

            this.gradientHBox(x, y, x + w, y + h, left, right);

            return;
        }

        GuiTexturedMesh mesh = new GuiTexturedMesh();

        emitRoundedSliceMaskGradH(mesh, this.refreshedui$matrix(), x, y, w, h, r, x, w, cr, cg, cb, endAlpha);
        this.refreshedui$draw(mesh, this.getRoundedRectMask());
    }

    /**
     * Checkerboard icon tiled inside a filled rounded rectangle. The body is cut along tile boundaries so
     * the checker phase stays aligned; each corner is the tiled icon clipped to a quarter disc
     * ({@link CheckerMesh}) — the 1.21.1 mask-stamp-and-multiply blend has no GUI pipeline on 1.21.11.
     */
    @Override
    public void roundedIconArea(Icon icon, float x, float y, float w, float h, float radius, int color)
    {
        if (w <= 0F || h <= 0F)
        {
            return;
        }

        Texture texture = BBSModClient.getTextures().getTexture(icon.texture);
        float r = clampRoundedRectRadius(w, h, radius);

        if (r < ROUNDED_RECT_MIN_RADIUS)
        {
            this.texturedArea(texture, color, x, y, w, h, icon.x, icon.y, icon.w, icon.h, icon.textureW, icon.textureH);

            return;
        }

        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;
        float innerW = w - 2F * r;
        float innerH = h - 2F * r;

        Matrix3x2fc m = this.refreshedui$matrix();
        GuiTexturedMesh mesh = new GuiTexturedMesh();

        if (innerW > 0F && innerH > 0F)
        {
            CheckerMesh.region(mesh, m, icon, color, x0 + r, y0 + r, innerW, innerH, x, y);
        }

        if (innerW > 0F)
        {
            CheckerMesh.region(mesh, m, icon, color, x0 + r, y0, innerW, r, x, y);
            CheckerMesh.region(mesh, m, icon, color, x0 + r, y1 - r, innerW, r, x, y);
        }

        if (innerH > 0F)
        {
            CheckerMesh.region(mesh, m, icon, color, x0, y0 + r, r, innerH, x, y);
            CheckerMesh.region(mesh, m, icon, color, x1 - r, y0 + r, r, innerH, x, y);
        }

        CheckerMesh.corner(mesh, m, icon, color, x0 + r, y0 + r, r, Math.PI, x, y);
        CheckerMesh.corner(mesh, m, icon, color, x1 - r, y0 + r, r, Math.PI * 1.5, x, y);
        CheckerMesh.corner(mesh, m, icon, color, x1 - r, y1 - r, r, 0D, x, y);
        CheckerMesh.corner(mesh, m, icon, color, x0 + r, y1 - r, r, Math.PI * 0.5, x, y);

        RenderPipeline pipeline = PixelArt.getTexturedPipeline(texture);

        mesh.draw(this.context, pipeline == null ? RenderPipelines.GUI_TEXTURED : pipeline, texture);
    }

    /* === Filled circle (3.5) === procedural circular SDF mask sampled as one quad. */

    @Unique
    private static final int FILLED_CIRCLE_MASK_SIZE = 64;
    @Unique
    private static volatile Texture filledCircleMask;

    @Unique
    private Texture getFilledCircleMask()
    {
        Texture cached = filledCircleMask;

        if (cached != null && cached.isValid())
        {
            return cached;
        }

        synchronized (Batcher2D.class)
        {
            cached = filledCircleMask;

            if (cached != null && cached.isValid())
            {
                return cached;
            }

            cached = buildFilledCircleMask(FILLED_CIRCLE_MASK_SIZE);
            filledCircleMask = cached;

            return cached;
        }
    }

    /* Procedural circular SDF mask: alpha=1 inside the inscribed circle, fading to 0 at the edge.
     * Sampled by {@link #filledCircle} as a single quad — silhouette is fully texture-defined, no
     * polygon faceting and AA stays crisp at any radius. */
    @Unique
    private static Texture buildFilledCircleMask(int size)
    {
        Pixels pixels = Pixels.fromSize(size, size);
        ByteBuffer buf = pixels.getBuffer();
        float center = (size - 1) * 0.5F;
        float maxR = center;

        buf.position(0);

        for (int y = 0; y < size; y++)
        {
            for (int x = 0; x < size; x++)
            {
                float dx = x - center;
                float dy = y - center;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0F, Math.min(1F, maxR - d + 0.5F));
                int alpha = Math.round(a * 255F);

                buf.put((byte) 255);
                buf.put((byte) 255);
                buf.put((byte) 255);
                buf.put((byte) alpha);
            }
        }

        buf.position(0);

        Texture texture = Texture.textureFromPixels(pixels, GL11.GL_LINEAR);

        /* Same bind-around-setWrap dance as the rounded-rect mask — see buildRoundedRectMask. */
        texture.bind();
        texture.setWrap(GL12.GL_CLAMP_TO_EDGE);
        texture.unbind();
        texture.setClearable(false);

        return texture;
    }

    /**
     * Filled circle (single color). Single textured quad sampling a procedural circular SDF mask —
     * silhouette is fully mask-defined, AA stays crisp at any radius. The {@code segments}
     * parameter is kept for API compatibility but is ignored (the geometry is always one quad).
     */
    @Override
    public void filledCircle(float cx, float cy, float radius, int color, int segments)
    {
        if (radius <= 0F)
        {
            return;
        }

        Matrix3x2fc m = this.refreshedui$matrix();
        GuiTexturedMesh mesh = new GuiTexturedMesh();

        emitMaskQuad(mesh, m, cx - radius, cy - radius, cx + radius, cy + radius, 0F, 0F, 1F, 0F, 1F, 1F, 0F, 1F, color);
        this.refreshedui$draw(mesh, this.getFilledCircleMask());
    }
}
