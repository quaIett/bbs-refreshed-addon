package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.UITimelineCanvas;
import org.qualet.refreshedui.RefreshedUiAddon;
import org.qualet.refreshedui.client.ui.IClipHover;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Timeline hover for "Grey clips" ({@link RefreshedUiAddon#greyClips}): BBS' white frame around the hovered clip is
 * dropped — {@code UIClipRendererMixin} lightens the hovered clip's fill instead, using {@link IClipHover} to apply
 * the same rule. Extends {@link UITimelineCanvas} only to reach its protected {@code marquee} (no inherited @Shadow).
 */
@Mixin(UIClips.class)
public abstract class UIClipsMixin extends UITimelineCanvas implements IClipHover
{
    @Shadow
    private boolean grabbing;

    @Override
    public boolean refreshedui$canHover()
    {
        return !this.grabbing && !this.marquee.isPressed();
    }

    @WrapOperation(
        method = "renderCameraWork",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;outline(FFFFI)V")
    )
    private void refreshedui$noHoverFrame(Batcher2D batcher, float x1, float y1, float x2, float y2, int color, Operation<Void> original)
    {
        if (RefreshedUiAddon.greyClips == null || !RefreshedUiAddon.greyClips.get())
        {
            original.call(batcher, x1, y1, x2, y2, color);
        }
    }
}
