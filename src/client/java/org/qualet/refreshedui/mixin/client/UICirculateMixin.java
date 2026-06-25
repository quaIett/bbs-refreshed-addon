package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * UICirculate theme:
 * <ul>
 *   <li>3.2a — rounded background ({@code bevelBox} kept when rounding is off);</li>
 *   <li>3.4 — black label without shadow ({@code textShadow} -> {@code text}, color A100).</li>
 * </ul>
 */
@Mixin(UICirculate.class)
public abstract class UICirculateMixin
{
    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;bevelBox(IIIIIZZ)V")
    )
    private void refreshedui$roundBackground(Batcher2D batcher, int x1, int y1, int x2, int y2, int fill, boolean shadow, boolean border)
    {
        int radius = UICornerRadii.interfaceChrome();

        if (radius > 0)
        {
            ((IRoundedBatcher) batcher).roundedBox(x1, y1, x2 - x1, y2 - y1, radius, fill);
        }
        else
        {
            batcher.bevelBox(x1, y1, x2, y2, fill, shadow, border);
        }
    }

    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;textShadow(Ljava/lang/String;FFI)V")
    )
    private void refreshedui$blackLabel(Batcher2D batcher, String label, float x, float y, int color)
    {
        batcher.text(label, x, y, Colors.A100);
    }
}
