package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Lerps;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * UIToggle M3-style animated switch (3.5).
 *
 * <p>A rounded track whose color interpolates input-surface -&gt; primary, with a circular thumb
 * that slides and brightens, eased over {@value #SWITCH_ANIM_MS}ms. Off state uses the trackpad
 * input surface (6d190214). {@code renderSkin} is a full replacement (see OVERWRITES.md);
 * {@code click}/{@code setValue} are additive and drive the animation state.</p>
 */
@Mixin(UIToggle.class)
public abstract class UIToggleMixin
{
    @Unique private static final int M3_THUMB_OFF = 0xffb0b0b8;
    @Unique private static final int SWITCH_TRACK_W = 28;
    @Unique private static final int SWITCH_TRACK_H = 12;
    @Unique private static final int SWITCH_THUMB_R = 5;
    @Unique private static final int SWITCH_THUMB_SEGMENTS = 28;
    @Unique private static final int SWITCH_ANIM_MS = 200;

    @Shadow private boolean value;
    @Shadow public int color;
    @Shadow public boolean textShadow;
    @Shadow public IKey label;

    /* hover/area/isEnabled live on superclasses (UIClickable/UIElement); inherited @Shadow does
     * not resolve here, so they are reached through a cast to the target type instead (all public,
     * except hover which is recomputed exactly as UIClickable does: area.isInside(context)). */

    /** 0 = off, 1 = on — visual thumb position and color mix. */
    @Unique private float toggleVisual;
    @Unique private float toggleAnimFrom;
    @Unique private float toggleAnimTo;
    @Unique private long toggleAnimStartMs;
    @Unique private Color toggleColorScratch = new Color();

    @Inject(
        method = "<init>(Lmchorse/bbs_mod/l10n/keys/IKey;ZLjava/util/function/Consumer;)V",
        at = @At("RETURN")
    )
    private void refreshedui$initToggle(IKey label, boolean value, Consumer<UIToggle> callback, CallbackInfo ci)
    {
        this.toggleVisual = this.value ? 1F : 0F;
        this.toggleAnimFrom = this.toggleVisual;
        this.toggleAnimTo = this.toggleVisual;
    }

    /**
     * Additive: snap the visual position only when the logical value actually changes, mirroring
     * upstream's early-return guard (repeated setValue with the same state must not reset the anim).
     * The original method still assigns {@code this.value} afterwards.
     */
    @Inject(method = "setValue", at = @At("HEAD"))
    private void refreshedui$syncVisual(boolean value, CallbackInfoReturnable<UIToggle> cir)
    {
        if (this.value != value)
        {
            this.toggleVisual = value ? 1F : 0F;
            this.toggleAnimFrom = this.toggleVisual;
            this.toggleAnimTo = this.toggleVisual;
        }
    }

    /** Additive: start the thumb animation toward the post-click value (original flips it next). */
    @Inject(method = "click", at = @At("HEAD"))
    private void refreshedui$animateClick(int mouseWheel, CallbackInfo ci)
    {
        this.toggleAnimFrom = this.toggleVisual;
        this.toggleAnimTo = !this.value ? 1F : 0F;
        this.toggleAnimStartMs = System.currentTimeMillis();
    }

    @Unique
    private static float easeOutExpo(float t)
    {
        return t >= 1F ? 1F : 1F - (float) Math.pow(2F, -10F * t);
    }

    @Unique
    private void stepToggleAnimation()
    {
        if (this.toggleVisual == this.toggleAnimTo)
        {
            return;
        }

        float t = (System.currentTimeMillis() - this.toggleAnimStartMs) / (float) SWITCH_ANIM_MS;

        if (t >= 1F)
        {
            this.toggleVisual = this.toggleAnimTo;
        }
        else
        {
            this.toggleVisual = Lerps.lerp(this.toggleAnimFrom, this.toggleAnimTo, easeOutExpo(t));
        }
    }

    @Inject(method = "renderSkin", at = @At("HEAD"), cancellable = true)
    private void refreshedui$renderSwitch(UIContext context, CallbackInfo ci)
    {
        this.stepToggleAnimation();

        UIToggle self = (UIToggle) (Object) this;
        Area area = self.area;
        boolean hover = area.isInside(context);

        FontRenderer font = context.batcher.getFont();
        int labelMax = area.w - SWITCH_TRACK_W - 8;
        String label = font.limitToWidth(this.label.get(), Math.max(8, labelMax));

        context.batcher.text(label, area.x, area.my(font.getHeight()), this.color, this.textShadow);

        float trackX = area.ex() - SWITCH_TRACK_W - 2;
        float trackY = area.my() - SWITCH_TRACK_H * 0.5F;
        float trackR = SWITCH_TRACK_H * 0.5F;

        int primary = BBSSettings.primaryColor.get() | Colors.A100;
        int offFill = BBSSettings.inputSurface();

        if (hover)
        {
            primary = Colors.mulRGB(primary, 1.05F);
            offFill = Colors.mulRGB(offFill, 1.08F);
        }

        Colors.interpolate(this.toggleColorScratch, offFill, primary, this.toggleVisual, false);

        IRoundedBatcher batcher = (IRoundedBatcher) context.batcher;

        batcher.roundedBox(trackX, trackY, SWITCH_TRACK_W, SWITCH_TRACK_H, trackR, this.toggleColorScratch.getARGBColor());

        /* Thumb travels along inner track */
        float pad = 2F;
        float travel = SWITCH_TRACK_W - 2F * pad - 2F * SWITCH_THUMB_R;
        float thumbCx = trackX + pad + SWITCH_THUMB_R + travel * this.toggleVisual;
        float thumbCy = area.my();

        Colors.interpolate(this.toggleColorScratch, M3_THUMB_OFF, Colors.WHITE, this.toggleVisual, false);
        int thumbBody = this.toggleColorScratch.getARGBColor();

        batcher.filledCircle(thumbCx, thumbCy, SWITCH_THUMB_R, thumbBody, SWITCH_THUMB_SEGMENTS);

        if (!self.isEnabled())
        {
            batcher.roundedBox(trackX, trackY, SWITCH_TRACK_W, SWITCH_TRACK_H, trackR, Colors.A50);

            context.batcher.outlinedIcon(Icons.LOCKED, area.ex() - SWITCH_TRACK_W / 2F - 2, thumbCy, 0.5F, 0.5F);
        }

        ci.cancel();
    }
}
