package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
 * </ul>
 *
 * <p>The active-tab rounded fill is no longer redirected here — it comes from the global
 * {@code UIDashboardPanels.renderHighlight} inject (see {@link UIDashboardPanelsMixin}). This mixin
 * only keeps the per-category icon contrast tint, which the global helper can't do.</p>
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
}
