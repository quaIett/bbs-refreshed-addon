package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.layout.UIDockLayout;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Rounds the dock canvas — the base surface painted behind the docked slots (3.2b).
 *
 * <p>This used to live in {@code UIFilmPanel.renderPanelSurfaces}, which BBS 2.4 removed when the
 * docking system was unified into the shared {@link UIDockLayout}; the canvas half of that method is
 * now {@code renderCanvas}, and the per-panel half is {@code UIDockLayout$UIDockSlot.render}
 * (see {@link UIDockSlotMixin}).</p>
 */
@Mixin(UIDockLayout.class)
public abstract class UIDockLayoutMixin
{
    @Redirect(
        method = "renderCanvas",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundCanvas(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }
}
