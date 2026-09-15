package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.context.UISimpleContextMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inner padding for context menus: stock lays the bar, the filter and the action rows flush against the
 * menu's edge, so the rounded frame ({@link UIContextMenuMixin}) hugs the rows and the search box with no
 * breathing room (the design mockup has a clear gutter around them). Grow the menu by {@link #PAD} on every
 * side and inset all three children by the same amount.
 *
 * <p>The size comes from {@code setMouse}'s {@code set(x, y, w, 0).h(...)} chain: width and height are
 * widened through {@code @ModifyArg} on those two calls — note {@code h(I)} is invoked on {@code set}'s
 * {@code UIElement} return value, so its owner is {@code UIElement}, and it is the only {@code h(I)} call in
 * the method — and the children are re-offset at TAIL after stock has positioned them.</p>
 */
@Mixin(UISimpleContextMenu.class)
public abstract class UISimpleContextMenuMixin
{
    /** Gutter between the menu frame and its content, px. */
    private static final int PAD = 4;

    @ModifyArg(
        method = "setMouse",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/context/UISimpleContextMenu;set(IIII)Lmchorse/bbs_mod/ui/framework/elements/UIElement;"),
        index = 2
    )
    private int refreshedui$padWidth(int w)
    {
        return w + PAD * 2;
    }

    @ModifyArg(
        method = "setMouse",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/UIElement;h(I)Lmchorse/bbs_mod/ui/framework/elements/UIElement;")
    )
    private int refreshedui$padHeight(int h)
    {
        return h + PAD * 2;
    }

    @Inject(method = "setMouse", at = @At("TAIL"))
    private void refreshedui$insetChildren(UIContext context, CallbackInfo ci)
    {
        UISimpleContextMenu self = (UISimpleContextMenu) (Object) this;
        int top = self.bar.isVisible() ? self.bar.getTotalHeight() : 0;

        self.bar.x(PAD).y(PAD).w(1F, -PAD * 2);
        self.filter.x(PAD).y(top + PAD).w(1F, -PAD * 2);

        if (self.filter.isVisible())
        {
            top += UISimpleContextMenu.FILTER_HEIGHT;
        }

        self.actions.x(PAD).y(top + PAD).w(1F, -PAD * 2).h(1F, -(top + PAD * 2));
    }
}
