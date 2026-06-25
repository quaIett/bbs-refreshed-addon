package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.colors.Colors;
import org.objectweb.asm.Opcodes;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * UIButton theme (3.3 + 3.17): no text shadow (the upstream default was {@code true}) and a
 * rounded background fill instead of the beveled box (the bevel is preserved when rounding is off).
 *
 * <p>Text color is <b>adaptive</b>: it picks white or black per frame based on the perceived
 * brightness of the button's fill (the BBS primary color, or a {@code .color(...)} custom color).
 * Dark fills get white text, light fills get black text — so the label stays readable for any
 * primary color. Buttons that opt into an explicit text color via {@code setColor}/{@code textColor}
 * keep it; transparent buttons ({@code background == false}) are left untouched.</p>
 */
@Mixin(UIButton.class)
public abstract class UIButtonMixin
{
    @Shadow
    public int textColor;

    @Shadow
    public boolean textShadow;

    @Shadow
    public boolean custom;

    @Shadow
    public int customColor;

    @Shadow
    public boolean background;

    /** When {@code true} the label color is chosen automatically; cleared by an explicit text color. */
    @Unique
    private boolean refreshedui$autoTextColor = true;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void refreshedui$noTextShadow(CallbackInfo ci)
    {
        this.textColor = Colors.A100;
        this.textShadow = false;
        this.refreshedui$autoTextColor = true;
    }

    @Inject(method = "setColor", at = @At("HEAD"))
    private void refreshedui$onSetColor(int color, boolean shadow, CallbackInfo ci)
    {
        this.refreshedui$autoTextColor = false;
    }

    @Inject(method = "textColor", at = @At("HEAD"))
    private void refreshedui$onTextColor(int color, boolean shadow, CallbackInfoReturnable<UIButton> cir)
    {
        this.refreshedui$autoTextColor = false;
    }

    /**
     * Replace the read of {@code this.textColor} inside {@code renderSkin} with an adaptive
     * white/black base color. The surrounding {@code Colors.mulRGB(..., hover ? 0.9F : 1F)} still
     * applies the hover dimming, so we only decide the base tone here.
     */
    @ModifyExpressionValue(
        method = "renderSkin",
        at = @At(
            value = "FIELD",
            target = "Lmchorse/bbs_mod/ui/framework/elements/buttons/UIButton;textColor:I",
            opcode = Opcodes.GETFIELD
        )
    )
    private int refreshedui$adaptiveTextColor(int original)
    {
        if (!this.refreshedui$autoTextColor || !this.background)
        {
            return original;
        }

        int fill = this.custom ? this.customColor : (BBSSettings.primaryColor.get() | Colors.A100);

        return UIContrastColor.contrastOn(fill);
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
