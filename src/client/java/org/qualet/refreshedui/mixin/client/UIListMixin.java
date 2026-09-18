package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.qualet.refreshedui.client.anim.HoverFade;
import org.qualet.refreshedui.client.anim.ListMotion;
import org.qualet.refreshedui.client.ui.IListMotionHost;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Rounds list rows (3.2a); merges adjacent multi-selected rows into one rounded block (2026-06-27) instead of
 * a stack of separate pills. The list background moved to {@code UIItems.render} in BBS 2.6 and is rounded by
 * {@link UIItemsMixin}.
 *
 * <p>BBS 2.6 (upstream 29cdbb4cd) draws every row's marks through {@link RowStyle#row} — header wash, hover
 * wash, pick wash and a 2px bar down the left edge. That call is replaced here for {@code UIList} rows only
 * (other {@code RowStyle} users — timeline tracks, section headers — are untouched).</p>
 *
 * <p>Motion: row hovers fade ({@link HoverFade}), and every list carries a {@link ListMotion} — the pick
 * slides between rows, folders unfold, dragged rows open a gap. It is updated at the head of
 * {@code renderList}, which it takes over while rows are moving, and turns the fold arrows.</p>
 */
@Mixin(UIList.class)
public abstract class UIListMixin implements IListMotionHost
{
    @Shadow
    public List<Integer> current;

    @Shadow
    protected abstract List<Object> visible();

    @Shadow
    protected abstract Boolean branch(Object element);

    @Shadow
    protected abstract int indentStep();

    @Unique
    private ListMotion refreshedui$motion;

    @Override
    public ListMotion refreshedui$motion()
    {
        if (this.refreshedui$motion == null)
        {
            this.refreshedui$motion = new ListMotion();
        }

        return this.refreshedui$motion;
    }

    @Override
    public List<?> refreshedui$visible()
    {
        return this.visible();
    }

    @Override
    public Boolean refreshedui$branch(Object row)
    {
        return this.branch(row);
    }

    @Override
    public int refreshedui$indentStep()
    {
        return this.indentStep();
    }

    @Inject(method = "renderList", at = @At("HEAD"), cancellable = true)
    private void refreshedui$moveRows(UIContext context, CallbackInfo ci)
    {
        UIList<?> self = (UIList<?>) (Object) this;
        ListMotion motion = this.refreshedui$motion();

        if (motion.begin(self, this, context))
        {
            motion.render(self, this, context);
            ci.cancel();
        }
    }

    /**
     * Rounded row marks in {@code RowStyle.row}'s order: header tint, then pick (accent, stronger under the
     * cursor) or hover (row colour / accent), then the row's own colour tag as a slim rounded pill so a
     * coloured row keeps its category. The pick rounds only the group's outer corners: square the top edge
     * when the row above is also selected and the bottom edge when the row below is. {@code index} is the
     * list index the row is drawn for (captured from the render-method arg). Hover strength fades
     * ({@link HoverFade}); a row the pick is still travelling to leaves its fill to the travelling one.
     */
    @Redirect(
        method = "renderListElement",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/RowStyle;row(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;IIIIIZZZ)V")
    )
    private void refreshedui$roundRow(Batcher2D batcher, int x, int y, int w, int h, int color, boolean header, boolean hover, boolean picked,
        @Local(argsOnly = true, ordinal = 0) int index, @Local(argsOnly = true) Object element)
    {
        float radius = UICornerRadii.buttonsAndTrackpads();
        int accent = BBSSettings.primaryColor.get() & Colors.RGB;
        int tint = color != 0 ? color & Colors.RGB : accent;
        float level = HoverFade.level(this, element, hover);

        if (header)
        {
            RoundedAreas.roundedBox(batcher, x, y, w, h, radius, Colors.A25 | tint);
        }

        if (this.refreshedui$motion().hidesPick(element))
        {
            /* The travelling pick is on its way here and paints this row's fill itself */
        }
        else if (picked)
        {
            boolean roundTop = !this.current.contains(index - 1);
            boolean roundBottom = !this.current.contains(index + 1);

            RoundedAreas.roundedBoxVertical(batcher, x, y, w, h, radius, Colors.setA(accent, 0.5F + 0.25F * level), roundTop, roundBottom);
        }
        else if (level > 0F)
        {
            RoundedAreas.roundedBox(batcher, x, y, w, h, radius, Colors.setA(tint, 0.25F * level));
        }

        if (color != 0)
        {
            RoundedAreas.roundedBox(batcher, x + 1, y + 2, RowStyle.STRIPE, h - 4, RowStyle.STRIPE / 2F, Colors.A100 | color);
        }
    }

    /** The fold arrow turns between closed and open instead of snapping (same drawing as {@code UISection.renderArrow}). */
    @Redirect(
        method = "renderArrow",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/UISection;renderArrow(Lmchorse/bbs_mod/ui/framework/UIContext;FFZI)V")
    )
    private void refreshedui$turnArrow(UIContext context, float cx, float cy, boolean expanded, int color, @Local(argsOnly = true) Object element)
    {
        float angle = this.refreshedui$motion().arrowAngle(element, expanded);
        MatrixStack matrices = context.batcher.getContext().getMatrices();

        matrices.push();
        matrices.translate(cx, cy, 0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        context.batcher.icon(Icons.ARROW_SMALL, color, 0, 0, 0.5F, 0.5F);
        matrices.pop();
    }
}
