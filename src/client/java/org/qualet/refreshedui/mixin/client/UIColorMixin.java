package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.color.UIColorPicker;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.batcher.IColorPickerSwatch;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Color field swatch (color picker button):
 * <ul>
 *   <li>3.2a — round the hover overlay;</li>
 *   <li>3.12 — rounded swatch with a 1px A25 border + rounded swatch content; the hover overlay
 *       insets 1px and uses the inner radius when rounding is on.</li>
 * </ul>
 */
@Mixin(UIColor.class)
public abstract class UIColorMixin
{
    /** Swatch: rounded A25 border + rounded picker content (3.12), or the original flat rect. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/input/color/UIColorPicker;renderRect(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;IIII)V")
    )
    private void refreshedui$roundSwatch(UIColorPicker picker, Batcher2D batcher, int x1, int y1, int x2, int y2)
    {
        int radius = UICornerRadii.interfaceChrome();

        if (radius > 0)
        {
            float w = x2 - x1;
            float h = y2 - y1;
            float r = Math.max(0.5F, Math.min(radius, Math.min(w, h) * 0.5F - 0.5F));
            float inner = Math.max(0.5F, r - 1F);

            ((IRoundedBatcher) batcher).roundedBox(x1, y1, w, h, r, Colors.A25);
            ((IColorPickerSwatch) picker).renderSwatchRounded(batcher, x1 + 1, y1 + 1, w - 2, h - 2, inner);
        }
        else
        {
            picker.renderRect(batcher, x1, y1, x2, y2);
        }
    }

    /** Hover overlay (3.2a + 3.12): inset 1px with the inner radius when rounding on, else original. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;II)V")
    )
    private void refreshedui$roundHover(Area area, Batcher2D batcher, int color, int offset)
    {
        int radius = UICornerRadii.interfaceChrome();

        if (radius > 0)
        {
            float r = Math.max(0.5F, Math.min(radius, Math.min(area.w, area.h) * 0.5F - 0.5F));
            float inner = Math.max(0.5F, r - 1F);

            RoundedAreas.renderRounded(area, batcher, color, inner, 1, 1, 1, 1);
        }
        else
        {
            RoundedAreas.renderRounded(area, batcher, color, radius, offset);
        }
    }
}
