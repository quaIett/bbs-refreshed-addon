package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.OverlaySizes;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Film panel theme:
 * <ul>
 *   <li>3.2b — rounded base surfaces (main bg, editor + child panels);</li>
 *   <li>3.7 — film move / player-settings / details overlays use their fixed default size
 *       (route the explicit-size addOverlay calls to the no-size form when the panel is mapped;
 *       the undo-history overlay stays at its explicit size since it is not mapped).</li>
 * </ul>
 */
@Mixin(UIFilmPanel.class)
public abstract class UIFilmPanelMixin
{
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V", ordinal = 0)
    )
    private void refreshedui$roundMainBackground(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }

    @Redirect(
        method = "renderPanelSurfaces",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundPanelSurface(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }

    @Redirect(
        method = "*",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlay;addOverlay(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlayPanel;IF)Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlay;")
    )
    private UIOverlay refreshedui$defaultSizeIF(UIContext context, UIOverlayPanel panel, int w, float h)
    {
        return OverlaySizes.sizeFor(panel) != null
            ? UIOverlay.addOverlay(context, panel)
            : UIOverlay.addOverlay(context, panel, w, h);
    }

    @Redirect(
        method = "*",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlay;addOverlay(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlayPanel;II)Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlay;")
    )
    private UIOverlay refreshedui$defaultSizeII(UIContext context, UIOverlayPanel panel, int w, int h)
    {
        return OverlaySizes.sizeFor(panel) != null
            ? UIOverlay.addOverlay(context, panel)
            : UIOverlay.addOverlay(context, panel, w, h);
    }

    /** Top-bar editor tab active highlight: rounded primary fill (3.8). */
    @Redirect(
        method = "*",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/dashboard/panels/UIDashboardPanels;renderHighlight(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;Lmchorse/bbs_mod/ui/utils/Area;)V")
    )
    private void refreshedui$roundTabHighlight(Batcher2D batcher, Area area)
    {
        RoundedAreas.renderRounded(area, batcher, BBSSettings.primaryColor(Colors.A100), UICornerRadii.buttonsAndTrackpads());
    }

    /** Active top-bar editor tab: adaptive contrast icon (white/black by primary brightness) over the highlight (3.9). */
    @Inject(method = "renderTopBarButton", at = @At("HEAD"))
    private void refreshedui$blackenTopBarButton(UIContext context, UIIcon button, boolean active, CallbackInfo ci)
    {
        if (button != null)
        {
            button.active(active).activeColor(UIContrastColor.onPrimary());
        }
    }
}
