package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mchorse.bbs_mod.ui.film.clips.renderer.UIAudioClipRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.qualet.refreshedui.RefreshedUiAddon;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Soft rounding of the audio clip's waveform backing with "Grey clips" on, matching the plain clip fill rounded
 * in {@code UIClipRendererMixin}.
 */
@Mixin(UIAudioClipRenderer.class)
public abstract class UIAudioClipRendererMixin
{
    @WrapOperation(
        method = "renderBackground(Lmchorse/bbs_mod/ui/framework/UIContext;ILmchorse/bbs_mod/camera/clips/misc/AudioClip;Lmchorse/bbs_mod/ui/utils/Area;ZZ)V",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V")
    )
    private void refreshedui$roundedWaveformFill(Batcher2D batcher, float x1, float y1, float x2, float y2, int color, Operation<Void> original)
    {
        if (RefreshedUiAddon.greyClips != null && RefreshedUiAddon.greyClips.get())
        {
            ((IRoundedBatcher) batcher).roundedBox(x1, y1, x2 - x1, y2 - y1, UICornerRadii.clips(), color);
        }
        else
        {
            original.call(batcher, x1, y1, x2, y2, color);
        }
    }
}
