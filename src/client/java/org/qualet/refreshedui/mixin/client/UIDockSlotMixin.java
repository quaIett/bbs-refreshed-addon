package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;

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
 *
 * <p>Rounding the surface alone is invisible under a panel that paints opaquely over the whole slot —
 * the clips and actions timelines, the replay editor's category bar, the keyframe editor. So after the
 * children (and the inset shadow) the slot cuts its own corners back out in the dock canvas colour, which
 * is what the 4px gap between slots shows anyway. Frameless slots (the film preview viewport) keep their
 * square edges, as they have no surface to round in the first place.</p>
 */
@Mixin(targets = "mchorse.bbs_mod.ui.framework.elements.layout.UIDockLayout$UIDockSlot")
public abstract class UIDockSlotMixin extends UIElement
{
    @Shadow
    public abstract boolean isFramed();

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundSlotSurface(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void refreshedui$cutSlotCorners(UIContext context, CallbackInfo ci)
    {
        if (!this.isFramed())
        {
            return;
        }

        Area area = this.area;

        ((IRoundedBatcher) context.batcher).roundedCornerCut(area.x, area.y, area.w, area.h,
            UICornerRadii.interfaceChrome(), BBSSettings.baseSurface());
    }
}
