package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIconStrip;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcons;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.anim.Animations;
import org.qualet.refreshedui.client.anim.SegmentSlide;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
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
 *
 * <p>Single-choice strips ({@link UIIcons}) slide their mark to the newly picked cell ({@link SegmentSlide}):
 * the mark is drawn once, right after the track and before any cell, at its sliding position — drawn from
 * inside the cell loop it would cover the icons of the cells it passes — and each icon turns to the
 * contrast colour as the mark covers it.</p>
 */
@Mixin(UIIconStrip.class)
public abstract class UIIconsMixin
{
    @Unique
    private static final int INACTIVE = Colors.setA(Colors.WHITE, 0.35F);

    @Shadow
    protected abstract boolean isActive(int index);

    @Shadow
    protected abstract int getCellWidth();

    @Shadow
    protected abstract int getContentX();

    @Shadow
    public abstract int getCount();

    @Unique
    private SegmentSlide refreshedui$slide;

    /** Cell index the sliding mark stands at this frame, or -1 when the marks are drawn per cell. */
    @Unique
    private float refreshedui$shown = -1F;

    /** Round the track background so the run's ends aren't square when nothing is highlighted. */
    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundedTrack(Area area, Batcher2D batcher, int color)
    {
        float radius = UICornerRadii.buttonsAndTrackpads();

        RoundedAreas.renderRounded(area, batcher, color, radius);

        this.refreshedui$shown = -1F;

        if ((Object) this instanceof UIIcons icons && Animations.enabled() && this.getCount() > 0)
        {
            if (this.refreshedui$slide == null)
            {
                this.refreshedui$slide = new SegmentSlide();
            }

            int cellW = this.getCellWidth();
            float shown = this.refreshedui$slide.update(icons.getValue());

            this.refreshedui$shown = shown;
            RoundedAreas.roundedBoxHorizontal(batcher, this.getContentX() + shown * cellW, area.y, cellW, area.h, radius,
                RoundedAreas.highlightFill(BBSSettings.primaryColor.get()), true, true);
        }
    }

    /**
     * Active-cell mark with neighbour merging. The cell index is recovered from the mark's area (cells are
     * laid out at {@code getContentX() + i * getCellWidth()}) rather than from a captured loop local.
     * Skipped for a sliding strip, whose mark is already down.
     */
    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;highlight(Lmchorse/bbs_mod/ui/utils/Area;Lmchorse/bbs_mod/utils/Direction;)V")
    )
    private void refreshedui$mergedHighlight(Batcher2D batcher, Area area, Direction edge)
    {
        if (this.refreshedui$shown >= 0F)
        {
            return;
        }

        int index = (area.x - this.getContentX()) / Math.max(1, this.getCellWidth());
        boolean mergeLeft = index > 0 && this.isActive(index - 1);
        boolean mergeRight = index + 1 < this.getCount() && this.isActive(index + 1);

        RoundedAreas.roundedBoxHorizontal(batcher, area.x, area.y, area.w, area.h, UICornerRadii.buttonsAndTrackpads(),
            RoundedAreas.highlightFill(BBSSettings.primaryColor.get()), !mergeLeft, !mergeRight);
    }

    /** Contrast-coloured icon on the mark; on a sliding strip, as much of it as the mark covers. */
    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;icon(Lmchorse/bbs_mod/ui/utils/icons/Icon;IFFFF)V")
    )
    private void refreshedui$iconColor(Batcher2D batcher, Icon icon, int color, float x, float y, float ax, float ay, @Local(ordinal = 0) boolean active)
    {
        int contrast = UIContrastColor.onPrimary();

        if (this.refreshedui$shown >= 0F)
        {
            int index = (int) ((x - this.getContentX()) / Math.max(1, this.getCellWidth()));
            float cover = SegmentSlide.coverage(this.refreshedui$shown, index);

            /* Stock paints the active cell white; before the mark reaches it, it is still an idle cell */
            color = SegmentSlide.mix(active ? INACTIVE : color, contrast, cover);
        }
        else if (active)
        {
            color = contrast;
        }

        batcher.icon(icon, color, x, y, ax, ay);
    }
}
