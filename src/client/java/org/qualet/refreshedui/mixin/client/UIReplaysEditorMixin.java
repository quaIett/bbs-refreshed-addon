package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.RoundedAreas;
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
 *   <li>3.9 — the active tab's icon draws black (Colors.A100 = opaque black) over the primary
 *       highlight. Updated on every category change (setCategory) rather than per-frame; the initial
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
        for (Map.Entry<UIReplaysEditor.ReplayCategory, UIIcon> entry : this.tabButtons.entrySet())
        {
            entry.getValue().active(entry.getKey() == c).activeColor(Colors.A100);
        }
    }

    /** 3.10b: replace the active-tab gradient with a rounded primary fill over the whole icon area. */
    @Redirect(
        method = "lambda$new$1",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;gradientVBox(FFFFII)V")
    )
    private void refreshedui$roundedActiveTab(Batcher2D batcher, float x1, float y1, float x2, float y2, int topColor, int bottomColor)
    {
        /* gradientVBox spans iconArea.x..ex() and iconArea.y..ey()-2; add the 2px back for the full height. */
        RoundedAreas.roundedBox(batcher, x1, y1, x2 - x1, (y2 + 2F) - y1, UICornerRadii.buttonsAndTrackpads(), BBSSettings.primaryColor(Colors.A100));
    }

    /** 3.10b: drop the 2px bottom bar of the active-tab bevel (the rounded fill replaces it). */
    @Redirect(
        method = "lambda$new$1",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V", ordinal = 1)
    )
    private void refreshedui$dropActiveTabBar(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        /* no-op */
    }
}
