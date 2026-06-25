package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.tooltips.ITooltip;
import mchorse.bbs_mod.ui.framework.tooltips.UITooltip;
import org.qualet.refreshedui.RefreshedUiAddon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code show_tooltips} gate (3.14): when the addon setting is off, hover tooltips never register or
 * render. Default true keeps the original behavior. Null-guarded so tooltips show before the setting
 * is registered.
 */
@Mixin(UITooltip.class)
public abstract class UITooltipMixin
{
    @Shadow
    public UIElement element;

    @org.spongepowered.asm.mixin.Unique
    private static boolean refreshedui$hidden()
    {
        return RefreshedUiAddon.showTooltips != null && !RefreshedUiAddon.showTooltips.get();
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void refreshedui$gateSet(UIContext context, UIElement element, CallbackInfo ci)
    {
        if (refreshedui$hidden())
        {
            this.element = null;
            ci.cancel();
        }
    }

    @Inject(method = "render(Lmchorse/bbs_mod/ui/framework/tooltips/ITooltip;Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At("HEAD"), cancellable = true)
    private void refreshedui$gateRenderTooltip(ITooltip tooltip, UIContext context, CallbackInfo ci)
    {
        if (refreshedui$hidden())
        {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At("HEAD"), cancellable = true)
    private void refreshedui$gateRender(UIContext context, CallbackInfo ci)
    {
        if (refreshedui$hidden())
        {
            this.element = null;
            ci.cancel();
        }
    }
}
