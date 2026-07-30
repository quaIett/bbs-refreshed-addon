package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Rounds the recessed surface each docked panel paints behind itself (3.2b).
 *
 * <p>The other half of the old {@code UIFilmPanel.renderPanelSurfaces} redirect — BBS 2.4 moved the
 * per-panel loop into the private inner {@code UIDockLayout$UIDockSlot}, which paints its own
 * {@code deepSurface} before rendering children. The canvas behind the slots is handled by
 * {@link UIDockLayoutMixin}.</p>
 *
 * <p>Only the surface is redirected; the inset shadow drawn after the children uses gradient boxes
 * and is left alone.</p>
 */
@Mixin(targets = "mchorse.bbs_mod.ui.framework.elements.layout.UIDockLayout$UIDockSlot")
public abstract class UIDockSlotMixin
{
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundSlotSurface(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }
}
