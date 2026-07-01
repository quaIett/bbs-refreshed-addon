package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.forms.UINestedEdit;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;
import mchorse.bbs_mod.ui.utils.UIConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Model blocks panel (dashboard tab with the model-block list): give the Pick/Edit form row a
 * standard margin so the two buttons get a gap instead of sitting flush. {@code row()} returns the
 * existing resizer without touching its margin, so the field is set explicitly — same fix as the
 * replay properties panel (see {@link UIReplayPropertiesPanelMixin}).
 */
@Mixin(UIModelBlockPanel.class)
public abstract class UIModelBlockPanelMixin
{
    @Shadow
    public UINestedEdit pickEdit;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$pickEditMargin(CallbackInfo ci)
    {
        this.pickEdit.row().margin = UIConstants.MARGIN;
    }
}
