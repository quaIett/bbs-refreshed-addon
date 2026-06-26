package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import org.qualet.refreshedui.client.anim.SectionReveal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Arms the section animation by intercepting the two tree edits inside {@link UISection#setExpanded}.
 *
 * <p>Expanding calls {@code this.add(this.fields)} and collapsing calls {@code this.fields.removeFromParent()}.
 * We redirect both:</p>
 * <ul>
 *   <li><b>Expand</b> — arm the reveal, then add the body only if it is not already a child (a re-expand
 *       mid-collapse still has it attached, and {@code add} does not de-duplicate).</li>
 *   <li><b>Collapse</b> — arm the reveal and <em>keep</em> the body in the tree, so the fold can play out;
 *       it is detached later by {@link SectionReveal#finishCollapseIfDone}. If animations are off,
 *       {@link SectionReveal#onCollapse} returns false and we detach immediately, exactly like stock.</li>
 * </ul>
 *
 * <p>The arrow still flips and {@code resizeParent} still runs as usual, so the reserved space is held for
 * the whole collapse and the sections below only settle once it finishes.</p>
 */
@Mixin(UISection.class)
public abstract class UISectionRevealMixin
{
    @Redirect(
        method = "setExpanded",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/UISection;add(Lmchorse/bbs_mod/ui/framework/elements/IUIElement;)V")
    )
    private void refreshedui$expand(UISection section, IUIElement fields)
    {
        SectionReveal.onExpand((UIElement) fields);

        if (((UIElement) fields).getParent() != section)
        {
            section.add(fields);
        }
    }

    @Redirect(
        method = "setExpanded",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/UIElement;removeFromParent()V")
    )
    private void refreshedui$collapse(UIElement fields)
    {
        if (!SectionReveal.onCollapse(fields))
        {
            fields.removeFromParent();
        }
    }
}
