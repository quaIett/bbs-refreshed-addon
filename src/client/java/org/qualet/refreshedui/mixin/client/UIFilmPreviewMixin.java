package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.UIFilmPreview;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.qualet.refreshedui.client.anim.Animations;
import org.qualet.refreshedui.client.anim.Animator;
import org.qualet.refreshedui.client.anim.Easings;
import org.qualet.refreshedui.client.anim.EditorSwitchFade;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Preview control bar:
 * <ul>
 *   <li>3.9 — active buttons draw the adaptive contrast icon (white/black by primary brightness) over
 *       the highlight. The icon glyphs are drawn by the UIIcon children during {@code super.render(...)},
 *       after this HEAD inject.</li>
 *   <li>3.10 — the bar gradient becomes a rounded chromeSurface bar, and the bar is offset up from the
 *       bottom edge. The 5 active control highlights become rounded fills via the global
 *       {@code UIDashboardPanels.renderHighlight} inject (see {@link UIDashboardPanelsMixin}).</li>
 *   <li>With BBS's "auto-hide preview icons" on, the bar slides down out of the preview and back up
 *       (in-out quad) instead of popping, when interface animations are enabled.</li>
 *   <li>While the Film panel fades between editors, the world picture glides to its new place instead
 *       of jumping ({@link EditorSwitchFade}).</li>
 * </ul>
 */
@Mixin(UIFilmPreview.class)
public abstract class UIFilmPreviewMixin
{
    /** Bar shell expands this many px beyond the icon row on each side (refreshed). */
    private static final int REFRESHEDUI_BAR_EDGE_GAP = 2;
    /** Distance from preview bottom edge to the controls bar, px (refreshed). */
    private static final int REFRESHEDUI_TOOLBAR_MARGIN_FROM_EDGE_PX = 7;
    /** Full slide duration of the auto-hiding bar, ms. */
    private static final long REFRESHEDUI_SLIDE_MS = 220L;
    /** A render gap longer than this means the preview was off screen: snap instead of animating. */
    private static final long REFRESHEDUI_SLIDE_SNAP_GAP_MS = 250L;

    /** Where the bar is heading: true = shown. */
    @Unique
    private boolean refreshedui$slideShown = true;
    /** Shown-progress the running slide started from. */
    @Unique
    private float refreshedui$slideFrom = 1F;
    /** Running slide, null when settled. */
    @Unique
    private Animator refreshedui$slide;
    /** How far down the bar is currently laid out, px (0 = resting place). */
    @Unique
    private int refreshedui$slideOffset;
    @Unique
    private long refreshedui$slideLastFrame;

    @Shadow
    private UIFilmPanel panel;

    @Shadow
    public UIElement icons;

    @Shadow
    public UIIcon onionSkin;

    @Shadow
    public UIIcon flight;

    @Shadow
    public UIIcon control;

    @Shadow
    public UIIcon recordReplay;

    @Shadow
    public UIIcon recordVideo;

    /** 3.9: active control buttons render the adaptive contrast icon over the highlight. */
    @Inject(method = "render", at = @At("HEAD"))
    private void refreshedui$blackenActiveControls(UIContext context, CallbackInfo ci)
    {
        int active = UIContrastColor.onPrimary();

        this.flight.active(this.panel.isFlying()).activeColor(active);
        this.control.active(this.panel.getController().isControlling()).activeColor(active);
        this.recordReplay.active(this.panel.getController().isRecording()).activeColor(active);
        this.recordVideo.active(this.panel.recorder.isRecording()).activeColor(active);
        this.onionSkin.active(this.panel.getController().getOnionSkin().enabled.get()).activeColor(active);
    }

    /** Editor switch fade: the world picture is drawn gliding from its old place while the switch fades. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;texturedBox(IIFFFFFFFFII)V", ordinal = 0)
    )
    private void refreshedui$glideWorldPicture(Batcher2D batcher, int texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        if (!EditorSwitchFade.drawPreview((UIFilmPreview) (Object) this, batcher, texture, color, x, y, w, h, u1, v1, u2, v2, textureW, textureH))
        {
            batcher.texturedBox(texture, color, x, y, w, h, u1, v1, u2, v2, textureW, textureH);
        }
    }

    /** 3.10: offset the control bar up from the preview bottom edge. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$offsetIconBar(CallbackInfo ci)
    {
        this.icons.y(1F, -REFRESHEDUI_TOOLBAR_MARGIN_FROM_EDGE_PX);
    }

    /** 3.10: replace the gradient bar with a rounded chromeSurface shell. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;gradientVBox(FFFFII)V")
    )
    private void refreshedui$roundedBar(Batcher2D batcher, float x1, float y1, float x2, float y2, int topColor, int bottomColor, UIContext context)
    {
        float px = x1 - REFRESHEDUI_BAR_EDGE_GAP;
        float py = y1 - REFRESHEDUI_BAR_EDGE_GAP;
        float pw = (x2 - x1) + 2F * REFRESHEDUI_BAR_EDGE_GAP;
        float ph = (y2 - y1) + 2F * REFRESHEDUI_BAR_EDGE_GAP;

        if (pw > 1F && ph > 1F)
        {
            /* Capsule radius (half height vs half width), then a quarter of that for subtle corners. */
            float capsuleR = Math.min(ph * 0.5F - 0.5F, pw * 0.5F - 0.5F);
            float rr = capsuleR * 0.25F;

            /* The bar shell is drawn before BBS clips to the preview, so a sliding bar would spill onto the timeline. */
            batcher.clip(((UIElement) (Object) this).area, context);
            ((IRoundedBatcher) batcher).roundedBox(px, py, pw, ph, Math.max(1F, rr), BBSSettings.chromeSurface());
            batcher.unclip(context);
        }
    }

    /**
     * Auto-hide slide: BBS decides whether the bar should be shown and flips its visibility here; instead
     * the bar keeps rendering while it slides down out of the preview (or back up), and only really hides
     * once it is fully out. The slide moves the layout, so clicks and hover follow the icons.
     */
    @Redirect(
        method = "updateIconsVisibility",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/UIElement;setVisible(Z)V")
    )
    private void refreshedui$slideIcons(UIElement icons, boolean visible)
    {
        long now = System.currentTimeMillis();
        boolean snap = !Animations.enabled()
            || !BBSSettings.editorPreviewIconsAutoHide.get()
            || now - this.refreshedui$slideLastFrame > REFRESHEDUI_SLIDE_SNAP_GAP_MS;

        this.refreshedui$slideLastFrame = now;

        if (snap)
        {
            this.refreshedui$slideShown = visible;
            this.refreshedui$slide = null;
        }
        else if (visible != this.refreshedui$slideShown)
        {
            /* Reverse from wherever the bar is right now, so a quick in-out never jumps. */
            this.refreshedui$slideFrom = this.refreshedui$slideProgress();
            this.refreshedui$slideShown = visible;
            this.refreshedui$slide = Animator.now();
        }

        float p = this.refreshedui$slideProgress();

        if (this.refreshedui$slide != null && p == (this.refreshedui$slideShown ? 1F : 0F))
        {
            this.refreshedui$slide = null;
        }

        icons.setVisible(p > 0F);

        int distance = icons.area.h + REFRESHEDUI_TOOLBAR_MARGIN_FROM_EDGE_PX + REFRESHEDUI_BAR_EDGE_GAP + 1;
        int offset = Math.round((1F - p) * distance);

        if (offset != this.refreshedui$slideOffset)
        {
            this.refreshedui$slideOffset = offset;
            icons.y(1F, -REFRESHEDUI_TOOLBAR_MARGIN_FROM_EDGE_PX + offset);
            icons.resize();
        }
    }

    /** Shown-progress of the bar in [0, 1]; 1 = at its resting place. */
    @Unique
    private float refreshedui$slideProgress()
    {
        float target = this.refreshedui$slideShown ? 1F : 0F;

        if (this.refreshedui$slide == null)
        {
            return target;
        }

        float from = this.refreshedui$slideFrom;
        long duration = Math.max(1L, (long) (REFRESHEDUI_SLIDE_MS * Math.abs(target - from)));
        float t = this.refreshedui$slide.progress(0L, duration, Easings.LINEAR);

        return from + (target - from) * Easings.inOutQuad(t);
    }

    /** The bar shell keeps drawing while the bar is still sliding out. */
    @ModifyExpressionValue(
        method = "render",
        at = @At(value = "FIELD", target = "Lmchorse/bbs_mod/ui/film/UIFilmPreview;iconsVisible:Z")
    )
    private boolean refreshedui$barShellWhileSliding(boolean visible)
    {
        return visible || this.refreshedui$slideProgress() > 0F;
    }

    /** HUD room above the bar stays fixed at the bar's resting place, not wherever the slide has it. */
    @ModifyArg(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/film/PreviewHud;reserve(Lmchorse/bbs_mod/ui/film/PreviewHud$Anchor;I)V"),
        index = 1
    )
    private int refreshedui$pinHudReserve(int amount)
    {
        return amount + this.refreshedui$slideOffset;
    }
}
