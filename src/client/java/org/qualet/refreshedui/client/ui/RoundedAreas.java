package org.qualet.refreshedui.client.ui;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;

/**
 * Rounded counterparts of {@link Area}'s {@code render(...)} helpers, kept external so consumer
 * mixins stay one-liners and {@code Area} itself is not mixed into.
 *
 * <p>Ported from the {@code Area.renderRounded(...)} / {@code renderInsetRounded(...)} methods added
 * on bbs-fs {@code master-refreshed}. {@code roundedBox} clamps the radius and falls back to a plain
 * box below the usable minimum, so {@code radius == 0} reproduces the original square render exactly.</p>
 */
public final class RoundedAreas
{
    public static void renderRounded(Area area, Batcher2D batcher, int color, float radius, int lx, int ty, int rx, int by)
    {
        float x = area.x + lx;
        float y = area.y + ty;
        float w = area.w - lx - rx;
        float h = area.h - ty - by;

        ((IRoundedBatcher) batcher).roundedBox(x, y, w, h, radius, color);
    }

    public static void renderRounded(Area area, Batcher2D batcher, int color, float radius)
    {
        renderRounded(area, batcher, color, radius, 0, 0, 0, 0);
    }

    public static void renderRounded(Area area, Batcher2D batcher, int color, float radius, int offset)
    {
        renderRounded(area, batcher, color, radius, offset, offset, offset, offset);
    }

    /** Direct rounded box (for sites whose original call was {@code batcher.box(...)}). */
    public static void roundedBox(Batcher2D batcher, float x, float y, float w, float h, float radius, int color)
    {
        ((IRoundedBatcher) batcher).roundedBox(x, y, w, h, radius, color);
    }

    /**
     * Input-field surface (design overhaul, stage 3): a rounded fill with a subtle theme-aware border —
     * the dark inset look from the mockup.
     *
     * <p>Drawn as TWO {@code roundedBox} calls (not {@code roundedFrame}): a full-size border box, then the
     * opaque fill inset on top, leaving the border as a ring. This is deliberate — a 1px {@code roundedFrame}
     * ring gets eaten by the rounded corners' anti-aliasing (so the border "doesn't render"), and its
     * small-radius fallback drops the fill entirely. Two plain {@code roundedBox} calls always paint a solid
     * fill (square fallback included) and give the ring a visible {@value #FIELD_BORDER_INSET}px width.</p>
     */
    public static void renderField(Area area, Batcher2D batcher, int fillColor, float radius)
    {
        IRoundedBatcher rounded = (IRoundedBatcher) batcher;
        int border = BBSSettings.isLightTheme() ? 0x26000000 : 0xff33363a;
        float inset = FIELD_BORDER_INSET;

        rounded.roundedBox(area.x, area.y, area.w, area.h, radius, border);
        rounded.roundedBox(area.x + inset, area.y + inset, area.w - inset * 2F, area.h - inset * 2F, Math.max(0.5F, radius - inset), fillColor);
    }

    /** Field border ring thickness — a thin hairline (the fill is always painted, so a faint ring never
     * loses the field background). */
    private static final float FIELD_BORDER_INSET = 0.5F;

    /**
     * Selection-frame style for context-menu entries (design pass 2026-06-27): a BRIGHT inner stroke in
     * {@code baseColor} around a MUTED (darkened) interior fill of the same hue — instead of a flat
     * full-strength fill. {@code baseColor}'s hue is kept verbatim (primary for hover entries, the action's
     * custom tint for colorful ones); only its alpha is dropped so the caller can pass e.g. {@code A50|primary}.
     *
     * <p>Same TWO-{@code roundedBox} trick as {@link #renderField}: a full-size border box, then the muted
     * fill inset on top leaves a visible ring (a thin {@code roundedFrame} ring is eaten by corner AA).</p>
     */
    public static void renderSelectionFrame(Batcher2D batcher, float x, float y, float w, float h, int baseColor, float radius)
    {
        renderSelectionFrameVertical(batcher, x, y, w, h, baseColor, radius, true, true, false);
    }

    /**
     * Selection frame variant for a VERTICAL run of adjacent entries: when {@code roundTop} /
     * {@code roundBottom} is false, that edge is squared AND the muted fill is extended over the (now
     * internal) border so adjacent entries merge into one block — the bright stroke survives only on the
     * group's outer perimeter + the continuous side rails, never as a line between two merged rows.
     *
     * <p>{@code bright} lifts the interior to the FULL (un-darkened) colour — used as the hover state, so a
     * hovered entry reads as its plain colour instead of the muted resting fill.</p>
     */
    public static void renderSelectionFrameVertical(Batcher2D batcher, float x, float y, float w, float h, int baseColor, float radius, boolean roundTop, boolean roundBottom, boolean bright)
    {
        IRoundedBatcher rounded = (IRoundedBatcher) batcher;
        int border = Colors.A100 | (baseColor & 0xFFFFFF);
        int fill = bright ? border : Colors.mulRGB(border, SELECTION_FILL_DARKEN);
        float inset = SELECTION_BORDER_INSET;

        rounded.roundedBoxCorners(x, y, w, h, radius, border, roundTop, roundTop, roundBottom, roundBottom);

        float ty = roundTop ? inset : 0F;
        float by = roundBottom ? inset : 0F;
        rounded.roundedBoxCorners(x + inset, y + ty, w - inset * 2F, h - ty - by, Math.max(0.5F, radius - inset),
            fill, roundTop, roundTop, roundBottom, roundBottom);
    }

    /**
     * Flat rounded fill for a VERTICAL run member: rounds only the group's outer corners ({@code roundTop}
     * for the first row, {@code roundBottom} for the last), squaring merged edges so a multi-selection draws
     * as one block instead of a stack of separate pills. Translucency-safe (per-corner mask, no overdraw).
     */
    public static void roundedBoxVertical(Batcher2D batcher, float x, float y, float w, float h, float radius, int color, boolean roundTop, boolean roundBottom)
    {
        ((IRoundedBatcher) batcher).roundedBoxCorners(x, y, w, h, radius, color, roundTop, roundTop, roundBottom, roundBottom);
    }

    /** Selection-frame stroke thickness, px. */
    private static final float SELECTION_BORDER_INSET = 1.5F;
    /** Interior fill brightness vs the stroke colour — muted/darker per the design mockup. */
    private static final float SELECTION_FILL_DARKEN = 0.4F;

    private RoundedAreas()
    {}
}
