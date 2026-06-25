package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.dashboard.panels.tabs.UIDataTabElement;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Rounds the data tab base surface (3.2b). {@code renderSkin} draws two boxes (base fill, then a
 * raised hover/selected overlay); only the first (the base fill) is rounded, hence {@code ordinal = 0}.
 */
@Mixin(UIDataTabElement.class)
public abstract class UIDataTabElementMixin
{
    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V", ordinal = 0)
    )
    private void refreshedui$roundBase(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        RoundedAreas.roundedBox(batcher, x1, y1, x2 - x1, y2 - y1, UICornerRadii.interfaceChrome(), color);
    }
}
