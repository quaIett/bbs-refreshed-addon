package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * UIButton theme (3.3): black text without shadow (the upstream field defaults were
 * {@code WHITE}/{@code true}), and a rounded background fill instead of the beveled box.
 * When rounding is off the bevel is preserved.
 */
@Mixin(UIButton.class)
public abstract class UIButtonMixin
{
    @Shadow
    public int textColor;

    @Shadow
    public boolean textShadow;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void refreshedui$blackText(CallbackInfo ci)
    {
        this.textColor = Colors.A100;
        this.textShadow = false;
    }

    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;bevelBox(IIIIIZZ)V")
    )
    private void refreshedui$roundBackground(Batcher2D batcher, int x1, int y1, int x2, int y2, int fill, boolean shadow, boolean border)
    {
        int radius = UICornerRadii.buttonsAndTrackpads();

        if (radius > 0)
        {
            ((IRoundedBatcher) batcher).roundedBox(x1, y1, x2 - x1, y2 - y1, radius, fill);
        }
        else
        {
            batcher.bevelBox(x1, y1, x2, y2, fill, shadow, border);
        }
    }
}
