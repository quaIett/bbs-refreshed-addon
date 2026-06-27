package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import org.qualet.refreshedui.client.ui.ITransformModes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keep the refreshed transform-mode selector in sync with the gizmo hotkeys. {@code UIPropTransform}
 * binds G / S / R to {@code enableMode(0|1|2)} (translate / scale / rotate); this forwards that mode
 * to the selector (see {@link ITransformModes}) so pressing a hotkey jumps to the matching tab.
 *
 * <p>Only the single-arg keyboard overload {@code enableMode(int)} is hooked — the mouse-handle pick
 * path uses the multi-arg overloads and shouldn't reshuffle the panel.</p>
 */
@Mixin(UIPropTransform.class)
public abstract class UIPropTransformMixin
{
    @Inject(method = "enableMode(I)V", at = @At("TAIL"))
    private void refreshedui$syncSelectorTab(int mode, CallbackInfo ci)
    {
        ((ITransformModes) (Object) this).refreshedui$setMode(mode);
    }
}
