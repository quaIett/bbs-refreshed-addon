package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.dashboard.panels.UISelectionScreen;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Rounds the selection screen backdrop (3.2b). */
@Mixin(UISelectionScreen.class)
public abstract class UISelectionScreenMixin
{
    @Redirect(
        method = "renderBackdrop",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V")
    )
    private void refreshedui$roundBackdrop(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        RoundedAreas.roundedBox(batcher, x1, y1, x2 - x1, y2 - y1, UICornerRadii.interfaceChrome(), color);
    }
}
