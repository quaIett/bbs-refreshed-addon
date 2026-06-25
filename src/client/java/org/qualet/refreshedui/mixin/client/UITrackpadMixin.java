package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Rounds the trackpad input surface (3.2a) and the +/- arrow side buttons. */
@Mixin(UITrackpad.class)
public abstract class UITrackpadMixin
{
    @Shadow private Area plusOne;
    @Shadow private Area minusOne;

    /** Main input surface — full rounded box with the hairline field border (design overhaul, 3). */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundSurface(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderField(area, batcher, color, UICornerRadii.buttonsAndTrackpads());
    }

    /**
     * Side arrow buttons ({@code plusOne}/{@code minusOne}). They sit flush with the surface's left
     * and right edges, so their outer corners must be rounded with the same radius to follow the
     * surface outline — otherwise the faint hover fill pokes square nubs past the rounded corners.
     * The inner edge (toward the centre) stays square so it joins the body seamlessly.
     */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;II)V")
    )
    private void refreshedui$roundArrow(Area area, Batcher2D batcher, int color, int offset)
    {
        float x = area.x + offset;
        float y = area.y + offset;
        float w = area.w - offset * 2;
        float h = area.h - offset * 2;

        boolean roundLeft = area == this.minusOne;
        boolean roundRight = area == this.plusOne;

        ((IRoundedBatcher) batcher).roundedBoxSides(x, y, w, h, UICornerRadii.buttonsAndTrackpads(), color, roundLeft, roundRight);
    }
}
