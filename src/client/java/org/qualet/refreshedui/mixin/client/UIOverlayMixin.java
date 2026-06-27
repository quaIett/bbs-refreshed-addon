package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import org.joml.Vector2i;
import org.qualet.refreshedui.client.anim.Animations;
import org.qualet.refreshedui.client.anim.OverlayReveal;
import org.qualet.refreshedui.client.ui.OverlaySizes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

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
    @Shadow @Final private static Map<String, Vector2i> offsets;

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

    /**
     * Play a close animation instead of detaching the overlay at once. Runs {@code closeItself}'s logic now
     * — click sound, panel close events, remembered drag offset — but keeps the overlay in the tree and
     * arms a reverse reveal; {@link OverlayReveal#finishClosed} detaches it once the slide-down + fade-out
     * finishes. With animations off, the original instant detach runs unchanged. A repeat close while the
     * animation plays is swallowed (the panel is already on its way out).
     */
    @Inject(method = "closeItself", at = @At("HEAD"), cancellable = true)
    private void refreshedui$animateClose(CallbackInfo ci)
    {
        if (!Animations.enabled())
        {
            return;
        }

        UIOverlay self = (UIOverlay) (Object) this;

        if (OverlayReveal.isClosing(self))
        {
            ci.cancel();

            return;
        }

        UIUtils.playClick();

        for (UIOverlayPanel panel : self.getChildren(UIOverlayPanel.class))
        {
            panel.onClose();
            offsets.put(panel.getClass().getSimpleName(), new Vector2i(panel.getFlex().x.offset, panel.getFlex().y.offset));
        }

        OverlayReveal.armClose(self);
        ci.cancel();
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$fadeBackdrop(Area area, Batcher2D batcher, int color)
    {
        area.render(batcher, Colors.mulA(color, OverlayReveal.visibility((UIElement) (Object) this)));
    }
}
