package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Direction;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3.8 — the engine-wide selection highlight as a rounded fill instead of the edge bar + gradient.
 *
 * <p>BBS 2.6 moved the old static {@code UIDashboardPanels.renderHighlight} into
 * {@link Batcher2D#highlight(Area, Direction, int)} (upstream 642783938): {@code UIIcon.highlight(when, edge)},
 * {@code UITabStrip} active-edge tabs, {@code UIIconStrip}, the form editor / bone picker area marks and the
 * context-menu verb strip all end up here (the 2-arg overload delegates to this one with the primary colour),
 * so one HEAD inject still covers every call site. Edge direction is intentionally ignored: our mark is a
 * uniform rounded pill, not an edge bar. Fill colour rules live in {@link RoundedAreas#highlightFill}.</p>
 *
 * <p>Kept separate from {@code Batcher2DMixin} (the primitives) since this is a consumer restyle.</p>
 */
@Mixin(Batcher2D.class)
public abstract class Batcher2DHighlightMixin
{
    @Inject(
        method = "highlight(Lmchorse/bbs_mod/ui/utils/Area;Lmchorse/bbs_mod/utils/Direction;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void refreshedui$roundHighlight(Area area, Direction edge, int color, CallbackInfo ci)
    {
        RoundedAreas.renderRounded(area, (Batcher2D) (Object) this, RoundedAreas.highlightFill(color), UICornerRadii.buttonsAndTrackpads());
        ci.cancel();
    }
}
