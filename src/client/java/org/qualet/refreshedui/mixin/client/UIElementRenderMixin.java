package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.anim.OverlayReveal;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <p>Detaches overlays whose close animation has finished (see {@link OverlayReveal#finishClosed}).</p>
 *
 * <p>Also rounds the locked (disabled) overlay drawn by {@code renderLockedArea}: clean BBS paints a plain
 * square {@code A50} box over disabled fields (e.g. the IK/physics groups before a bone is picked), which
 * clashed with our rounded fields. The redirect swaps it for a {@code roundedBox} clamped to the widget size.</p>
 */
@Mixin(UIElement.class)
public abstract class UIElementRenderMixin
{
    @Redirect(
        method = "renderLockedArea",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundLockedOverlay(Area area, Batcher2D batcher, int color)
    {
        float radius = UICornerRadii.interfaceChromeClamped(area.w, area.h);

        RoundedAreas.renderRounded(area, batcher, color, radius);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void refreshedui$finishClosedOverlays(UIContext context, CallbackInfo ci)
    {
        UIElement self = (UIElement) (Object) this;

        /* When the overlay container is about to render, first detach any overlay whose close animation has
         * finished — here, at its render head, the removal happens before its children loop, so it cannot
         * corrupt that iteration. Inert (a ref compare + empty-map check) when nothing is closing. */
        if (context.menu != null && context.menu.overlay == self)
        {
            OverlayReveal.finishClosed(self);
        }
    }
}
