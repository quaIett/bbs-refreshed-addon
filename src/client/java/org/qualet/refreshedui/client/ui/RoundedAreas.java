package org.qualet.refreshedui.client.ui;

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

    private RoundedAreas()
    {}
}
