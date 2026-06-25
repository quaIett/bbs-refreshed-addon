package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import org.qualet.refreshedui.client.ui.OverlaySizes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixed-size popup overlays (3.7): when {@code addOverlay(context, panel)} sizes the panel to half
 * the screen, substitute the panel's fixed preferred pixel size from {@link OverlaySizes} (if any).
 */
@Mixin(UIOverlay.class)
public abstract class UIOverlayMixin
{
    @Redirect(
        method = "addOverlay(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlayPanel;)Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlay;",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/UIElement;wh(FF)Lmchorse/bbs_mod/ui/framework/elements/UIElement;")
    )
    private static UIElement refreshedui$fixedSize(UIElement self, float w, float h)
    {
        if (self instanceof UIOverlayPanel)
        {
            int[] size = OverlaySizes.sizeFor((UIOverlayPanel) self);

            if (size != null)
            {
                return self.wh(size[0], size[1]);
            }
        }

        return self.wh(w, h);
    }
}
