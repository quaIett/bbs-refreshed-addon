package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.tooltips.styles.DarkTooltipStyle;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Dark tooltip background (3.2a rounding, 3.6 shadow->outline): rounded frame with muted primary border. */
@Mixin(DarkTooltipStyle.class)
public abstract class DarkTooltipStyleMixin
{
    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundedFrame(Area area, Batcher2D batcher, int color)
    {
        int border = Colors.mulRGB(BBSSettings.primaryColor.get() | Colors.A100, 0.7F);

        ((IRoundedBatcher) batcher).roundedFrame(area.x, area.y, area.w, area.h,
            UICornerRadii.interfaceChromeClamped(area.w, area.h), 1F, border, color);
    }

    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;dropShadow(IIIIIII)V")
    )
    private void refreshedui$noShadow(Batcher2D batcher, int left, int top, int right, int bottom, int offset, int opaque, int shadow)
    {
    }
}
