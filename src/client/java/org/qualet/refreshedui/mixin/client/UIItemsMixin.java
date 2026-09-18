package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.items.UIItems;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.anim.ListMotion;
import org.qualet.refreshedui.client.ui.IListMotionHost;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rounds the list / grid background (3.2a). BBS 2.6 (upstream aebdf23b1) pulled the shared item machinery
 * out of {@code UIList} into {@link UIItems}, whose {@code render} now paints the {@code background} fill —
 * so the redirect that used to sit in {@code UIListMixin} lives here and covers lists and item grids alike.
 *
 * <p>While a list opens a gap for rows dragged over it ({@link ListMotion}), the gap itself says where they
 * will land, so the insertion line goes: a flat list draws none, a tree keeps a short one in the middle of
 * the gap, indented to the depth the rows would land at — the one thing the gap cannot show.</p>
 */
@Mixin(UIItems.class)
public abstract class UIItemsMixin
{
    @Shadow
    protected abstract int insertionInset(int insertion);

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundBackground(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }

    @Inject(method = "renderInsertion", at = @At("HEAD"), cancellable = true)
    private void refreshedui$caretInGap(UIContext context, int insertion, CallbackInfo ci)
    {
        if (!((Object) this instanceof IListMotionHost host) || !host.refreshedui$motion().hasGap())
        {
            return;
        }

        ci.cancel();

        if (host.refreshedui$indentStep() <= 0)
        {
            return;
        }

        UIList<?> list = (UIList<?>) (Object) this;
        int s = list.rowHeight();
        int y = list.area.y - (int) list.scroll.getScroll() + Math.round(host.refreshedui$motion().gapMiddle(s));

        context.batcher.box(list.area.x + this.insertionInset(insertion), y - 1, list.area.ex(), y + 1, Colors.A100 | BBSSettings.primaryColor.get());
    }
}
