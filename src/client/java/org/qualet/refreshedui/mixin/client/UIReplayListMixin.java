package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.film.replays.UIReplayList;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import org.qualet.refreshedui.client.ui.OverlaySizes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replay list random-textures folder picker uses its fixed default overlay size (3.7). */
@Mixin(UIReplayList.class)
public abstract class UIReplayListMixin
{
    @Redirect(
        method = "*",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlay;addOverlay(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlayPanel;IF)Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlay;")
    )
    private UIOverlay refreshedui$defaultSize(UIContext context, UIOverlayPanel panel, int w, float h)
    {
        return OverlaySizes.sizeFor(panel) != null
            ? UIOverlay.addOverlay(context, panel)
            : UIOverlay.addOverlay(context, panel, w, h);
    }
}
