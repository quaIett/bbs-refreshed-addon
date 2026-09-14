package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIconStrip;
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
 * Icon strips — {@code UIIcons} (the icon-row mode selector, BBS' replacement for {@code UICirculate},
 * used all over the particle scheme sections) and {@code UIIconToggles}. BBS 2.6 moved the painting of
 * both into their shared base {@link UIIconStrip#renderSkin}, so this mixin targets the base. Its
 * {@code renderSkin} draws the selected cell the engine's way — a {@code Batcher2D.highlight} mark + a
 * plain white icon. Mirror the dock-stack-tab treatment ({@link UIDockLayoutTabsMixin}) onto it:
 * <ul>
 *   <li>the track background (in 2.6 only the run of fixed-width cells, not the whole element) gets
 *       rounded ends (matches our other rounded controls);</li>
 *   <li>an active cell's icon is tinted with the adaptive contrast color so it reads on the fill.</li>
 * </ul>
 *
 * <p>The active cell's fill is not redirected here — it comes from the global highlight restyle.</p>
 */
@Mixin(UIIconStrip.class)
public abstract class UIIconsMixin
{
    /** Round the track background so the run's ends aren't square when nothing is highlighted. */
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
