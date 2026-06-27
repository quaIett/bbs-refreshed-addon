package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.anim.PanelTransitions;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

/**
 * Taskbar + the engine-wide selection highlight:
 * <ul>
 *   <li>3.8 — every selection highlight uses a rounded primary fill instead of the bevel. BBS funnels
 *       ALL selection indicators through the static {@link UIDashboardPanels#renderHighlight} helper, so
 *       one HEAD inject that cancels its body and draws the rounded fill covers every call site
 *       (taskbar, dock tabs, UIIcons, replay / tool / form-editor tabs, settings sidebar, control bars,
 *       context menus…) — no per-site redirects needed;</li>
 *   <li>3.9 — every panel button gets a per-frame active flag so the active one's icon draws with the
 *       adaptive contrast color (white/black by primary brightness).</li>
 * </ul>
 */
@Mixin(UIDashboardPanels.class)
public abstract class UIDashboardPanelsMixin
{
    @Shadow
    public List<UIDashboardPanel> panels;

    @Shadow
    public UIScrollView panelButtons;

    @Shadow
    public UIDashboardPanel panel;

    /**
     * 3.8 — the ONE place the rounded selection fill is applied. BBS routes every selection indicator
     * through this static helper (the 2-arg {@code renderHighlight} / {@code renderHighlightHorizontal}
     * delegators call the 3-arg form too), so cancelling its body here and drawing the rounded primary
     * fill replaces the bevel everywhere at once — no per-call-site redirects. Direction is intentionally
     * ignored: our fill is a uniform rounded pill, not an edge bar.
     */
    @Inject(
        method = "renderHighlight(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;Lmchorse/bbs_mod/ui/utils/Area;Lmchorse/bbs_mod/utils/Direction;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void refreshedui$roundHighlight(Batcher2D batcher, Area area, Direction direction, CallbackInfo ci)
    {
        RoundedAreas.renderRounded(area, batcher, BBSSettings.primaryColor(Colors.A100), UICornerRadii.buttonsAndTrackpads());
        ci.cancel();
    }

    /**
     * Active button: adaptive contrast icon (white/black by primary brightness) over the primary highlight.
     * Wrap the existing pre-render callback so all buttons get their active flag set each frame,
     * then delegate to the original (which still draws the rounded highlight via the redirect above).
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$blackenActivePanelIcons(CallbackInfo ci)
    {
        Consumer<UIContext> original = this.panelButtons.preRenderCallback;

        this.panelButtons.preRender((context) ->
        {
            int activeColor = UIContrastColor.onPrimary();

            for (int i = 0, c = this.panels.size(); i < c; i++)
            {
                UIIcon button = (UIIcon) this.panelButtons.getChildren().get(i);

                button.active(this.panel == this.panels.get(i)).activeColor(activeColor);
            }

            if (original != null)
            {
                original.accept(context);
            }
        });
    }

    /**
     * Switching top-level dashboard panels (Morphing / Film / Model Blocks / ...) arms the appear reveal
     * over the newly shown panel's subtree — its text staggers in (see {@link PanelTransitions}).
     */
    @Inject(method = "setPanel", at = @At("TAIL"))
    private void refreshedui$animatePanelAppear(UIDashboardPanel panel, CallbackInfo ci)
    {
        if (panel != null)
        {
            PanelTransitions.onPanelAppear(panel);
        }
    }
}
