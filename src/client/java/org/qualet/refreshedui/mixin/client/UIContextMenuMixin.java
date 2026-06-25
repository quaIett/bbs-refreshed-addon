package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Context menu background (3.2a rounding, 3.6 shadow->outline): a rounded frame with a muted primary
 * border over the raised surface, replacing the drop shadow + flat rounded fill.
 */
@Mixin(UIContextMenu.class)
public abstract class UIContextMenuMixin
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

    /** Shadow replaced by the primary outline above. */
    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;dropShadow(IIIIIII)V")
    )
    private void refreshedui$noShadow(Batcher2D batcher, int left, int top, int right, int bottom, int offset, int opaque, int shadow)
    {
    }
}
