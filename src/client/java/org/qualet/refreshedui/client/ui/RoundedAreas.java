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

    /**
     * Horizontal counterpart of {@link #roundedBoxVertical}: a member of a ROW run rounds only the group's
     * outer corners ({@code roundLeft} for the first cell, {@code roundRight} for the last), so adjacent
     * active cells of an icon strip draw as one block instead of a row of separate pills.
     */
    public static void roundedBoxHorizontal(Batcher2D batcher, float x, float y, float w, float h, float radius, int color, boolean roundLeft, boolean roundRight)
    {
        ((IRoundedBatcher) batcher).roundedBoxCorners(x, y, w, h, radius, color, roundLeft, roundRight, roundRight, roundLeft);
    }

    /**
     * Fill of the engine selection mark ({@code Batcher2D.highlight}) in our style. Accent-coloured marks (the
     * "this one is active" case) get the full-strength primary the adaptive contrast icons are tuned for;
     * marks in a colour of their own ({@code Colors.NEGATIVE} on destructive verb-strip buttons) are standing
     * hints on several buttons at once, so they get a soft translucent fill instead.
     */
    public static int highlightFill(int color)
    {
        int rgb = color & Colors.RGB;
        boolean accent = rgb == (BBSSettings.primaryColor.get() & Colors.RGB);

        return accent ? Colors.A100 | rgb : Colors.A25 | rgb;
    }

    /**
     * Neutral hover wash for a context-menu row: a plain rounded grey, no stroke and no colour, so the
     * cursor is a hint and never competes with a row's own tint (icon) or an active toggle's frame. Same
     * tone family as {@code MaterialField}'s hover so surfaces agree.
     */
    public static void renderMenuHover(Batcher2D batcher, float x, float y, float w, float h, float radius)
    {
        renderMenuHover(batcher, x, y, w, h, radius, 1F);
    }

    /** The same wash at {@code strength} (0..1) of its resting alpha — a hover fading in or out. */
    public static void renderMenuHover(Batcher2D batcher, float x, float y, float w, float h, float radius, float strength)
    {
        ((IRoundedBatcher) batcher).roundedBox(x, y, w, h, radius, Colors.setA(MENU_HOVER_RGB, MENU_HOVER_ALPHA * strength));
    }

    private static final int MENU_HOVER_RGB = 0xe8e8e8;
    private static final float MENU_HOVER_ALPHA = 0.12F;

    /** Selection-frame stroke thickness, px. */
    private static final float SELECTION_BORDER_INSET = 1.5F;
    /** Interior fill brightness vs the stroke colour — muted/darker per the design mockup. */
    private static final float SELECTION_FILL_DARKEN = 0.4F;

    private RoundedAreas()
    {}
}
