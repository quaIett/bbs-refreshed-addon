package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Rounds the trackpad input surface (3.2a) and the +/- arrow side buttons, and swaps the stock
 *  directional arrows ({@code MOVE_LEFT}/{@code MOVE_RIGHT}) on those buttons for our atlas' real
 *  plus/minus glyphs ({@code ADD}/{@code REMOVE}). */
@Mixin(UITrackpad.class)
public abstract class UITrackpadMixin
{
    @Shadow private Area plusOne;
    @Shadow private Area minusOne;

    /** Main input surface — full rounded box with the hairline field border (design overhaul, 3). */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundSurface(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderField(area, batcher, color, UICornerRadii.buttonsAndTrackpads());
    }

    /**
     * Side arrow buttons ({@code plusOne}/{@code minusOne}). They sit flush with the surface's left
     * and right edges, so their outer corners must be rounded with the same radius to follow the
     * surface outline — otherwise the faint hover fill pokes square nubs past the rounded corners.
     * The inner edge (toward the centre) stays square so it joins the body seamlessly.
     */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;II)V")
    )
    private void refreshedui$roundArrow(Area area, Batcher2D batcher, int color, int offset)
    {
        float x = area.x + offset;
        float y = area.y + offset;
        float w = area.w - offset * 2;
        float h = area.h - offset * 2;

        boolean roundLeft = area == this.minusOne;
        boolean roundRight = area == this.plusOne;

        ((IRoundedBatcher) batcher).roundedBoxSides(x, y, w, h, UICornerRadii.buttonsAndTrackpads(), color, roundLeft, roundRight);
    }

    /**
     * Left side button (decrement) — stock {@code MOVE_LEFT} arrow → our {@code REMOVE} (minus) glyph,
     * drawn centred in the {@code minusOne} button so the larger 16×16 atlas icon stays put (the stock
     * arrow was a 6×16 strip positioned by its own width). Keeps the original hover-fade {@code color}.
     */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;icon(Lmchorse/bbs_mod/ui/utils/icons/Icon;IFF)V", ordinal = 0)
    )
    private void refreshedui$minusIcon(Batcher2D batcher, Icon icon, int color, float x, float y)
    {
        this.refreshedui$drawHalfIcon(batcher, Icons.REMOVE, color, this.minusOne);
    }

    /** Right side button (increment) — stock {@code MOVE_RIGHT} arrow → our {@code ADD} (plus) glyph,
     *  centred in {@code plusOne}. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;icon(Lmchorse/bbs_mod/ui/utils/icons/Icon;IFF)V", ordinal = 1)
    )
    private void refreshedui$plusIcon(Batcher2D batcher, Icon icon, int color, float x, float y)
    {
        this.refreshedui$drawHalfIcon(batcher, Icons.ADD, color, this.plusOne);
    }

    /**
     * Draw {@code ic} at HALF its native size, centred in {@code button}. Uses {@code texturedBox}
     * (not {@code icon}/{@code iconArea}): it maps the icon's UV region onto an arbitrary quad, i.e.
     * it SCALES — {@code icon} is native-size only and {@code iconArea} tiles (which would clip the
     * glyph, not shrink it). Replicates {@code icon}'s light-theme {@code darkenWhite} (texturedBox
     * skips it) so a white glyph doesn't vanish on a light input surface.
     */
    @Unique
    private void refreshedui$drawHalfIcon(Batcher2D batcher, Icon ic, int color, Area button)
    {
        if (BBSSettings.isLightTheme() && (color & 0xFFFFFF) == 0xFFFFFF)
        {
            color = color & 0xFF000000;
        }

        float w = ic.w / 2F;
        float h = ic.h / 2F;
        float cx = button.x + button.w / 2F;
        float cy = button.y + button.h / 2F;

        batcher.texturedBox(
            BBSModClient.getTextures().getTexture(ic.texture), color,
            cx - w / 2F, cy - h / 2F, w, h,
            ic.x, ic.y, ic.x + ic.w, ic.y + ic.h, ic.textureW, ic.textureH
        );
    }
}
