package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Dock-stack window tabs (the row of buttons that appears when two windows share one layout area):
 * <ul>
 *   <li>3.9 — the active tab's icon — drawn inline via Batcher2D.icon rather than a UIIcon element —
 *       is tinted with the adaptive contrast color (white/black by primary brightness) over the
 *       primary highlight.</li>
 * </ul>
 *
 * <p>The rounded fill of the active tab itself is no longer redirected here — it comes from the global
 * {@code UIDashboardPanels.renderHighlight} inject (see {@link UIDashboardPanelsMixin}). This mixin only
 * needs the inline-icon contrast tint, which the global helper can't do.</p>
 *
 * <p>Targets the private static inner class {@code UIFilmPanel$UIDockStackTabs}.</p>
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
}
