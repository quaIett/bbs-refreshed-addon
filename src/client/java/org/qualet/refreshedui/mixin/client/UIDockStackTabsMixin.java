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
 * Dock-stack window tabs (the row of buttons that appears when two windows share one layout area):
 * <ul>
 *   <li>3.9 — the active tab's icon — drawn inline via Batcher2D.icon rather than a UIIcon element —
 *       is tinted with the adaptive contrast color (white/black by primary brightness) over the
 *       primary highlight.</li>
 *   <li>3.10b — the active tab's bevel (a 2px bottom bar + vertical gradient) becomes a single rounded
 *       primary fill over the whole tab, matching the preview / replay category tabs.</li>
 * </ul>
 *
 * <p>Targets the private static inner class {@code UIFilmPanel$UIDockStackTabs}. Its {@code render}
 * draws a background box, then per active tab calls {@code UIDashboardPanels.renderHighlight} (BBS 2.3).</p>
 */
@Mixin(targets = "mchorse.bbs_mod.ui.film.UIFilmPanel$UIDockStackTabs")
public abstract class UIDockStackTabsMixin
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

    /**
     * 3.10b: replace the active-tab highlight with a rounded primary fill over the whole tab.
     * BBS 2.3 draws it via {@code UIDashboardPanels.renderHighlight(batcher, Area.SHARED, Direction)}
     * (Area.SHARED is pre-set to the full tab rect), so a single redirect of that call replaces the
     * old gradientVBox + 2px-bar bevel.
     */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/dashboard/panels/UIDashboardPanels;renderHighlight(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;Lmchorse/bbs_mod/ui/utils/Area;Lmchorse/bbs_mod/utils/Direction;)V")
    )
    private void refreshedui$roundedActiveTab(Batcher2D batcher, Area area, Direction direction)
    {
        RoundedAreas.renderRounded(area, batcher, BBSSettings.primaryColor(Colors.A100), UICornerRadii.buttonsAndTrackpads());
    }
}
