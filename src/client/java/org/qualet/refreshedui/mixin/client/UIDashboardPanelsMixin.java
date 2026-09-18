package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Direction;
import org.qualet.refreshedui.client.anim.SegmentSlide;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Taskbar:
 * <ul>
 *   <li>3.9 — every panel button gets a per-frame active flag so the active one's icon draws with the
 *       adaptive contrast color (white/black by primary brightness).</li>
 *   <li>The active-panel mark slides along the bar to the newly opened panel ({@link SegmentSlide}). The
 *       bar draws that one mark itself, before its buttons — each button used to draw its own through
 *       {@code UIIcon.highlight}, which is switched off here — and a button's icon turns to the contrast
 *       colour as the mark arrives under it.</li>
 * </ul>
 *
 * <p>The engine-wide rounded selection highlight (3.8) used to be a HEAD inject on the static
 * {@code renderHighlight} here; BBS 2.6 moved that helper to {@code Batcher2D.highlight}, so it now lives in
 * {@link Batcher2DHighlightMixin}.</p>
 */
@Mixin(UIDashboardPanels.class)
public abstract class UIDashboardPanelsMixin
{
    @Unique
    private static final BooleanSupplier NEVER = () -> false;

    @Shadow
    public List<UIDashboardPanel> panels;

    @Shadow
    public UIScrollView panelButtons;

    @Shadow
    public UIDashboardPanel panel;

    @Shadow
    public abstract Direction getSide();

    @Unique
    private SegmentSlide refreshedui$slide;

    /**
     * Wrap the existing pre-render callback (it runs inside the button strip's scroll, before the buttons)
     * so the mark and the buttons' active state are set up every frame, then delegate to the original.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$slidingPanelMark(CallbackInfo ci)
    {
        Consumer<UIContext> original = this.panelButtons.preRenderCallback;

        this.panelButtons.preRender((context) ->
        {
            this.refreshedui$renderMark(context);

            if (original != null)
            {
                original.accept(context);
            }
        });
    }

    @Unique
    private void refreshedui$renderMark(UIContext context)
    {
        if (this.refreshedui$slide == null)
        {
            this.refreshedui$slide = new SegmentSlide();
        }

        int count = Math.min(this.panels.size(), this.panelButtons.getChildren().size());
        int active = this.panels.indexOf(this.panel);
        int contrast = UIContrastColor.onPrimary();
        Direction side = this.getSide();

        if (active < 0 || active >= count)
        {
            for (int i = 0; i < count; i++)
            {
                ((UIIcon) this.panelButtons.getChildren().get(i)).highlight(NEVER, side).active(false);
            }

            return;
        }

        float shown = this.refreshedui$slide.update(active);

        for (int i = 0; i < count; i++)
        {
            UIIcon button = (UIIcon) this.panelButtons.getChildren().get(i);
            float cover = SegmentSlide.coverage(shown, i);

            button.highlight(NEVER, side).active(cover > 0F).activeColor(SegmentSlide.mix(button.iconColor, contrast, cover));
        }

        /* Between two buttons the mark is where it would be a fraction of the way from one to the other.
         * Drawn at fractional pixels (the look Batcher2DHighlightMixin gives highlight(), without its int
         * Area) so the end of the ease doesn't step. */
        int from = Math.max(0, Math.min(count - 1, (int) Math.floor(shown)));
        int to = Math.min(count - 1, from + 1);
        float f = shown - from;
        Area a = ((UIIcon) this.panelButtons.getChildren().get(from)).area;
        Area b = ((UIIcon) this.panelButtons.getChildren().get(to)).area;

        RoundedAreas.roundedBox(context.batcher, a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f, a.w, a.h,
            UICornerRadii.buttonsAndTrackpads(), RoundedAreas.highlightFill(BBSSettings.primaryColor.get()));
    }
}
