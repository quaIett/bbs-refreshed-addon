package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.anim.OverlayReveal;
import org.qualet.refreshedui.client.ui.OverlaySizes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixed-size popup overlays (3.7): when {@code addOverlay(context, panel)} sizes the panel to half
 * the screen, substitute the panel's fixed preferred pixel size from {@link OverlaySizes} (if any).
 *
 * <p>Also drives the {@link OverlayReveal} appear animation: {@code setupPanel} (where every overlay is
 * attached) arms a fresh reveal for the overlay + its panel, and {@code render} fades the full-screen
 * backdrop in alongside the panel's slide-up.</p>
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

    @Inject(method = "setupPanel", at = @At("TAIL"))
    private static void refreshedui$armReveal(UIContext context, UIOverlay overlay, UIOverlayPanel panel, CallbackInfo ci)
    {
        OverlayReveal.arm(overlay, panel);
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$fadeBackdrop(Area area, Batcher2D batcher, int color)
    {
        area.render(batcher, Colors.mulA(color, OverlayReveal.progress((UIElement) (Object) this)));
    }
}
