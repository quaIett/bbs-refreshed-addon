package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.tooltips.ITooltip;
import mchorse.bbs_mod.ui.framework.tooltips.LabelTooltip;
import mchorse.bbs_mod.ui.framework.tooltips.UITooltip;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Direction;
import org.joml.Matrix3x2fStack;
import org.qualet.refreshedui.RefreshedUiAddon;
import org.qualet.refreshedui.client.anim.GuiAlpha;
import org.qualet.refreshedui.client.anim.TooltipReveal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code show_tooltips} gate (3.14): when the addon setting is off, hover tooltips never register or
 * render. Default true keeps the original behavior. Null-guarded so tooltips show before the setting
 * is registered.
 *
 * <p>Tooltip reveal: a tooltip waits a moment, then fades in while sliding the last few pixels away from
 * its element ({@link TooltipReveal}). The fade scales the alpha of everything the tooltip records
 * ({@link GuiAlpha}); on 1.21.11 the rounded frame's border is a ring, so no layer shows through another.</p>
 */
@Mixin(UITooltip.class)
public abstract class UITooltipMixin
{
    @Shadow
    public UIElement element;

    @Shadow
    public Area area;

    @org.spongepowered.asm.mixin.Unique
    private static boolean refreshedui$hidden()
    {
        return RefreshedUiAddon.showTooltips != null && !RefreshedUiAddon.showTooltips.get();
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void refreshedui$gateSet(UIContext context, UIElement element, CallbackInfo ci)
    {
        if (refreshedui$hidden())
        {
            this.element = null;
            ci.cancel();
        }
    }

    @Inject(method = "render(Lmchorse/bbs_mod/ui/framework/tooltips/ITooltip;Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At("HEAD"), cancellable = true)
    private void refreshedui$gateRenderTooltip(ITooltip tooltip, UIContext context, CallbackInfo ci)
    {
        if (refreshedui$hidden())
        {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At("HEAD"), cancellable = true)
    private void refreshedui$gateRender(UIContext context, CallbackInfo ci)
    {
        if (refreshedui$hidden())
        {
            this.element = null;
            ci.cancel();

            return;
        }

        if (this.element == null)
        {
            TooltipReveal.hidden();

            return;
        }

        float vis = TooltipReveal.visibility(this.element);

        if (vis >= 1F)
        {
            return;
        }

        ci.cancel();

        if (vis <= 0F)
        {
            return;
        }

        /* Start a few pixels back toward the element and move out along the tooltip's own direction */
        Direction direction = this.element.tooltip instanceof LabelTooltip label && label.direction != null ? label.direction : Direction.TOP;
        float offset = (1F - vis) * TooltipReveal.SLIDE_PX;
        UIElement element = this.element;
        Area area = this.area;

        GuiAlpha.fade(vis, () ->
        {
            Matrix3x2fStack matrices = context.batcher.getContext().getMatrices();

            matrices.pushMatrix();
            matrices.translate(-direction.factorX * offset, -direction.factorY * offset);
            element.renderTooltip(context, area);
            matrices.popMatrix();
        });
    }
}
