package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Context-menu entry hover highlight: a rounded primary selection frame (bright primary stroke + muted
 * darker fill) instead of the engine's flat wash. Mirrors {@link ColorfulContextActionMixin} (active toggle
 * entries) so hover and active share one look.
 *
 * <p>BBS 2.6 (upstream 29cdbb4cd) draws the hover through {@code RowStyle.hover(batcher, x, y, w, h, 0)};
 * colour 0 means "the accent", so it is resolved to the primary colour here.</p>
 */
@Mixin(ContextAction.class)
public abstract class ContextActionMixin
{
    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/RowStyle;hover(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;IIIII)V")
    )
    private void refreshedui$roundHighlight(Batcher2D batcher, int x, int y, int w, int h, int color)
    {
        int base = color != 0 ? color : BBSSettings.primaryColor.get();

        RoundedAreas.renderSelectionFrame(batcher, x, y, w, h, Colors.A50 | base, UICornerRadii.buttonsAndTrackpads());
    }
}
