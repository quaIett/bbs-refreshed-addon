package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import org.qualet.refreshedui.client.anim.PanelTransitions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks when rendering descends into the panel that is currently playing its appear reveal, so the text
 * interceptor ({@code Batcher2DTextStaggerMixin}) can scope the stagger to that panel's subtree. While the
 * appearing root is on the render stack, everything drawn under it is "inside" (see {@link PanelTransitions}).
 *
 * <p>This rides {@code UIElement.render}, a hot path, but both hooks are a single reference compare and do
 * nothing unless a transition is armed.</p>
 */
@Mixin(UIElement.class)
public abstract class UIElementRenderMixin
{
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
