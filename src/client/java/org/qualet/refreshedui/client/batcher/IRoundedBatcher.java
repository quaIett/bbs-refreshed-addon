package org.qualet.refreshedui.client.batcher;

/**
 * Accessor interface mixed into BBS's {@code Batcher2D} to expose the addon's rounded-rectangle
 * primitives. Call sites cast the live batcher: {@code ((IRoundedBatcher) context.batcher).roundedBox(...)}.
 *
 * <p>The primitives cannot live in an external helper — they need {@code Batcher2D}'s private
 * {@code DrawContext} and the rounded-rect alpha-mask machinery, so they are added as {@code @Unique}
 * members by {@code Batcher2DMixin}, which implements this interface.</p>
 *
 * <p>Stage 3.1 exposes the rounded-rect family. Later stages extend this interface as their
 * consumers land: {@code filledCircle} (toggle, 3.5), {@code roundedBoxHorizontalAlpha} /
 * {@code roundedIconArea} (color picker, 3.12).</p>
 */
public interface IRoundedBatcher
{
    /** Filled rounded rectangle (single flat color). Falls back to a plain box below a usable radius. */
    void roundedBox(float x, float y, float w, float h, float radius, int color);

    /** Rounded border with a rounded inset fill, both in one batch. {@code inset} is the border thickness. */
    void roundedFrame(float x, float y, float w, float h, float radius, float inset, int borderColor, int fillColor);

    /** Like {@link #roundedBox} but only the left and/or right side is rounded — a half-pill cap. */
    void roundedBoxSides(float x, float y, float w, float h, float radius, int color, boolean roundLeft, boolean roundRight);

    /** Filled circle (single color) sampling a procedural circular SDF mask. {@code segments} is ignored. */
    void filledCircle(float cx, float cy, float radius, int color, int segments);

    /**
     * Filled rounded rectangle with a horizontal alpha ramp (left {@code a=1}, right {@code a=endAlpha}),
     * same RGB throughout. Falls back to a plain horizontal-gradient box below a usable radius. (3.12)
     */
    void roundedBoxHorizontalAlpha(float x, float y, float w, float h, float radius, float cr, float cg, float cb, float endAlpha);

    /**
     * Checkerboard {@code Icon} tiled inside a filled rounded rectangle (transparency swatch). Body is
     * tiled per-cell to keep the checker phase aligned; the four corners use a mask 2-pass for AA. (3.12)
     */
    void roundedIconArea(mchorse.bbs_mod.ui.utils.icons.Icon icon, float x, float y, float w, float h, float radius, int color);
}
