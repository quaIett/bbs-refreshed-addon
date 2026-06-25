package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.utils.colors.Colors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives the right-hand form properties column the same A50 backdrop the left tree
 * already has ({@code UIFormEditor}), but only when editing in the world (immersive
 * {@link UIFormPalette}). Backdrop tracks {@code options.area}, so it follows the
 * panel's height and the draggable width.
 */
@Mixin(UIFormPanel.class)
public abstract class UIFormPanelMixin
{
    @Shadow public UIScrollView options;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$addImmersiveBackdrop(CallbackInfo ci)
    {
        UIElement self = (UIElement) (Object) this;

        UIRenderable backdrop = new UIRenderable((context) ->
        {
            UIFormPalette palette = self.getParent(UIFormPalette.class);

            if (palette != null && palette.isImmersive())
            {
                this.options.area.render(context.batcher, Colors.A50);
            }
        });

        self.prepend(backdrop);
    }
}
