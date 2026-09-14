package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Rounds list rows (3.2a); merges adjacent multi-selected rows into one rounded block (2026-06-27) instead of
 * a stack of separate pills. The list background moved to {@code UIItems.render} in BBS 2.6 and is rounded by
 * {@link UIItemsMixin}.
 *
 * <p>BBS 2.6 (upstream 29cdbb4cd) draws every row's marks through {@link RowStyle#row} — header wash, hover
 * wash, pick wash and a 2px bar down the left edge. That call is replaced here for {@code UIList} rows only
 * (other {@code RowStyle} users — timeline tracks, section headers — are untouched).</p>
 */
@Mixin(UIList.class)
public abstract class UIListMixin
{
    @Shadow
    public List<Integer> current;

    /**
     * Rounded row marks in {@code RowStyle.row}'s order: header tint, then pick (accent, stronger under the
     * cursor) or hover (row colour / accent), then the row's own colour tag as a slim rounded pill so a
     * coloured row keeps its category. The pick rounds only the group's outer corners: square the top edge
     * when the row above is also selected and the bottom edge when the row below is. {@code index} is the
     * list index the row is drawn for (captured from the render-method arg).
     */
    @Redirect(
        method = "renderListElement",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/RowStyle;row(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;IIIIIZZZ)V")
    )
    private void refreshedui$roundRow(Batcher2D batcher, int x, int y, int w, int h, int color, boolean header, boolean hover, boolean picked,
        @Local(argsOnly = true, ordinal = 0) int index)
    {
        float radius = UICornerRadii.buttonsAndTrackpads();
        int accent = BBSSettings.primaryColor.get() & Colors.RGB;
        int tint = color != 0 ? color & Colors.RGB : accent;

        if (header)
        {
            RoundedAreas.roundedBox(batcher, x, y, w, h, radius, Colors.A25 | tint);
        }

        if (picked)
        {
            boolean roundTop = !this.current.contains(index - 1);
            boolean roundBottom = !this.current.contains(index + 1);

            RoundedAreas.roundedBoxVertical(batcher, x, y, w, h, radius, (hover ? Colors.A75 : Colors.A50) | accent, roundTop, roundBottom);
        }
        else if (hover)
        {
            RoundedAreas.roundedBox(batcher, x, y, w, h, radius, Colors.A25 | tint);
        }

        if (color != 0)
        {
            RoundedAreas.roundedBox(batcher, x + 1, y + 2, RowStyle.STRIPE, h - 4, RowStyle.STRIPE / 2F, Colors.A100 | color);
        }
    }
}
