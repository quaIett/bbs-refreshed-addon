package org.qualet.refreshedui.mixin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.resources.Pixels;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
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
 * (TL/top/TR | L/center/R | BL/bottom/BR) in a single POSITION_TEXTURE_COLOR batch: the four
 * corner cells sample the mask quadrant (UV mirrored per corner so the arc faces outward), and
 * the five edge/center cells sample UV(1,1) — the mask's fully-opaque inner texel — so the body
 * is solid and corner-to-edge joins share the exact same sampler position (no seams).
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

    @Shadow
    protected abstract void fillTexturedBox(BufferBuilder builder, Matrix4f matrix, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH);

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

    /* Single-quadrant mask: outer arc tip (UV 0,0) -> alpha 0, inner corner texel (UV 1,1) -> alpha 1.
     * GL_CLAMP_TO_EDGE is mandatory: edge/center cells sample UV exactly 1.0 and would wrap to alpha 0
     * under the default GL_REPEAT, turning the body semi-transparent. */
    @Unique
    private static Texture buildRoundedRectMask(int size)
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
                int alpha = Math.round(a * 255F);

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

    /* Two triangles for one cell. UVs are passed per corner so callers can mirror the mask quadrant. */
    @Unique
    private static void emitMaskQuad(BufferBuilder b, Matrix4f m,
        float x0, float y0, float x1, float y1,
        float u00, float v00, float u10, float v10, float u11, float v11, float u01, float v01,
        int color)
    {
        b.vertex(m, x0, y1, 0F).texture(u01, v01).color(color).next();
        b.vertex(m, x1, y1, 0F).texture(u11, v11).color(color).next();
        b.vertex(m, x1, y0, 0F).texture(u10, v10).color(color).next();
        b.vertex(m, x0, y1, 0F).texture(u01, v01).color(color).next();
        b.vertex(m, x1, y0, 0F).texture(u10, v10).color(color).next();
        b.vertex(m, x0, y0, 0F).texture(u00, v00).color(color).next();
    }

    /* Emit one rounded-rect silhouette as up to 9 mask-sampled quads into the caller's active
     * TRIANGLES batch. r must be clamped and >= ROUNDED_RECT_MIN_RADIUS. */
    @Unique
    private static void emitRoundedSliceMask(BufferBuilder b, Matrix4f m,
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
     * "half-pill" caps as one batch without scissor + plain-box tricks. */
    @Unique
    private static void emitRoundedSliceMaskSides(BufferBuilder b, Matrix4f m,
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

        Texture mask = this.getRoundedRectMask();
        Matrix4f matrix4f = this.context.getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, mask.id);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        emitRoundedSliceMask(builder, matrix4f, x, y, w, h, r, color);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        this.context.draw();
    }

    /**
     * Rounded border with a rounded inset fill, both emitted into one batch. {@code inset} is the
     * border thickness; the inner radius follows the outer one minus the inset.
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
        Texture mask = this.getRoundedRectMask();
        Matrix4f matrix4f = this.context.getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, mask.id);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        emitRoundedSliceMask(builder, matrix4f, x, y, w, h, outerR, borderColor);
        emitRoundedSliceMask(builder, matrix4f, innerX, innerY, innerW, innerH, innerR, fillColor);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        this.context.draw();
    }

    /**
     * Like {@link #roundedBox} but only the left and/or right side is rounded — a half-pill cap whose
     * flat side meets a straight body. Single draw call, no scissor.
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

        Texture mask = this.getRoundedRectMask();
        Matrix4f matrix4f = this.context.getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, mask.id);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        emitRoundedSliceMaskSides(builder, matrix4f, x, y, w, h, r, color, roundLeft, roundRight);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        this.context.draw();
    }

    /* === Color picker rounding (3.12) === horizontal alpha ramp + tiled checker swatch. */

    /* Per-vertex color variant of emitMaskQuad for gradients. UV layout identical. */
    @Unique
    private static void emitMaskQuadC(BufferBuilder b, Matrix4f m,
        float x0, float y0, float x1, float y1,
        float u00, float v00, float u10, float v10, float u11, float v11, float u01, float v01,
        int c00, int c10, int c11, int c01)
    {
        b.vertex(m, x0, y1, 0F).texture(u01, v01).color(c01).next();
        b.vertex(m, x1, y1, 0F).texture(u11, v11).color(c11).next();
        b.vertex(m, x1, y0, 0F).texture(u10, v10).color(c10).next();
        b.vertex(m, x0, y1, 0F).texture(u01, v01).color(c01).next();
        b.vertex(m, x1, y0, 0F).texture(u10, v10).color(c10).next();
        b.vertex(m, x0, y0, 0F).texture(u00, v00).color(c00).next();
    }

    /* Horizontal alpha-ramp variant of emitRoundedSliceMask: color depends on x only, so we
     * precompute it at the four distinct x positions (x0, xa, xb, x1) and reuse per cell. */
    @Unique
    private static void emitRoundedSliceMaskGradH(BufferBuilder b, Matrix4f m,
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

        Texture mask = this.getRoundedRectMask();
        Matrix4f matrix4f = this.context.getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, mask.id);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        emitRoundedSliceMaskGradH(builder, matrix4f, x, y, w, h, r, x, w, cr, cg, cb, endAlpha);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        this.context.draw();
    }

    @Unique
    private static float checkboardU(Icon icon, float px, float originX)
    {
        float dx = px - originX;
        float tile = icon.w;

        if (tile <= 0.0001F)
        {
            return icon.x / (float) icon.textureW;
        }

        float lx = dx - (float) Math.floor(dx / tile) * tile;

        return (icon.x + lx) / (float) icon.textureW;
    }

    @Unique
    private static float checkboardV(Icon icon, float py, float originY)
    {
        float dy = py - originY;
        float tile = icon.h;

        if (tile <= 0.0001F)
        {
            return icon.y / (float) icon.textureH;
        }

        float ly = dy - (float) Math.floor(dy / tile) * tile;

        return (icon.y + ly) / (float) icon.textureH;
    }

    /* Tiles an Icon inside an axis-aligned rectangle with phase locked to originX/originY (same
     * repeat as texturedArea would have if that area started at the origin). */
    @Unique
    private void fillCheckerboardTiledRegion(Texture texture, int color, float rx, float ry, float rw, float rh, float originX, float originY, Icon icon)
    {
        if (rw <= 0F || rh <= 0F)
        {
            return;
        }

        float x2 = rx + rw;
        float y2 = ry + rh;
        float tileW = icon.w;
        float tileH = icon.h;

        Matrix4f matrix = this.context.getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, texture.id);

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        for (float yy = ry; yy < y2; )
        {
            double relY = yy - originY;
            float dv = (float) (relY - Math.floor(relY / tileH) * tileH);
            float yh = Math.min(tileH - dv, y2 - yy);

            if (yh <= 0F)
            {
                break;
            }

            for (float xx = rx; xx < x2; )
            {
                double relX = xx - originX;
                float du = (float) (relX - Math.floor(relX / tileW) * tileW);
                float xw = Math.min(tileW - du, x2 - xx);

                if (xw <= 0F)
                {
                    break;
                }

                float u1 = icon.x + du;
                float v1 = icon.y + dv;

                this.fillTexturedBox(builder, matrix, color, xx, yy, xw, yh, u1, v1, u1 + xw, v1 + yh, icon.textureW, icon.textureH);
                xx += xw;
            }

            yy += yh;
        }

        RenderSystem.enableBlend();
        BufferRenderer.drawWithGlobalProgram(builder.end());

        this.context.draw();
    }

    /* Four corner quads with mask UV (no edges/center). Used by the 2-pass textured variant. */
    @Unique
    private static void emitRoundedCornersMask(BufferBuilder b, Matrix4f m,
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

        emitMaskQuad(b, m, x0, y0, xa, ya, 0F, 0F, 1F, 0F, 1F, 1F, 0F, 1F, color);
        emitMaskQuad(b, m, xb, y0, x1, ya, 1F, 0F, 0F, 0F, 0F, 1F, 1F, 1F, color);
        emitMaskQuad(b, m, xb, yb, x1, y1, 1F, 1F, 0F, 1F, 0F, 0F, 1F, 0F, color);
        emitMaskQuad(b, m, x0, yb, xa, y1, 0F, 1F, 1F, 1F, 1F, 0F, 0F, 0F, color);
    }

    /* Four corner quads with checker-tile UV. */
    @Unique
    private static void emitRoundedCornersChecker(BufferBuilder b, Matrix4f m,
        float x, float y, float w, float h, float r, Icon icon, float ox, float oy, int color)
    {
        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;
        float xa = x0 + r;
        float xb = x1 - r;
        float ya = y0 + r;
        float yb = y1 - r;

        emitCheckerQuad(b, m, x0, y0, xa, ya, icon, ox, oy, color);
        emitCheckerQuad(b, m, xb, y0, x1, ya, icon, ox, oy, color);
        emitCheckerQuad(b, m, xb, yb, x1, y1, icon, ox, oy, color);
        emitCheckerQuad(b, m, x0, yb, xa, y1, icon, ox, oy, color);
    }

    @Unique
    private static void emitCheckerQuad(BufferBuilder b, Matrix4f m,
        float x0, float y0, float x1, float y1, Icon icon, float ox, float oy, int color)
    {
        float u00 = checkboardU(icon, x0, ox), v00 = checkboardV(icon, y0, oy);
        float u10 = checkboardU(icon, x1, ox), v10 = checkboardV(icon, y0, oy);
        float u11 = checkboardU(icon, x1, ox), v11 = checkboardV(icon, y1, oy);
        float u01 = checkboardU(icon, x0, ox), v01 = checkboardV(icon, y1, oy);

        b.vertex(m, x0, y1, 0F).texture(u01, v01).color(color).next();
        b.vertex(m, x1, y1, 0F).texture(u11, v11).color(color).next();
        b.vertex(m, x1, y0, 0F).texture(u10, v10).color(color).next();
        b.vertex(m, x0, y1, 0F).texture(u01, v01).color(color).next();
        b.vertex(m, x1, y0, 0F).texture(u10, v10).color(color).next();
        b.vertex(m, x0, y0, 0F).texture(u00, v00).color(color).next();
    }

    /**
     * Checkerboard icon tiled inside a filled rounded rectangle. Body uses per-tile subdivision via
     * {@link #fillCheckerboardTiledRegion} so the checker phase stays aligned; the four corner cells
     * use the shared mask 2-pass (stamp silhouette, multiply tile) for a smooth AA edge.
     */
    @Override
    public void roundedIconArea(Icon icon, float x, float y, float w, float h, float radius, int color)
    {
        if (w <= 0F || h <= 0F)
        {
            return;
        }

        float r = clampRoundedRectRadius(w, h, radius);

        if (r < ROUNDED_RECT_MIN_RADIUS)
        {
            this.texturedArea(BBSModClient.getTextures().getTexture(icon.texture), color, x, y, w, h, icon.x, icon.y, icon.w, icon.h, icon.textureW, icon.textureH);

            return;
        }

        Texture texture = BBSModClient.getTextures().getTexture(icon.texture);
        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;
        float innerW = w - 2F * r;
        float innerH = h - 2F * r;

        Matrix4f matrix4f = this.context.getMatrices().peek().getPositionMatrix();

        /* Body (5 cells): tiled-correct subdivision preserves checker phase. */
        if (innerW > 0F && innerH > 0F)
        {
            this.fillCheckerboardTiledRegion(texture, color, x0 + r, y0 + r, innerW, innerH, x, y, icon);
        }

        if (innerW > 0F)
        {
            this.fillCheckerboardTiledRegion(texture, color, x0 + r, y0, innerW, r, x, y, icon);
            this.fillCheckerboardTiledRegion(texture, color, x0 + r, y1 - r, innerW, r, x, y, icon);
        }

        if (innerH > 0F)
        {
            this.fillCheckerboardTiledRegion(texture, color, x0, y0 + r, r, innerH, x, y, icon);
            this.fillCheckerboardTiledRegion(texture, color, x1 - r, y0 + r, r, innerH, x, y, icon);
        }

        /* Corners (4 cells): 2-pass mask × content. */
        Texture mask = this.getRoundedRectMask();

        /* Pass 1: stamp mask alpha into the four corner quads (overwrites destination). */
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.setShaderTexture(0, mask.id);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);
        emitRoundedCornersMask(builder, matrix4f, x, y, w, h, r, Colors.WHITE);
        BufferRenderer.drawWithGlobalProgram(builder.end());
        this.context.draw();

        /* Pass 2: multiply icon RGB into the stamped corner pixels; preserve alpha from pass 1. */
        RenderSystem.blendFuncSeparate(
            GlStateManager.SrcFactor.DST_COLOR, GlStateManager.DstFactor.ZERO,
            GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, texture.id);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

        builder = Tessellator.getInstance().getBuffer();
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);
        emitRoundedCornersChecker(builder, matrix4f, x, y, w, h, r, icon, x, y, color);
        BufferRenderer.drawWithGlobalProgram(builder.end());
        this.context.draw();

        RenderSystem.defaultBlendFunc();
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
     * parameter is kept for API compatibility but is ignored (the geometry is always 2 triangles).
     */
    @Override
    public void filledCircle(float cx, float cy, float radius, int color, int segments)
    {
        if (radius <= 0F)
        {
            return;
        }

        Texture mask = this.getFilledCircleMask();
        Matrix4f matrix4f = this.context.getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        float x0 = cx - radius;
        float y0 = cy - radius;
        float x1 = cx + radius;
        float y1 = cy + radius;

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, mask.id);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        builder.vertex(matrix4f, x0, y1, 0F).texture(0F, 1F).color(color).next();
        builder.vertex(matrix4f, x1, y1, 0F).texture(1F, 1F).color(color).next();
        builder.vertex(matrix4f, x1, y0, 0F).texture(1F, 0F).color(color).next();
        builder.vertex(matrix4f, x0, y1, 0F).texture(0F, 1F).color(color).next();
        builder.vertex(matrix4f, x1, y0, 0F).texture(1F, 0F).color(color).next();
        builder.vertex(matrix4f, x0, y0, 0F).texture(0F, 0F).color(color).next();

        BufferRenderer.drawWithGlobalProgram(builder.end());

        this.context.draw();
    }
}
