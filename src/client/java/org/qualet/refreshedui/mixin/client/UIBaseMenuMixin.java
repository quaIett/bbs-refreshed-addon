package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import org.qualet.refreshedui.client.anim.PointerClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stamps {@link PointerClock} with every left press that reaches a BBS screen. */
@Mixin(UIBaseMenu.class)
public abstract class UIBaseMenuMixin
{
    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void refreshedui$stampPress(int mouseX, int mouseY, int mouseButton, CallbackInfoReturnable<Boolean> cir)
    {
        if (mouseButton == 0)
        {
            PointerClock.leftPressed();
        }
    }
}
