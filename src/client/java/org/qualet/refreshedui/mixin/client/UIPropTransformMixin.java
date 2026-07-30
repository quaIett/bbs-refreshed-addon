package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformOp;
import org.qualet.refreshedui.client.ui.ITransformModes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keep the refreshed transform-mode selector in sync with the gizmo hotkeys. {@code UIPropTransform}
 * binds G / S / R to {@code enableMode(TRANSLATE|SCALE|ROTATE)}; this forwards that mode to the
 * selector (see {@link ITransformModes}) so pressing a hotkey jumps to the matching tab.
 *
 * <p>Only the single-arg keyboard overload is hooked — the mouse-handle pick path uses the multi-arg
 * overloads and shouldn't reshuffle the panel.</p>
 *
 * <p>BBS 2.4 replaced the raw {@code int} mode with the {@link TransformOp} enum. Its constants are
 * declared in the same order the old ints used (translate / scale / rotate), so the selector still
 * takes the ordinal.</p>
 */
@Mixin(UIPropTransform.class)
public abstract class UIPropTransformMixin
{
    @Inject(method = "enableMode(Lmchorse/bbs_mod/ui/framework/elements/input/drag/TransformOp;)V", at = @At("TAIL"))
    private void refreshedui$syncSelectorTab(TransformOp op, CallbackInfo ci)
    {
        ((ITransformModes) (Object) this).refreshedui$setMode(op.ordinal());
    }
}
