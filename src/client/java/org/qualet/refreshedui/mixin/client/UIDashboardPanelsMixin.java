package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

/**
 * Taskbar:
 * <ul>
 *   <li>3.8 — active-panel highlight uses a rounded primary fill instead of the bevel highlight;</li>
 *   <li>3.9 — every panel button gets a per-frame active flag so the active one's icon draws black.</li>
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

    @Redirect(
        method = "*",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/dashboard/panels/UIDashboardPanels;renderHighlight(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;Lmchorse/bbs_mod/ui/utils/Area;)V")
    )
    private void refreshedui$roundHighlight(Batcher2D batcher, Area area)
    {
        RoundedAreas.renderRounded(area, batcher, BBSSettings.primaryColor(Colors.A100), UICornerRadii.buttonsAndTrackpads());
    }

    /**
     * Active button: black icon over the primary highlight (Colors.A100 = opaque black).
     * Wrap the existing pre-render callback so all buttons get their active flag set each frame,
     * then delegate to the original (which still draws the rounded highlight via the redirect above).
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$blackenActivePanelIcons(CallbackInfo ci)
    {
        Consumer<UIContext> original = this.panelButtons.preRenderCallback;

        this.panelButtons.preRender((context) ->
        {
            for (int i = 0, c = this.panels.size(); i < c; i++)
            {
                UIIcon button = (UIIcon) this.panelButtons.getChildren().get(i);

                button.active(this.panel == this.panels.get(i)).activeColor(Colors.A100);
            }

            if (original != null)
            {
                original.accept(context);
            }
        });
    }
}
