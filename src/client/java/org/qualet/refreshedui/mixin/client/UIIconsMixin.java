package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIconStrip;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Direction;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Icon strips — {@code UIIcons} (the icon-row mode selector, BBS' replacement for {@code UICirculate},
 * used all over the particle scheme sections) and {@code UIIconToggles} (e.g. the gizmo element toggles in
 * the settings). BBS 2.6 moved the painting of both into their shared base {@link UIIconStrip#renderSkin},
 * so this mixin targets the base. Its {@code renderSkin} draws the selected cell the engine's way — a
 * {@code Batcher2D.highlight} mark + a plain white icon. Mirror the dock-stack-tab treatment
 * ({@link UIDockLayoutTabsMixin}) onto it:
 * <ul>
 *   <li>the track background (in 2.6 only the run of fixed-width cells, not the whole element) gets
 *       rounded ends (matches our other rounded controls);</li>
 *   <li>active cells get the global highlight fill, but a run of ADJACENT active cells merges into one
 *       rounded block — only the run's outer corners round, like grouped context-menu rows
 *       ({@link UIActionListMixin}), just horizontal;</li>
 *   <li>an active cell's icon is tinted with the adaptive contrast color so it reads on the fill.</li>
 * </ul>
 */
@Mixin(UIIconStrip.class)
public abstract class UIIconsMixin
{
    @Shadow
    protected abstract boolean isActive(int index);

    @Shadow
    protected abstract int getCellWidth();

    @Shadow
    protected abstract int getContentX();

    @Shadow
    public abstract int getCount();

    /** Round the track background so the run's ends aren't square when nothing is highlighted. */
    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundedTrack(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.buttonsAndTrackpads());
    }

    /**
     * Active-cell mark with neighbour merging. The cell index is recovered from the mark's area (cells are
     * laid out at {@code getContentX() + i * getCellWidth()}) rather than from a captured loop local.
     */
    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;highlight(Lmchorse/bbs_mod/ui/utils/Area;Lmchorse/bbs_mod/utils/Direction;)V")
    )
    private void refreshedui$mergedHighlight(Batcher2D batcher, Area area, Direction edge)
    {
        int index = (area.x - this.getContentX()) / Math.max(1, this.getCellWidth());
        boolean mergeLeft = index > 0 && this.isActive(index - 1);
        boolean mergeRight = index + 1 < this.getCount() && this.isActive(index + 1);

        RoundedAreas.roundedBoxHorizontal(batcher, area.x, area.y, area.w, area.h, UICornerRadii.buttonsAndTrackpads(),
            RoundedAreas.highlightFill(BBSSettings.primaryColor.get()), !mergeLeft, !mergeRight);
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
