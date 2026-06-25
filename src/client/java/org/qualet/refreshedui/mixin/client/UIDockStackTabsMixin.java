package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Dock-stack window tabs (the row of buttons that appears when two windows share one layout area):
 * <ul>
 *   <li>3.9 — the active tab's icon — drawn inline via Batcher2D.icon rather than a UIIcon element —
 *       is tinted black (Colors.A100 = opaque black) over the primary highlight.</li>
 *   <li>3.10b — the active tab's bevel (a 2px bottom bar + vertical gradient) becomes a single rounded
 *       primary fill over the whole tab, matching the preview / replay category tabs.</li>
 * </ul>
 *
 * <p>Targets the private static inner class {@code UIFilmPanel$UIDockStackTabs}. Its {@code render}
 * draws a background box (ordinal&nbsp;0), then per active tab a 2px box (ordinal&nbsp;1) + gradient.</p>
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
        return active ? Colors.A100 : color;
    }

    /** 3.10b: replace the active-tab gradient with a rounded primary fill over the whole tab. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;gradientVBox(FFFFII)V")
    )
    private void refreshedui$roundedActiveTab(Batcher2D batcher, float x1, float y1, float x2, float y2, int topColor, int bottomColor)
    {
        /* gradientVBox spans x..ex and y..ey-2; add the 2px back for the full tab height. */
        RoundedAreas.roundedBox(batcher, x1, y1, x2 - x1, (y2 + 2F) - y1, UICornerRadii.buttonsAndTrackpads(), BBSSettings.primaryColor(Colors.A100));
    }

    /** 3.10b: drop the 2px bottom bar of the active-tab bevel (the rounded fill replaces it). */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V", ordinal = 1)
    )
    private void refreshedui$dropActiveTabBar(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        /* no-op */
    }
}
