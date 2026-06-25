package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.film.clips.UICurveClip;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import org.qualet.refreshedui.client.ui.OverlaySizes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Curve clip panels picker uses its fixed default overlay size (3.7). */
@Mixin(UICurveClip.class)
public abstract class UICurveClipMixin
{
    @Redirect(
        method = "*",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlay;addOverlay(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlayPanel;FF)Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlay;")
    )
    private static UIOverlay refreshedui$defaultSize(UIContext context, UIOverlayPanel panel, float w, float h)
    {
        return OverlaySizes.sizeFor(panel) != null
            ? UIOverlay.addOverlay(context, panel)
            : UIOverlay.addOverlay(context, panel, w, h);
    }
}
