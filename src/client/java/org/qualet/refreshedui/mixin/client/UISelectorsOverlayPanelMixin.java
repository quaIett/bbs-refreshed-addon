package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.selectors.UISelectorsOverlayPanel;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Rounds the selectors overlay content surface (3.2b). Outer panel rounded by inherited
 * {@code UIOverlayPanel.renderBackground} (via {@code super}); inner content rounded here ({@code ordinal = 0}).
 */
@Mixin(UISelectorsOverlayPanel.class)
public abstract class UISelectorsOverlayPanelMixin
{
    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V", ordinal = 0)
    )
    private void refreshedui$roundContent(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }
}
