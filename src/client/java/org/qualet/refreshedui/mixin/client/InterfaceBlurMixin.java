package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.utils.InterfaceBlur;
import org.qualet.refreshedui.client.blur.RefreshedBlur;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swaps the algorithm behind BBS's blur under overlay panels for {@link RefreshedBlur} (dual Kawase) while
 * keeping everything else BBS decides: the on/off setting, the radius, once per frame, and
 * {@code applyUnder} (which only resets {@code applied} after calling {@code apply}). If the Kawase chain
 * cannot be built, the original box blur runs instead.
 */
@Mixin(InterfaceBlur.class)
public abstract class InterfaceBlurMixin
{
    @Shadow private static boolean applied;

    @Shadow private static boolean broken;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$kawase(CallbackInfo ci)
    {
        if (!RefreshedBlur.enabled() || applied || broken || !BBSSettings.interfaceBlur.get())
        {
            return;
        }

        if (RefreshedBlur.render(BBSSettings.interfaceBlurRadius.get()))
        {
            applied = true;
            ci.cancel();
        }
    }
}
