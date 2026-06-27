package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Context-menu entry hover highlight: a rounded primary selection frame (bright primary stroke + muted
 * darker fill) instead of the engine's flat box. Mirrors {@link ColorfulContextActionMixin} (active toggle
 * entries) so hover and active share one look.
 */
@Mixin(ContextAction.class)
public abstract class ContextActionMixin
{
    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V")
    )
    private void refreshedui$roundHighlight(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        RoundedAreas.renderSelectionFrame(batcher, x1, y1, x2 - x1, y2 - y1, color, UICornerRadii.buttonsAndTrackpads());
    }
}
