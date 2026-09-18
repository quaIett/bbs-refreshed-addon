package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import org.qualet.refreshedui.client.anim.ContextMenuReveal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arms the context menu opening ({@link ContextMenuReveal}) at the three places {@code UIContext} puts a
 * menu on screen: a fresh one, one opened in place of the current one, and a parked one brought back by
 * a step back. TAIL only fires past the early returns, and the menu is checked to be the one now on screen.
 */
@Mixin(UIContext.class)
public abstract class UIContextMixin
{
    @Shadow
    public UIContextMenu contextMenu;

    @Inject(method = "setContextMenu", at = @At("TAIL"))
    private void refreshedui$openMenu(UIContextMenu menu, CallbackInfo ci)
    {
        this.refreshedui$arm(menu);
    }

    @Inject(method = "replaceContextMenu(Lmchorse/bbs_mod/ui/framework/elements/context/UIContextMenu;)V", at = @At("TAIL"))
    private void refreshedui$replaceMenu(UIContextMenu menu, CallbackInfo ci)
    {
        this.refreshedui$arm(menu);
    }

    @Inject(method = "backContextMenu", at = @At("TAIL"))
    private void refreshedui$backMenu(CallbackInfo ci)
    {
        this.refreshedui$arm(this.contextMenu);
    }

    private void refreshedui$arm(UIContextMenu menu)
    {
        if (menu != null && menu == this.contextMenu)
        {
            ContextMenuReveal.armOpen(menu, (UIContext) (Object) this);
        }
    }
}
