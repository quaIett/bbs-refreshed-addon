package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.film.utils.shader.UIShaderOptionCell;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Shader-option cells (the Iris options grid of the curve clip, curve fixer merged into BBS FS 2.7):
 * restores the rounded skin the standalone curvefixer addon drew when refreshedui was present.
 * <ul>
 *   <li>cell background {@code surfaceBox} -> rounded fill;</li>
 *   <li>the 2px "already animated" {@code outline} -> a ring drawn as a full rounded box with the fill
 *       inset on top (a thin rounded frame gets eaten by the corner anti-aliasing), so the fill call is
 *       deferred until the outline colour is known;</li>
 *   <li>value readout {@code box} -> rounded box one px tighter in radius.</li>
 * </ul>
 * The boolean cells' embedded {@code UIToggle} is a stock toggle and is already themed by {@link UIToggleMixin}.
 */
@Mixin(UIShaderOptionCell.class)
public abstract class UIShaderOptionCellMixin
{
    @Unique
    private static final float refreshedui$RING = 2F;

    @Shadow
    private boolean added;

    /** Fill of an added cell, held from the background call until the ring is drawn. */
    @Unique
    private int refreshedui$fill;

    @Unique
    private static float refreshedui$radius()
    {
        return Math.max(0.5F, UICornerRadii.buttonsAndTrackpads());
    }

    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;surfaceBox(IIIIIZZ)V")
    )
    private void refreshedui$roundBackground(Batcher2D batcher, int x1, int y1, int x2, int y2, int fill, boolean shadow, boolean border)
    {
        if (this.added)
        {
            /* Painted together with the ring in refreshedui$roundRing. */
            this.refreshedui$fill = fill;

            return;
        }

        ((IRoundedBatcher) batcher).roundedBox(x1, y1, x2 - x1, y2 - y1, refreshedui$radius(), fill);
    }

    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;outline(FFFFII)V")
    )
    private void refreshedui$roundRing(Batcher2D batcher, float x1, float y1, float x2, float y2, int color, int thickness)
    {
        IRoundedBatcher rounded = (IRoundedBatcher) batcher;
        float radius = refreshedui$radius();
        float inset = refreshedui$RING;

        rounded.roundedBox(x1, y1, x2 - x1, y2 - y1, radius, color);
        rounded.roundedBox(x1 + inset, y1 + inset, x2 - x1 - inset * 2F, y2 - y1 - inset * 2F, Math.max(0.5F, radius - inset), this.refreshedui$fill);
    }

    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V")
    )
    private void refreshedui$roundValueBox(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        ((IRoundedBatcher) batcher).roundedBox(x1, y1, x2 - x1, y2 - y1, Math.max(0.5F, refreshedui$radius() - 1F), color);
    }
}
