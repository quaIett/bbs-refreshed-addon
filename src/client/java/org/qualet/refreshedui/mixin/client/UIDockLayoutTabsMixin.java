package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Dock-stack tabs of the reusable {@link mchorse.bbs_mod.ui.framework.elements.layout.UIDockLayout}
 * (BBS' form-editor redesign extracted the docking system into this shared component; the new
 * particle scheme editor uses it). Its private inner {@code UIDockStackTabs.render} draws the active
 * tab the engine's old way — a {@code UIDashboardPanels.renderHighlight} bevel + a plain white icon.
 * This mirrors {@link UIDockStackTabsMixin} (the film-panel variant) onto the generic layout:
 * the active tab's icon is tinted with the adaptive contrast color (white/black by primary brightness)
 * so it reads on top of the fill. The rounded fill itself comes from the global
 * {@code UIDashboardPanels.renderHighlight} inject (see {@link UIDashboardPanelsMixin}).
 */
@Mixin(targets = "mchorse.bbs_mod.ui.framework.elements.layout.UIDockLayout$UIDockStackTabs")
public abstract class UIDockLayoutTabsMixin
{
    @ModifyArg(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;icon(Lmchorse/bbs_mod/ui/utils/icons/Icon;IFFFF)V"),
        index = 1
    )
    private int refreshedui$blackenActiveTabIcon(int color, @Local(ordinal = 0) boolean active)
    {
        return active ? UIContrastColor.onPrimary() : color;
    }
}
