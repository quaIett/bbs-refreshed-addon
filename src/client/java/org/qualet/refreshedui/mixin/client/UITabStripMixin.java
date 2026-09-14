package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.UITabStrip;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Rounds the active document-tab fill (3.2b). In BBS 2.6 {@code UIDataTabElement} only draws its content;
 * the fill behind the active tab moved to {@link UITabStrip#preRender} — a plain {@code Area.render} when the
 * strip was given an {@code activeColor} (the data tabs use {@code BBSSettings::baseSurface}). Strips that mark
 * the active tab with an edge instead go through {@code Batcher2D.highlight}, rounded by
 * {@link Batcher2DHighlightMixin}. The strip's own background ({@code render}) is left square.
 */
@Mixin(UITabStrip.class)
public abstract class UITabStripMixin
{
    @Redirect(
        method = "preRender",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundActiveFill(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }
}
