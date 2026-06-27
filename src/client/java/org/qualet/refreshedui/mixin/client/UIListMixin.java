package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Rounds the list background and the selection highlight (3.2a); merges adjacent multi-selected rows
 * into one rounded block (2026-06-27) instead of a stack of separate pills. */
@Mixin(UIList.class)
public abstract class UIListMixin
{
    @Shadow
    public List<Integer> current;

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundBackground(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }

    /**
     * Round only the group's outer corners: square the top edge when the row above is also selected and the
     * bottom edge when the row below is — so a contiguous multi-selection reads as one block. {@code index}
     * is the list index the selection box is being drawn for (captured from the render-method arg).
     */
    @Redirect(
        method = "renderListElement",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V")
    )
    private void refreshedui$roundSelection(Batcher2D batcher, float x1, float y1, float x2, float y2, int color,
        @Local(argsOnly = true, ordinal = 0) int index)
    {
        boolean roundTop = !this.current.contains(index - 1);
        boolean roundBottom = !this.current.contains(index + 1);

        RoundedAreas.roundedBoxVertical(batcher, x1, y1, x2 - x1, y2 - y1, UICornerRadii.buttonsAndTrackpads(), color, roundTop, roundBottom);
    }
}
