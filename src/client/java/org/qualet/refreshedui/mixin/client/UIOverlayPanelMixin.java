package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Overlay panel background (3.2a rounding, 3.6 shadow->outline).
 * {@code renderBackground} draws panel (ordinal 0), icons strip (ordinal 1), close button (ordinal 2):
 * the panel becomes a rounded frame with a muted primary border (or fill+outline when rounding is off),
 * and the icons strip gets a 1px inset to sit inside that border. The drop shadow is dropped.
 */
@Mixin(UIOverlayPanel.class)
public abstract class UIOverlayPanelMixin
{
    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;dropShadow(IIIIIII)V")
    )
    private void refreshedui$noShadow(Batcher2D batcher, int left, int top, int right, int bottom, int offset, int opaque, int shadow)
    {
    }

    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V", ordinal = 0)
    )
    private void refreshedui$panelFrame(Area area, Batcher2D batcher, int color)
    {
        int radius = UICornerRadii.interfaceChrome();
        int border = Colors.mulRGB(BBSSettings.primaryColor.get() | Colors.A100, 0.7F);

        if (radius > 0)
        {
            ((IRoundedBatcher) batcher).roundedFrame(area.x, area.y, area.w, area.h, radius, 1F, border, color);
        }
        else
        {
            area.render(batcher, color);
            batcher.outline(area.x, area.y, area.ex(), area.ey(), border);
        }
    }

    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V", ordinal = 1)
    )
    private void refreshedui$iconsInset(Area area, Batcher2D batcher, int color)
    {
        batcher.box(area.x, area.y + 1, area.ex() - 1, area.ey() - 1, color);
    }
}
