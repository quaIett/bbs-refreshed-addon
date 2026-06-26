package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.dashboard.textures.UITexturePainter;
import mchorse.bbs_mod.ui.framework.UIContext;
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

/**
 * Texture painter:
 * <ul>
 *   <li>3.2b — rounds the painter panel background.</li>
 *   <li>active-tool highlight in the tool bar uses our selected-item style: a rounded primary fill
 *       with the active tool's icon tinted to the adaptive contrast color (mirrors the taskbar's
 *       active-panel button, see {@link UIDashboardPanelsMixin}).</li>
 * </ul>
 */
@Mixin(UITexturePainter.class)
public abstract class UITexturePainterMixin
{
    @Shadow private UIIcon toolIconBrush;
    @Shadow private UIIcon toolIconEraser;
    @Shadow private UIIcon toolIconMove;
    @Shadow private UIIcon toolIconFill;
    @Shadow private UIIcon toolIconPipette;
    @Shadow private UIIcon toolIconSelection;

    @Shadow protected abstract UIIcon getActiveToolIcon();

    @Redirect(
        method = "renderPanelBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundBackground(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }

    /** Round the active-tool highlight into our primary pill. */
    @Redirect(
        method = "renderActiveToolHighlight",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/dashboard/panels/UIDashboardPanels;renderHighlight(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;Lmchorse/bbs_mod/ui/utils/Area;Lmchorse/bbs_mod/utils/Direction;)V")
    )
    private void refreshedui$roundedActiveTool(Batcher2D batcher, Area area, Direction direction)
    {
        RoundedAreas.renderRounded(area, batcher, BBSSettings.primaryColor(Colors.A100), UICornerRadii.buttonsAndTrackpads());
    }

    /**
     * Tint the active tool's icon to the adaptive contrast color so it reads on top of the primary fill.
     * The highlight is an icon-bar pre-render, so flagging the icons here (before the bar's children draw)
     * makes the active {@link UIIcon} render in its activeColor; the rest stay normal.
     */
    @Inject(method = "renderActiveToolHighlight", at = @At("HEAD"))
    private void refreshedui$blackenActiveToolIcon(UIContext context, CallbackInfo ci)
    {
        int onPrimary = UIContrastColor.onPrimary();
        UIIcon active = this.getActiveToolIcon();
        UIIcon[] tools = {
            this.toolIconBrush, this.toolIconEraser, this.toolIconMove,
            this.toolIconFill, this.toolIconPipette, this.toolIconSelection
        };

        for (UIIcon tool : tools)
        {
            if (tool != null)
            {
                tool.active(tool == active).activeColor(onPrimary);
            }
        }
    }
}
