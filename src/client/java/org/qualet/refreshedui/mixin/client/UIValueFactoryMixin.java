package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.settings.ui.UIValueFactory;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import org.qualet.refreshedui.LockedValueBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/** Settings locked off by {@code BBSSettingsMixin} get a disabled toggle (UIToggleMixin draws the lock). */
@Mixin(UIValueFactory.class)
public abstract class UIValueFactoryMixin
{
    @Inject(method = "booleanUI", at = @At("RETURN"))
    private static void refreshedui$lockToggle(ValueBoolean value, Consumer<UIToggle> callback, CallbackInfoReturnable<UIToggle> cir)
    {
        if (value instanceof LockedValueBoolean)
        {
            cir.getReturnValue().setEnabled(false);
        }
    }
}
