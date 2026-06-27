package org.qualet.refreshedui.client.ui;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
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

    private RoundedAreas()
    {}
}
