package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Global flat + rounded row marks (BBS 2.6 {@link RowStyle} is the single place rows paint their hover and
 * colour tags: landing-screen buttons, menus, trees, timeline tracks). The gradient hover wash becomes a flat
 * rounded fill and the colour tag becomes a slim rounded pill — the same look {@code UIListMixin} gives list
 * rows. Call sites that redirect {@code RowStyle} themselves ({@code UIListMixin}, {@code ContextActionMixin})
 * keep their own styling.
 */
@Mixin(RowStyle.class)
public abstract class RowStyleMixin
{
    @Inject(method = "hover", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$flatHover(Batcher2D batcher, int x, int y, int w, int h, int color, CallbackInfo ci)
    {
        int tint = color != 0 ? color & Colors.RGB : BBSSettings.primaryColor.get() & Colors.RGB;

        RoundedAreas.roundedBox(batcher, x, y, w, h, UICornerRadii.buttonsAndTrackpads(), Colors.A25 | tint);
        ci.cancel();
    }

    @Inject(method = "swatch", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$pillSwatch(Batcher2D batcher, int x, int y, int h, int color, CallbackInfo ci)
    {
        RoundedAreas.roundedBox(batcher, x + 1, y + 2, RowStyle.STRIPE, h - 4, RowStyle.STRIPE / 2F, Colors.A100 | color);
        ci.cancel();
    }
}
