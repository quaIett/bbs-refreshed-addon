package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.InterfaceBlur;
import org.qualet.refreshedui.client.blur.RefreshedBlur;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swaps the algorithm behind BBS's blur under overlay panels for {@link RefreshedBlur} (dual Kawase) while
 * keeping everything else BBS decides: the on/off setting, the radius and where the blur layer is marked.
 *
 * <p>MC 1.21.11: {@code apply} only marks a layer; the blur runs later in {@code render}, reached from the GUI
 * renderer's blur slot. The Kawase chain takes that slot; if it cannot be built, BBS's box blur runs instead.</p>
 */
@Mixin(InterfaceBlur.class)
public abstract class InterfaceBlurMixin
{
    @Shadow private static boolean marked;

    @Inject(method = "apply", at = @At("HEAD"))
    private static void refreshedui$markStrength(Batcher2D batcher, CallbackInfo ci)
    {
        RefreshedBlur.mark();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$kawase(CallbackInfoReturnable<Boolean> cir)
    {
        if (!marked || !RefreshedBlur.enabled() || !BBSSettings.interfaceBlur.get())
        {
            return;
        }

        if (RefreshedBlur.render(BBSSettings.interfaceBlurRadius.get()))
        {
            marked = false;
            cir.setReturnValue(true);
        }
    }
}
