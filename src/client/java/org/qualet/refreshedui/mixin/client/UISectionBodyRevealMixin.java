package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.joml.Matrix3x2fStack;
import org.qualet.refreshedui.client.anim.GuiAlpha;
import org.qualet.refreshedui.client.anim.SectionReveal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Plays the section unfold/fold animation row by row by wrapping each child's render in the body's loop.
 *
 * <p>{@code UIElement.render} iterates {@code this.children} and calls {@code element.render(context)} on
 * each. We redirect that single call: when the element doing the iterating is a {@code UISection} body that
 * is animating ({@link SectionReveal#reveal}), the child is one of the section's rows, so its render is
 * wrapped by the row's current visibility — clipped to a window growing (expand) or shrinking (collapse)
 * from its top, slid a few pixels and faded through {@link GuiAlpha}, with a per-row stagger. Wrapping
 * the call (rather than hooking each element's own render) is what makes this cover every row: trackpads,
 * toggles and buttons override {@code render} without calling {@code super}, so a per-element inject would
 * miss their drawing.</p>
 *
 * <p>This redirect fires for every child render in the whole UI, but is inert (a map-empty check inside
 * {@code reveal}) whenever no section is animating, and a fully-visible row renders untouched.</p>
 */
@Mixin(UIElement.class)
public abstract class UISectionBodyRevealMixin
{
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/framework/elements/IUIElement;render(Lmchorse/bbs_mod/ui/framework/UIContext;)V"
        )
    )
    private void refreshedui$revealRow(IUIElement child, UIContext context)
    {
        UIElement parent = (UIElement) (Object) this;
        SectionReveal.Reveal reveal = SectionReveal.reveal(parent);

        if (reveal == null || !(child instanceof UIElement row))
        {
            /* Not an animating section body: this may instead be a container whose section children are
             * unfolding/folding — slide this child (and everything below it) to track their visible edge. */
            float offset = SectionReveal.precedingMissing(parent, child);

            if (offset > 0F)
            {
                Matrix3x2fStack matrices = context.batcher.getContext().getMatrices();

                matrices.pushMatrix();
                matrices.translate(0F, -offset);
                child.render(context);
                matrices.popMatrix();
            }
            else
            {
                child.render(context);
            }

            return;
        }

        UIElement body = parent;
        int index = body.getChildren().indexOf(child);
        float vis = index < 0 ? 1F : reveal.visibility(index, body.getChildren().size());

        if (vis >= 1F)
        {
            /* Row is fully shown — render it normally (e.g. an expanded row while later rows still cascade,
             * or a collapsing row whose turn to recede has not come yet). */
            child.render(context);

            return;
        }

        Area area = row.area;
        int h = (int) Math.ceil(area.h * vis);

        Batcher2D batcher = context.batcher;
        Matrix3x2fStack matrices = batcher.getContext().getMatrices();

        matrices.pushMatrix();
        matrices.translate(0F, -(1F - vis) * SectionReveal.SLIDE_PX);
        batcher.clip(area.x, area.y, area.w, h, context);

        /* Fade the whole row: every element it records while the multiplier is pushed has its alpha scaled */
        GuiAlpha.push(vis);

        try
        {
            child.render(context);
        }
        finally
        {
            GuiAlpha.pop();
            batcher.unclip(context);
            matrices.popMatrix();
        }
    }
}
