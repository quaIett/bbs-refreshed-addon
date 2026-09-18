package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.anim.HoverFade;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Universal flat + rounded row and cell marks. BBS 2.6 {@link RowStyle} is the single place rows and grid cells
 * paint their state — landing-screen buttons, settings categories, texture browser tree / layers / grid, form
 * grid, frame strip, timeline tracks — so replacing its primitives restyles all of them at once. Gradient washes
 * become flat rounded fills and the hard edge bars become slim rounded pills:
 * <ul>
 *   <li>pick: accent at 50% (75% under the cursor); hover and header: the row's colour (or accent) at 25%;</li>
 *   <li>a row's own colour keeps a slim pill down its left edge, so coloured rows (tracks, categories) keep meaning;</li>
 *   <li>drop target: accent at 35%; grid cells: the same fills, and the chosen cell's bottom bar a slim pill.</li>
 * </ul>
 * A {@code row}'s hover fades in and out ({@link HoverFade}); {@code hover} is only called while hovered, so
 * the washes painted through it stay instant.
 * Call sites that redirect {@code RowStyle} themselves ({@code UIListMixin} — merged multi-selection,
 * {@code ContextActionMixin}) keep their own styling.
 */
@Mixin(RowStyle.class)
public abstract class RowStyleMixin
{
    @Unique
    private static int refreshedui$accent()
    {
        return BBSSettings.primaryColor.get() & Colors.RGB;
    }

    @Unique
    private static void refreshedui$fill(Batcher2D batcher, int x, int y, int w, int h, int color)
    {
        RoundedAreas.roundedBox(batcher, x, y, w, h, UICornerRadii.buttonsAndTrackpads(), color);
    }

    @Inject(method = "row", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$flatRow(Batcher2D batcher, int x, int y, int w, int h, int color, boolean header, boolean hover, boolean picked, CallbackInfo ci)
    {
        int accent = refreshedui$accent();
        int tint = color != 0 ? color & Colors.RGB : accent;

        if (header)
        {
            refreshedui$fill(batcher, x, y, w, h, Colors.A25 | tint);
        }

        /* Every row passes through here on every frame, hovered or not, so the hover can fade both ways */
        float level = HoverFade.levelRect(x, y, w, h, hover);

        if (picked)
        {
            refreshedui$fill(batcher, x, y, w, h, Colors.setA(accent, 0.5F + 0.25F * level));
        }
        else if (level > 0F)
        {
            refreshedui$fill(batcher, x, y, w, h, Colors.setA(tint, 0.25F * level));
        }

        if (color != 0)
        {
            RoundedAreas.roundedBox(batcher, x + 1, y + 2, RowStyle.STRIPE, h - 4, RowStyle.STRIPE / 2F, Colors.A100 | color);
        }

        ci.cancel();
    }

    @Inject(method = "hover", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$flatHover(Batcher2D batcher, int x, int y, int w, int h, int color, CallbackInfo ci)
    {
        int tint = color != 0 ? color & Colors.RGB : refreshedui$accent();

        refreshedui$fill(batcher, x, y, w, h, Colors.A25 | tint);
        ci.cancel();
    }

    @Inject(method = "swatch", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$pillSwatch(Batcher2D batcher, int x, int y, int h, int color, CallbackInfo ci)
    {
        RoundedAreas.roundedBox(batcher, x + 1, y + 2, RowStyle.STRIPE, h - 4, RowStyle.STRIPE / 2F, Colors.A100 | color);
        ci.cancel();
    }

    @Inject(method = "dropTarget", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$flatDropTarget(Batcher2D batcher, int x, int y, int w, int h, CallbackInfo ci)
    {
        refreshedui$fill(batcher, x, y, w, h, Colors.setA(refreshedui$accent(), 0.35F));
        ci.cancel();
    }

    @Inject(method = "cellWash", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$flatCell(Batcher2D batcher, int x, int y, int w, int h, boolean hover, boolean lit, CallbackInfo ci)
    {
        int accent = refreshedui$accent();

        if (lit)
        {
            refreshedui$fill(batcher, x, y, w, h, (hover ? Colors.A75 : Colors.A50) | accent);
        }
        else if (hover)
        {
            refreshedui$fill(batcher, x, y, w, h, Colors.A25 | accent);
        }

        ci.cancel();
    }

    @Inject(method = "cellBar", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$pillCellBar(Batcher2D batcher, int x, int y, int w, int h, CallbackInfo ci)
    {
        RoundedAreas.roundedBox(batcher, x + 2, y + h - RowStyle.STRIPE - 1, w - 4, RowStyle.STRIPE, RowStyle.STRIPE / 2F, Colors.A100 | refreshedui$accent());
        ci.cancel();
    }
}
