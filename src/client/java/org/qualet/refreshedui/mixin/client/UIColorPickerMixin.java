package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.input.color.UIColorPicker;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.batcher.IColorPickerSwatch;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Color picker window theming + rounding (3.12):
 * <ul>
 *   <li>window background → rounded frame (raisedSurface fill + muted primary border) when rounding on;</li>
 *   <li>preview swatch → rounded box (dividerColor border) + rounded swatch content;</li>
 *   <li>preview / picker / hue / alpha / red outlines: A25 → dividerColor;</li>
 *   <li>slider backdrop: A6 → deepSurface.</li>
 * </ul>
 * Each rounding branch is gated on {@code interfaceChrome() > 0} with a flat themed fallback.
 */
@Mixin(UIColorPicker.class)
public abstract class UIColorPickerMixin implements IColorPickerSwatch
{
    @Shadow
    public Color color;

    @Shadow
    public boolean editAlpha;

    @Shadow
    public abstract void renderRect(Batcher2D batcher, int x1, int y1, int x2, int y2);

    /** Rounded swatch content: solid fill, or checkerboard + horizontal alpha ramp when editing alpha. */
    @Override
    public void renderSwatchRounded(Batcher2D batcher, float x, float y, float w, float h, float radius)
    {
        if (w <= 0F || h <= 0F)
        {
            return;
        }

        float r = Math.min(radius, Math.min(w * 0.5F, h * 0.5F));

        if (!this.editAlpha)
        {
            ((IRoundedBatcher) batcher).roundedBox(x, y, w, h, r, this.color.getARGBColor());

            return;
        }

        ((IRoundedBatcher) batcher).roundedIconArea(Icons.CHECKBOARD, x, y, w, h, r, Colors.WHITE);
        ((IRoundedBatcher) batcher).roundedBoxHorizontalAlpha(x, y, w, h, r, this.color.r, this.color.g, this.color.b, this.color.a);
    }

    /** Window background: rounded frame (raisedSurface + muted primary border), or flat fallback. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundWindow(Area area, Batcher2D batcher, int color)
    {
        int borderColor = Colors.mulRGB(BBSSettings.primaryColor.get() | Colors.A100, 0.7F);

        if (UICornerRadii.interfaceChrome() > 0)
        {
            ((IRoundedBatcher) batcher).roundedFrame(area.x, area.y, area.w, area.h, UICornerRadii.interfaceChromeClamped(area.w, area.h), 1F, borderColor, BBSSettings.raisedSurface());
        }
        else
        {
            area.render(batcher, BBSSettings.raisedSurface());
            batcher.outline(area.x, area.y, area.ex(), area.ey(), borderColor);
        }
    }

    /** Preview swatch: rounded box (dividerColor border) + rounded content, or flat fallback. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/input/color/UIColorPicker;renderRect(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;IIII)V")
    )
    private void refreshedui$roundPreview(UIColorPicker self, Batcher2D batcher, int x1, int y1, int x2, int y2)
    {
        int radius = UICornerRadii.interfaceChrome();
        float w = x2 - x1;
        float h = y2 - y1;

        if (radius > 0)
        {
            float pr = Math.max(0.5F, Math.min(radius, Math.min(w, h) * 0.5F - 0.5F));

            ((IRoundedBatcher) batcher).roundedBox(x1, y1, w, h, pr, BBSSettings.dividerColor());
            this.renderSwatchRounded(batcher, x1 + 1, y1 + 1, w - 2, h - 2, Math.max(0.5F, pr - 1F));
        }
        else
        {
            self.renderRect(batcher, x1, y1, x2, y2);
        }
    }

    /** Preview outline in render(): drop when rounded (the rounded box already drew the border); else dividerColor. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;outline(FFFFI)V")
    )
    private void refreshedui$roundPreviewOutline(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        if (UICornerRadii.interfaceChrome() > 0)
        {
            return;
        }

        batcher.outline(x1, y1, x2, y2, BBSSettings.dividerColor());
    }

    /** Picker / hue / alpha / red field outlines: A25 → dividerColor. */
    @ModifyArg(
        method = {"renderHsv", "renderRgb"},
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;outline(FFFFI)V"),
        index = 4
    )
    private int refreshedui$dividerOutline(int color)
    {
        return BBSSettings.dividerColor();
    }

    /** Slider backdrop fill: A6 → deepSurface. */
    @ModifyArg(
        method = "renderSliderBackdrop",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V"),
        index = 4
    )
    private int refreshedui$deepBackdrop(int color)
    {
        return BBSSettings.deepSurface();
    }
}
