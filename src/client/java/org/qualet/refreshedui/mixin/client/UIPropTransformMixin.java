package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformGesture;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformOp;
import org.qualet.refreshedui.client.ui.ITransformModes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keep the refreshed transform-mode selector in sync with the gizmo hotkeys. {@code UIPropTransform}
 * binds G / S / R (in {@code enableHotkeys}) to {@code gesture.enableMode(TRANSLATE|SCALE|ROTATE)};
 * this forwards that mode to the selector (see {@link ITransformModes}) so pressing a hotkey jumps to
 * the matching tab.
 *
 * <p>BBS 2.6 split the edit session out into {@link TransformGesture}, so {@code UIPropTransform} no
 * longer has an {@code enableMode} of its own — the hotkeys call the gesture from lambdas. Those calls
 * are wrapped here ({@code method = "*"} reaches the synthetic lambda bodies). Only the single-arg
 * keyboard overload is matched — the mouse-handle pick path ({@code Gizmo}) calls the 4-arg overload
 * from outside this class and shouldn't reshuffle the panel.</p>
 *
 * <p>{@link TransformOp} constants are declared in the order the selector's tabs use
 * (translate / scale / rotate), so the selector takes the ordinal.</p>
 */
@Mixin(UIPropTransform.class)
public abstract class UIPropTransformMixin
{
    @WrapOperation(
        method = "*",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/input/drag/TransformGesture;enableMode(Lmchorse/bbs_mod/ui/framework/elements/input/drag/TransformOp;)V")
    )
    private void refreshedui$syncSelectorTab(TransformGesture gesture, TransformOp op, Operation<Void> original)
    {
        original.call(gesture, op);

        ((ITransformModes) (Object) this).refreshedui$setMode(op.ordinal());
    }
}
