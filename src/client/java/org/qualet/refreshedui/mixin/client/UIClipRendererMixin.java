package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.film.clips.renderer.UIClipRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Drops the current-clip drop shadow (3.6): the refreshed theme uses outlines instead of shadows. */
@Mixin(UIClipRenderer.class)
public abstract class UIClipRendererMixin
{
    @Redirect(
        method = "renderClip",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;dropShadow(IIIIIII)V")
    )
    private void refreshedui$noCurrentShadow(Batcher2D batcher, int left, int top, int right, int bottom, int offset, int opaque, int shadow)
    {
    }
}
