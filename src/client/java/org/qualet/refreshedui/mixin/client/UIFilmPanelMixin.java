package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
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
 *   <li>3.2b — rounded main background. The editor canvas and the per-panel surfaces used to be
 *       rounded from here too (via {@code renderPanelSurfaces}), but BBS 2.4 unified the docking
 *       system into {@code UIDockLayout} and dropped that method — those two redirects now live in
 *       {@link UIDockLayoutMixin} and {@link UIDockSlotMixin}.</li>
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

    /** Active top-bar editor button (camera / replays): adaptive contrast icon (white/black by primary
     * brightness) over the highlight (3.9). BBS 2.6 dropped {@code renderTopBarButton}: the buttons now
     * sit in the shared {@code UIPanelActionBar} as {@code editor(icon, editor::isVisible)} and each
     * {@link UIIcon} paints its own highlight + icon, so the {@code active} flag is refreshed here once per
     * frame from the same visibility the bar highlights by. The fill itself comes from the global
     * highlight restyle. */
    @Inject(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At("HEAD"))
    private void refreshedui$blackenTopBarButton(UIContext context, CallbackInfo ci)
    {
        UIFilmPanel self = (UIFilmPanel) (Object) this;

        refreshedui$tintEditorButton(self.openCameraEditor, self.cameraEditor);
        refreshedui$tintEditorButton(self.openReplayEditor, self.replayEditor);
    }

    private static void refreshedui$tintEditorButton(UIIcon button, UIElement editor)
    {
        if (button != null)
        {
            button.active(editor != null && editor.isVisible()).activeColor(UIContrastColor.onPrimary());
        }
    }
}
