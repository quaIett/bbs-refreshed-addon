package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

/**
 * Taskbar:
 * <ul>
 *   <li>3.9 — every panel button gets a per-frame active flag so the active one's icon draws with the
 *       adaptive contrast color (white/black by primary brightness).</li>
 * </ul>
 *
 * <p>The engine-wide rounded selection highlight (3.8) used to be a HEAD inject on the static
 * {@code renderHighlight} here; BBS 2.6 moved that helper to {@code Batcher2D.highlight}, so it now lives in
 * {@link Batcher2DHighlightMixin}.</p>
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
     * Active button: adaptive contrast icon (white/black by primary brightness) over the primary highlight.
     * Wrap the existing pre-render callback so all buttons get their active flag set each frame,
     * then delegate to the original. The highlight itself is drawn by each {@code UIIcon} (rounded via
     * {@link Batcher2DHighlightMixin}).
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
}
