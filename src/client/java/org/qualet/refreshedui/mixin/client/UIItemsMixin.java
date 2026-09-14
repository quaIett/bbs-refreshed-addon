package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.input.items.UIItems;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Rounds the list / grid background (3.2a). BBS 2.6 (upstream aebdf23b1) pulled the shared item machinery
 * out of {@code UIList} into {@link UIItems}, whose {@code render} now paints the {@code background} fill —
 * so the redirect that used to sit in {@code UIListMixin} lives here and covers lists and item grids alike.
 */
@Mixin(UIItems.class)
public abstract class UIItemsMixin
{
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundBackground(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }
}
