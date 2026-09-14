package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextarea;
import org.qualet.refreshedui.client.ui.MaterialField;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Multi-line text area in the same MD3 filled-field look as single-line text boxes ({@link MaterialField}). */
@Mixin(UITextarea.class)
public abstract class UITextareaMixin
{
    @Shadow private boolean focused;

    @Unique
    private final MaterialField.State refreshedui$field = new MaterialField.State();

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void refreshedui$materialSurface(UIContext context, CallbackInfo ci)
    {
        UITextarea<?> self = (UITextarea<?>) (Object) this;
        boolean hovered = self.isEnabled() && self.area.isInside(context);

        MaterialField.render(context.batcher, self.area, UICornerRadii.interfaceChrome(), this.refreshedui$field, hovered, this.focused);
        ci.cancel();
    }
}
