package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.UITabStrip;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Dock-stack tabs of the reusable {@link mchorse.bbs_mod.ui.framework.elements.layout.UIDockLayout}.
 * In BBS 2.6 the private inner {@code UIDockStackTabs} became a {@link UITabStrip}: every tab is a child
 * {@link UIIcon} that paints its own icon, and the strip only marks the active one
 * ({@code UITabStrip.preRender} → {@code Batcher2D.highlight}). So there is no icon draw call left to
 * recolor inside the tabs' {@code render}; instead, at its head, flag the active tab's {@link UIIcon} as
 * {@code active} with the adaptive contrast color (white/black by primary brightness) so its icon reads
 * on top of the fill, and clear the flag on the others. The fill itself comes from the global highlight
 * restyle.
 */
@Mixin(targets = "mchorse.bbs_mod.ui.framework.elements.layout.UIDockLayout$UIDockStackTabs")
public abstract class UIDockLayoutTabsMixin
{
    @Shadow @Final private List<String> panelIds;
    @Shadow private String activePanelId;

    @Inject(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At("HEAD"))
    private void refreshedui$blackenActiveTabIcon(UIContext context, CallbackInfo ci)
    {
        UITabStrip strip = (UITabStrip) (Object) this;
        int active = this.panelIds.indexOf(this.activePanelId);
        int color = UIContrastColor.onPrimary();

        for (int i = 0, c = strip.getTabCount(); i < c; i++)
        {
            UIElement tab = strip.getTab(i);

            if (tab instanceof UIIcon icon)
            {
                icon.active(i == active).activeColor(color);
            }
        }
    }
}
