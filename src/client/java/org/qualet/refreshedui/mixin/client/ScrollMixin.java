package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Scroll;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.IHiddenScroll;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scrollbar handle restyle (3.2a): the static {@code bar(...)} draws a flat rounded handle instead of
 * the old beveled 3-box handle, and {@code renderScrollbar} feeds it a drag-aware flat color.
 *
 * <p>{@code bar} is a full-method replacement via {@code @Inject(HEAD, cancellable)} (see OVERWRITES.md).
 * When rounding is off it falls back to the old {@code surfaceBox} look.</p>
 *
 * <p>{@link IHiddenScroll}: a scroll marked hidden turns its scrollbar off and skips
 * {@code renderScrollbar} entirely, so the primary-colored edge shades BBS draws for a bar-less scroll
 * do not appear either.</p>
 */
@Mixin(Scroll.class)
public abstract class ScrollMixin implements IHiddenScroll
{
    @Shadow
    public boolean dragging;

    @Shadow
    public boolean scrollbar;

    @Unique
    private boolean refreshedui$hidden;

    @Override
    public void refreshedui$hide()
    {
        this.refreshedui$hidden = true;
        this.scrollbar = false;
    }

    @Inject(method = "renderScrollbar", at = @At("HEAD"), cancellable = true)
    private void refreshedui$skipHidden(Batcher2D batcher, CallbackInfo ci)
    {
        if (this.refreshedui$hidden)
        {
            ci.cancel();
        }
    }

    /** Flat handle colors from master-refreshed (HANDLE_COLOR / HANDLE_ACTIVE_COLOR). */
    @Inject(
        method = "bar(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;IIIII)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void refreshedui$roundBar(Batcher2D batcher, int x1, int y1, int x2, int y2, int fill, CallbackInfo ci)
    {
        if (x2 - x1 != 0 && y2 - y1 != 0)
        {
            int radius = UICornerRadii.interfaceChrome();

            if (radius > 0)
            {
                float w = x2 - x1;
                float h = y2 - y1;
                float rad = Math.min((float) radius, Math.min(w * 0.5F, h * 0.5F) - 0.5F);

                if (rad < 0.5F)
                {
                    rad = 0.5F;
                }

                ((IRoundedBatcher) batcher).roundedBox(x1, y1, w, h, rad, fill);
            }
            else
            {
                batcher.surfaceBox(x1, y1, x2, y2, fill, true, false);
            }
        }

        ci.cancel();
    }

    @ModifyArg(
        method = "renderScrollbar",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Scroll;bar(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;IIIII)V"),
        index = 5
    )
    private int refreshedui$scrollbarColor(int original)
    {
        return this.dragging ? 0xff6e747c : 0xff4d525a;
    }
}
