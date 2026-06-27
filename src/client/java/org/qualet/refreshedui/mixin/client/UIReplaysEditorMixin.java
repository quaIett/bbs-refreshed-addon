package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Replay category tabs (position / pose / …):
 * <ul>
 *   <li>3.9 — the active tab's icon draws with the adaptive contrast color (white/black by primary
 *       brightness) over the primary highlight. Updated on every category change (setCategory) rather
 *       than per-frame; the initial
 *       {@code setCategory(PLAYER)} call in the constructor seeds the flags after the buttons are
 *       created.</li>
 *   <li>3.10b — the active-tab indicator's bevel (a 2px bottom bar + vertical gradient) becomes a
 *       single rounded primary fill over the whole icon area, matching the preview / dock tabs.</li>
 * </ul>
 *
 * <p>The indicator is drawn manually inside the icon-bar {@code UIRenderable}, compiled to the
 * synthetic {@code lambda$new$1(UIContext)} (the only {@code <init>} lambda that paints — bg box
 * ordinal&nbsp;0, indicator 2px box ordinal&nbsp;1, indicator gradient). Targeting the lambda by
 * name keeps the redirects off the unrelated clip-background {@code gradientVBox} elsewhere in the
 * class; it is pinned to the clean-master dependency jar.</p>
 */
@Mixin(UIReplaysEditor.class)
public abstract class UIReplaysEditorMixin
{
    @Shadow
    public Map<UIReplaysEditor.ReplayCategory, UIIcon> tabButtons;

    @Inject(method = "setCategory", at = @At("TAIL"))
    private void refreshedui$blackenActiveTab(UIReplaysEditor.ReplayCategory c, CallbackInfo ci)
    {
        int activeColor = UIContrastColor.onPrimary();

        for (Map.Entry<UIReplaysEditor.ReplayCategory, UIIcon> entry : this.tabButtons.entrySet())
        {
            entry.getValue().active(entry.getKey() == c).activeColor(activeColor);
        }
    }

    /**
     * 3.10b: BBS 2.3 routes the active-category indicator through the shared
     * {@link UIDashboardPanels#renderHighlight} helper (was a manual gradient + 2px bottom bar). Replace
     * that call — scoped to the icon-bar pre-render lambda ({@code lambda$new$1}) — with a single rounded
     * primary fill over the active icon, matching the dashboard taskbar / dock tabs.
     */
    @Redirect(
        method = "lambda$new$1",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/dashboard/panels/UIDashboardPanels;renderHighlight(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;Lmchorse/bbs_mod/ui/utils/Area;Lmchorse/bbs_mod/utils/Direction;)V")
    )
    private void refreshedui$roundedActiveTab(Batcher2D batcher, Area area, Direction direction)
    {
        RoundedAreas.renderRounded(area, batcher, BBSSettings.primaryColor(Colors.A100), UICornerRadii.buttonsAndTrackpads());
    }
}
