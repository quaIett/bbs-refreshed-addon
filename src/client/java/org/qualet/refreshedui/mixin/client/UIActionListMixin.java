package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.context.UIActionList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.utils.context.ColorfulContextAction;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import org.qualet.refreshedui.client.ui.SelectionMerge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Context-menu entry merging: an active toggle entry ({@link ColorfulContextAction}) draws a persistent
 * selection frame, so several adjacent ones stack into separate rounded pills with seams between them (e.g.
 * the clip-type "add" menu — every camera/action clip type is a colorful entry). Just before each entry
 * renders, stamp {@link SelectionMerge} with whether the neighbour above/below is also a colorful entry, so
 * {@link ColorfulContextActionMixin} squares the shared edges and the whole run reads as one rounded block.
 * Each entry keeps its OWN colour, so a multi-colour run becomes a single segmented block (no internal
 * border lines) rather than a stack of separate pills.
 *
 * <p>Plain (hover-only) entries can't stack — only one is hovered at a time — so they clear the flags and
 * stay single ({@link ContextActionMixin} ignores them). Inherited members are reached via a cast to
 * {@link UIList}, since {@code @Shadow} of a superclass member does not resolve in this setup.</p>
 */
@Mixin(UIActionList.class)
public abstract class UIActionListMixin
{
    @Inject(method = "renderListElement", at = @At("HEAD"))
    private void refreshedui$markSelectionGroup(UIContext context, ContextAction element, int i, int x, int y, boolean hover, boolean selected, CallbackInfo ci)
    {
        if (!(element instanceof ColorfulContextAction))
        {
            SelectionMerge.clear();

            return;
        }

        List<ContextAction> list = ((UIList<ContextAction>) (Object) this).getList();

        SelectionMerge.set(
            refreshedui$isColorful(list, i - 1),
            refreshedui$isColorful(list, i + 1)
        );
    }

    private static boolean refreshedui$isColorful(List<ContextAction> list, int index)
    {
        return index >= 0 && index < list.size() && list.get(index) instanceof ColorfulContextAction;
    }
}
