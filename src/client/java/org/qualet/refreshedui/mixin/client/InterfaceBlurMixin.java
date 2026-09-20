package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.utils.InterfaceBlur;
import org.qualet.refreshedui.client.anim.BlurFade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Fades BBS's background blur out with a closing overlay panel. BBS 2.7 blurs with dual Kawase (adapted from
 * this addon), so the algorithm itself is no longer ours to supply — only the close transition is, and BBS
 * has none, so the blur would otherwise hold full strength and vanish at once on detach.
 *
 * @see BlurFade
 */
@Mixin(InterfaceBlur.class)
public abstract class InterfaceBlurMixin
{
    @ModifyArg(
        method = "apply",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/InterfaceBlur;render(I)V"),
        index = 0
    )
    private static int refreshedui$fadeRadius(int radius)
    {
        return BlurFade.radius(radius);
    }
}
