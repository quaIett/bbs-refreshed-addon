package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.anim.PanelTransitions;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks when rendering descends into the panel that is currently playing its appear reveal, so the text
 * interceptor ({@code Batcher2DTextStaggerMixin}) can scope the stagger to that panel's subtree. While the
 * appearing root is on the render stack, everything drawn under it is "inside" (see {@link PanelTransitions}).
 *
 * <p>This rides {@code UIElement.render}, a hot path, but both hooks are a single reference compare and do
 * nothing unless a transition is armed.</p>
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
    private void refreshedui$enterAppearRoot(UIContext context, CallbackInfo ci)
    {
        PanelTransitions.enter((UIElement) (Object) this);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void refreshedui$exitAppearRoot(UIContext context, CallbackInfo ci)
    {
        PanelTransitions.exit((UIElement) (Object) this);
    }
}
