package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Dock-stack tabs of the reusable {@link mchorse.bbs_mod.ui.framework.elements.layout.UIDockLayout}
 * (BBS' form-editor redesign extracted the docking system into this shared component; the new
 * particle scheme editor uses it). Its private inner {@code UIDockStackTabs.render} draws the active
 * tab the engine's old way — a {@code UIDashboardPanels.renderHighlight} bevel + a plain white icon.
 * This mirrors {@link UIDockStackTabsMixin} (the film-panel variant) onto the generic layout:
 * <ul>
 *   <li>the active tab's highlight becomes a single rounded primary fill over the whole tab;</li>
 *   <li>the active tab's icon is tinted with the adaptive contrast color (white/black by primary
 *       brightness) so it reads on top of the fill.</li>
 * </ul>
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

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/dashboard/panels/UIDashboardPanels;renderHighlight(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;Lmchorse/bbs_mod/ui/utils/Area;Lmchorse/bbs_mod/utils/Direction;)V")
    )
    private void refreshedui$roundedActiveTab(Batcher2D batcher, Area area, Direction direction)
    {
        RoundedAreas.renderRounded(area, batcher, BBSSettings.primaryColor(Colors.A100), UICornerRadii.buttonsAndTrackpads());
    }
}
