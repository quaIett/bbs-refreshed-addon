package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import org.qualet.refreshedui.client.ui.IHiddenScroll;
import org.spongepowered.asm.mixin.Mixin;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clip panel title row: the title textbox sits flush against the {@code enabled} toggle (default row
 * gap is {@link UIConstants#MARGIN} = 3px), so its rounded right edge visually overlaps the toggle.
 * Widen the gap by 7px. In a {@link mchorse.bbs_mod.ui.utils.resizers.layout.RowResizer} the trailing
 * fixed-width toggle stays flush-right regardless of margin, so this only trims the flexible title's
 * right side by 7px without moving the toggle.
 *
 * <p>The clip's properties scroll never shows a scrollbar nor BBS's edge shades (the wheel still scrolls it).</p>
 */
@Mixin(UIClip.class)
public abstract class UIClipMixin
{
    @Shadow
    public UIScrollView panels;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$hideScrollbar(CallbackInfo ci)
    {
        ((IHiddenScroll) this.panels.scroll).refreshedui$hide();
    }

    @Redirect(method = "registerPanels", at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/UI;row([Lmchorse/bbs_mod/ui/framework/elements/UIElement;)Lmchorse/bbs_mod/ui/framework/elements/UIElement;"))
    private UIElement refreshedui$titleRowGap(UIElement[] elements)
    {
        return UI.row(UIConstants.MARGIN + 7, elements);
    }
}
