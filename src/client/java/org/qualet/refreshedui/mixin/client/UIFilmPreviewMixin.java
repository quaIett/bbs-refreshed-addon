package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.UIFilmPreview;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Preview control bar:
 * <ul>
 *   <li>3.9 — active buttons draw the adaptive contrast icon (white/black by primary brightness) over
 *       the highlight. The icon glyphs are drawn by the UIIcon children during {@code super.render(...)},
 *       after this HEAD inject.</li>
 *   <li>3.10 — the bar gradient becomes a rounded chromeSurface bar, the 5 active highlights become
 *       rounded fills, and the bar is offset up from the bottom edge.</li>
 * </ul>
 */
@Mixin(UIFilmPreview.class)
public abstract class UIFilmPreviewMixin
{
    /** Bar shell expands this many px beyond the icon row on each side (refreshed). */
    private static final int REFRESHEDUI_BAR_EDGE_GAP = 2;
    /** Distance from preview bottom edge to the controls bar, px (refreshed). */
    private static final int REFRESHEDUI_TOOLBAR_MARGIN_FROM_EDGE_PX = 7;

    @Shadow
    private UIFilmPanel panel;

    @Shadow
    public UIElement icons;

    @Shadow
    public UIIcon onionSkin;

    @Shadow
    public UIIcon flight;

    @Shadow
    public UIIcon control;

    @Shadow
    public UIIcon recordReplay;

    @Shadow
    public UIIcon recordVideo;

    /** 3.9: active control buttons render the adaptive contrast icon over the highlight. */
    @Inject(method = "render", at = @At("HEAD"))
    private void refreshedui$blackenActiveControls(UIContext context, CallbackInfo ci)
    {
        int active = UIContrastColor.onPrimary();

        this.flight.active(this.panel.isFlying()).activeColor(active);
        this.control.active(this.panel.getController().isControlling()).activeColor(active);
        this.recordReplay.active(this.panel.getController().isRecording()).activeColor(active);
        this.recordVideo.active(this.panel.recorder.isRecording()).activeColor(active);
        this.onionSkin.active(this.panel.getController().getOnionSkin().enabled.get()).activeColor(active);
    }

    /** 3.10: offset the control bar up from the preview bottom edge. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$offsetIconBar(CallbackInfo ci)
    {
        this.icons.y(1F, -REFRESHEDUI_TOOLBAR_MARGIN_FROM_EDGE_PX);
    }

    /** 3.10: replace the gradient bar with a rounded chromeSurface shell. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;gradientVBox(FFFFII)V")
    )
    private void refreshedui$roundedBar(Batcher2D batcher, float x1, float y1, float x2, float y2, int topColor, int bottomColor)
    {
        float px = x1 - REFRESHEDUI_BAR_EDGE_GAP;
        float py = y1 - REFRESHEDUI_BAR_EDGE_GAP;
        float pw = (x2 - x1) + 2F * REFRESHEDUI_BAR_EDGE_GAP;
        float ph = (y2 - y1) + 2F * REFRESHEDUI_BAR_EDGE_GAP;

        if (pw > 1F && ph > 1F)
        {
            /* Capsule radius (half height vs half width), then a quarter of that for subtle corners. */
            float capsuleR = Math.min(ph * 0.5F - 0.5F, pw * 0.5F - 0.5F);
            float rr = capsuleR * 0.25F;

            ((IRoundedBatcher) batcher).roundedBox(px, py, pw, ph, Math.max(1F, rr), BBSSettings.chromeSurface());
        }
    }

    /** 3.10: active control highlight uses a rounded primary fill instead of the bevel highlight. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/dashboard/panels/UIDashboardPanels;renderHighlight(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;Lmchorse/bbs_mod/ui/utils/Area;)V")
    )
    private void refreshedui$roundedHighlight(Batcher2D batcher, Area area)
    {
        RoundedAreas.renderRounded(area, batcher, BBSSettings.primaryColor(Colors.A100), UICornerRadii.buttonsAndTrackpads());
    }
}
