package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcons;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@link UIIcons} — the icon-row mode selector (BBS' replacement for {@code UICirculate}, used all over
 * the particle scheme sections: material / facing / direction source / lighting mode / expiration mode,
 * etc.). Its {@code renderSkin} draws the selected cell the engine's old way — a
 * {@code UIDashboardPanels.renderHighlight} bevel + a plain white icon. Mirror the dock-stack-tab
 * treatment ({@link UIDockLayoutTabsMixin}) onto it:
 * <ul>
 *   <li>the whole-element track background gets rounded ends (matches our other rounded controls);</li>
 *   <li>the selected cell's icon is tinted with the adaptive contrast color so it reads on the fill.</li>
 * </ul>
 *
 * <p>The selected cell's rounded fill is no longer redirected here — it comes from the global
 * {@code UIDashboardPanels.renderHighlight} inject (see {@link UIDashboardPanelsMixin}).</p>
 */
@Mixin(UIIcons.class)
public abstract class UIIconsMixin
{
    /** Round the track background so the element's ends aren't square when nothing is highlighted. */
    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundedTrack(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.buttonsAndTrackpads());
    }

    @ModifyArg(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;icon(Lmchorse/bbs_mod/ui/utils/icons/Icon;IFFFF)V"),
        index = 1
    )
    private int refreshedui$blackenActiveIcon(int color, @Local(ordinal = 0) boolean active)
    {
        return active ? UIContrastColor.onPrimary() : color;
    }
}
