package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Rounds the background block of BBS's new collapsible {@link UISection} (Sections pt.1/pt.2).
 *
 * <p>Stock {@code render} fills the section block with a square {@code box(x1,y1,x2,y2,color)} on the
 * raised surface. We redirect that single call to a {@code roundedBox} of the same rect/color, matching
 * the rest of the refreshed interface chrome. The header (arrow + title) draws on top, untouched.</p>
 */
@Mixin(UISection.class)
public abstract class UISectionMixin
{
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V")
    )
    private void refreshedui$roundedBlock(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        float w = x2 - x1;
        float h = y2 - y1;

        ((IRoundedBatcher) batcher).roundedBox(x1, y1, w, h,
            UICornerRadii.interfaceChromeClamped((int) w, (int) h), color);
    }
}
