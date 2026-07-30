package org.qualet.refreshedui.mixin.client;

import org.qualet.refreshedui.client.ui.IDefaultValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mchorse.bbs_mod.ui.framework.elements.input.UINumericInput;

/**
 * Remembers the value a numeric field was last handed by its owner, so a slider can offer a
 * right-click reset ({@link IDefaultValue}). Purely observational — no field behaves differently
 * because of this mixin, it only records.
 *
 * <p>BBS has no runtime schema default to reset to, so "default" is defined as the last value that
 * arrived from outside: a plain {@code setValue} that is neither part of a
 * {@code setValueAndNotify} (every user-driven path goes through that one) nor mid-drag (a delayed
 * -input drag writes with a bare {@code setValue}). What is left is the panel loading data into the
 * field — panel open, selection change, undo.</p>
 */
@Mixin(UINumericInput.class)
public abstract class UINumericInputMixin implements IDefaultValue
{
    @Unique
    private double refreshedui$defaultValue;

    @Unique
    private boolean refreshedui$hasDefaultValue;

    /** Depth, not a flag: {@code setValueAndNotify} can nest through a callback. */
    @Unique
    private int refreshedui$notifyDepth;

    @Inject(method = "setValueAndNotify", at = @At("HEAD"))
    private void refreshedui$enterNotify(double value, CallbackInfo ci)
    {
        this.refreshedui$notifyDepth++;
    }

    @Inject(method = "setValueAndNotify", at = @At("TAIL"))
    private void refreshedui$exitNotify(double value, CallbackInfo ci)
    {
        this.refreshedui$notifyDepth--;
    }

    @Inject(method = "setValue", at = @At("TAIL"))
    private void refreshedui$captureDefault(double value, CallbackInfo ci)
    {
        UINumericInput<?> self = (UINumericInput<?>) (Object) this;

        if (this.refreshedui$notifyDepth == 0 && !self.isDragging())
        {
            this.refreshedui$defaultValue = self.getValue();
            this.refreshedui$hasDefaultValue = true;
        }
    }

    @Override
    public boolean refreshedui$hasDefault()
    {
        return this.refreshedui$hasDefaultValue;
    }

    @Override
    public double refreshedui$getDefault()
    {
        return this.refreshedui$defaultValue;
    }
}
