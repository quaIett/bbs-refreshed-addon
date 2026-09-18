package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mchorse.bbs_mod.ui.framework.elements.utils.UITimelineCanvas;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.anim.CursorGlide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The time cursor every timeline draws through {@link UITimelineCanvas#renderCursor} (film clips, replay
 * keyframes, animation-state keyframes, audio) glides to a clicked spot instead of jumping; see
 * {@link CursorGlide}. Only the drawn column is eased — the tick itself, and everything that follows it,
 * changes at once as before.
 */
@Mixin(UITimelineCanvas.class)
public abstract class UITimelineCanvasMixin
{
    @ModifyVariable(method = "renderCursor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int refreshedui$glide(int x, @Local(argsOnly = true) Area area)
    {
        return CursorGlide.display(area, x);
    }
}
