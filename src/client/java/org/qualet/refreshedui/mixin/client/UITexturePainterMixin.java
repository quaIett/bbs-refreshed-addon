package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.dashboard.textures.TexturePaintTool;
import mchorse.bbs_mod.ui.dashboard.textures.UITexturePainter;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Texture painter:
 * <ul>
 *   <li>3.2b — rounds the painter panel background.</li>
 *   <li>active-tool highlight in the tool strip uses our selected-item style: a rounded primary fill
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

    @Shadow private TexturePaintTool activeTool;

    @Redirect(
        method = "renderPanelBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundBackground(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }

    /** The tool icon matching {@code activeTool} (BBS 2.6 dropped its own {@code getActiveToolIcon}). */
    @Unique
    private UIIcon refreshedui$activeToolIcon()
    {
        if (this.activeTool == null)
        {
            return null;
        }

        return switch (this.activeTool)
        {
            case BRUSH -> this.toolIconBrush;
            case ERASER -> this.toolIconEraser;
            case MOVE -> this.toolIconMove;
            case FILL -> this.toolIconFill;
            case PIPETTE -> this.toolIconPipette;
            case SELECTION -> this.toolIconSelection;
        };
    }

    /**
     * Tint the active tool's icon to the adaptive contrast color so it reads on top of the primary fill.
     * In BBS 2.6 each tool icon draws its own highlight ({@code UIIcon.highlight(when, RIGHT)} →
     * {@code Batcher2D.highlight}, rounded by {@code Batcher2DHighlightMixin}), and the old icon-bar
     * pre-render {@code renderActiveToolHighlight} is gone. The panel background renderable draws before
     * the tool strip's children, so flagging the icons here makes the active {@link UIIcon} render in its
     * activeColor the same frame; the rest stay normal.
     */
    @Inject(method = "renderPanelBackground", at = @At("HEAD"))
    private void refreshedui$blackenActiveToolIcon(UIContext context, CallbackInfo ci)
    {
        int onPrimary = UIContrastColor.onPrimary();
        UIIcon active = this.refreshedui$activeToolIcon();
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
