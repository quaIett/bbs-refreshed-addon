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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Color picker window theming + rounding (3.12):
 * <ul>
 *   <li>window background → rounded frame (raisedSurface fill + muted primary border) when rounding on;</li>
 *   <li>preview (BBS 2.6: split initial | current swatch) → one rounded box (dividerColor border) with the
 *       left half rounded on the left and the right half rounded on the right;</li>
 *   <li>preview seam / outline and picker / hue / alpha / red outlines: A25 → dividerColor.</li>
 * </ul>
 * Each rounding branch is gated on {@code interfaceChrome() > 0} with a flat themed fallback.
 * (The old slider-backdrop A6 → deepSurface remap is upstream in 2.6 and was dropped.)
 */
@Mixin(UIColorPicker.class)
public abstract class UIColorPickerMixin implements IColorPickerSwatch
{
    @Shadow
    public Color color;

    @Shadow
    public boolean editAlpha;

    @Shadow
    public Area preview;

    @Shadow
    private void renderSwatch(Batcher2D batcher, int x1, int y1, int x2, int y2, Color color)
    {
    }

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
        method = "renderBackground",
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

    /** Outer preview corner radius, clamped to the preview box. */
    @Unique
    private float refreshedui$previewRadius()
    {
        return Math.max(0.5F, Math.min(UICornerRadii.interfaceChrome(), Math.min(this.preview.w, this.preview.h) * 0.5F - 0.5F));
    }

    /**
     * Preview, left half (initial colour) — drawn first, so when rounded it also lays down the rounded
     * dividerColor border box for the whole preview (and the checkerboard under both halves when editing alpha).
     */
    @Redirect(
        method = "renderPreview",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/input/color/UIColorPicker;renderSwatch(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;IIIILmchorse/bbs_mod/utils/colors/Color;)V", ordinal = 0)
    )
    private void refreshedui$roundPreviewInitial(UIColorPicker self, Batcher2D batcher, int x1, int y1, int x2, int y2, Color color)
    {
        if (UICornerRadii.interfaceChrome() <= 0)
        {
            this.renderSwatch(batcher, x1, y1, x2, y2, color);

            return;
        }

        float pr = this.refreshedui$previewRadius();
        float inner = Math.max(0.5F, pr - 1F);
        Area p = this.preview;

        ((IRoundedBatcher) batcher).roundedBox(p.x, p.y, p.w, p.h, pr, BBSSettings.dividerColor());

        if (this.editAlpha && p.w > 2 && p.h > 2)
        {
            ((IRoundedBatcher) batcher).roundedIconArea(Icons.CHECKBOARD, p.x + 1, p.y + 1, p.w - 2, p.h - 2, Math.min(inner, (p.h - 2) * 0.5F), Colors.WHITE);
        }

        this.refreshedui$renderHalfSwatch(batcher, x1 + 1, y1 + 1, x2 - x1 - 1, y2 - y1 - 2, inner, color, true);
    }

    /** Preview, right half (current colour): rounded on the right only, or flat fallback. */
    @Redirect(
        method = "renderPreview",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/input/color/UIColorPicker;renderSwatch(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;IIIILmchorse/bbs_mod/utils/colors/Color;)V", ordinal = 1)
    )
    private void refreshedui$roundPreviewCurrent(UIColorPicker self, Batcher2D batcher, int x1, int y1, int x2, int y2, Color color)
    {
        if (UICornerRadii.interfaceChrome() <= 0)
        {
            this.renderSwatch(batcher, x1, y1, x2, y2, color);

            return;
        }

        float inner = Math.max(0.5F, this.refreshedui$previewRadius() - 1F);

        this.refreshedui$renderHalfSwatch(batcher, x1, y1 + 1, x2 - x1 - 1, y2 - y1 - 2, inner, color, false);
    }

    /** One preview half: its colour (translucent over the checkerboard when editing alpha), rounded on one side. */
    @Unique
    private void refreshedui$renderHalfSwatch(Batcher2D batcher, float x, float y, float w, float h, float radius, Color color, boolean left)
    {
        if (w <= 0F || h <= 0F)
        {
            return;
        }

        float r = Math.min(radius, Math.min(w, h * 0.5F));
        int argb = this.editAlpha ? color.getARGBColor() : Colors.A100 | color.getRGBColor();

        ((IRoundedBatcher) batcher).roundedBoxSides(x, y, w, h, r, argb, left, !left);
    }

    /** Preview seam between the two halves: A25 → dividerColor; kept inside the border when rounded. */
    @Redirect(
        method = "renderPreview",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V")
    )
    private void refreshedui$previewSeam(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        if (UICornerRadii.interfaceChrome() > 0)
        {
            batcher.box(x1, y1 + 1, x2, y2 - 1, BBSSettings.dividerColor());
        }
        else
        {
            batcher.box(x1, y1, x2, y2, BBSSettings.dividerColor());
        }
    }

    /** Preview outline: drop when rounded (the rounded box already drew the border); else dividerColor. */
    @Redirect(
        method = "renderPreview",
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
}
