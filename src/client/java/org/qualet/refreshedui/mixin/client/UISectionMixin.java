package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.qualet.refreshedui.client.anim.SectionReveal;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rounds the background block of BBS's new collapsible {@link UISection} (Sections pt.1/pt.2), grows/shrinks
 * that block in step with the row cascade, and finalizes a deferred collapse once it finishes.
 *
 * <p>Stock {@code render} fills the section block with a square {@code box(x1,y1,x2,y2,color)} on the raised
 * surface. We redirect that single call to a {@code roundedBox} of the same rect/color, matching the rest of
 * the refreshed interface chrome. While the body is animating, the block's bottom edge follows the lowest
 * visible row ({@link SectionReveal#bgBottom}), so the card unrolls (or rolls up) together with its rows
 * instead of popping — but only visually: the reserved space is unchanged, so the sections below do not move
 * until a collapse completes. The {@code HEAD} inject detaches the body for real at the end of a collapse,
 * before the children loop runs, so the structural change cannot corrupt the iteration. The header (arrow +
 * title) draws on top, untouched.</p>
 */
@Mixin(UISection.class)
public abstract class UISectionMixin
{
    @Shadow
    public UIElement fields;

    @Inject(method = "render", at = @At("HEAD"))
    private void refreshedui$finishCollapse(UIContext context, CallbackInfo ci)
    {
        SectionReveal.finishCollapseIfDone(this.fields);
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V")
    )
    private void refreshedui$roundedBlock(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        float bottom = SectionReveal.bgBottom(this.fields, y1, y2);

        float w = x2 - x1;
        float h = bottom - y1;

        ((IRoundedBatcher) batcher).roundedBox(x1, y1, w, h,
            UICornerRadii.interfaceChromeClamped((int) w, (int) h), color);
    }
}
