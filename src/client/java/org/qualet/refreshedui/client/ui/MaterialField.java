package org.qualet.refreshedui.client.ui;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.anim.Animations;
import org.qualet.refreshedui.client.anim.Easings;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;

/**
 * Material 3 "filled" text field surface (design 2026-09-15), shared by text boxes, text areas and
 * trackpads: a container a step lighter than the panel, an 8% on-surface hover overlay, and a bottom
 * active indicator clipped to the container's rounded corners — 1px neutral at rest, 2px primary while
 * focused (or dragging). Hover and focus both animate over 150ms.
 */
public final class MaterialField
{
    private static final long DURATION_MS = 150L;

    /**
     * Container = a translucent tonal layer, NOT a solid colour: fields sit on every surface step (deep form
     * editors, chrome bars, raised overlays/sections), so no single colour is "a touch lighter than what's
     * behind" everywhere. A faint white wash is — on any surface and any secondary colour (black on light UIs).
     */
    /* 26/255: alpha is 8-bit and truncated (Color.getARGBColor), so 10% = 25, 10.4% = 26, 10.6% = 27, 11% = 28. */
    private static final float CONTAINER_ALPHA = 0.104F;
    /** MD3 state layer: on-surface at 8%. */
    private static final int HOVER_RGB = 0xe8e8e8;
    private static final float HOVER_ALPHA = 0.08F;
    /** Resting indicator — neutral on-surface-variant. Also the placeholder text colour. */
    public static final int ON_SURFACE_VARIANT = 0xffc8c8c8;

    /** Animated hover/focus progress for one field. Pull-based: advanced by whoever renders the field. */
    public static final class State
    {
        private float hover;
        private float focus;
        private long lastMs;

        private void update(boolean hovered, boolean active)
        {
            long now = System.currentTimeMillis();
            /* First frame (or back after being hidden for a while) snaps instead of animating from stale values. */
            float step = this.lastMs == 0L ? 1F : Math.min(1F, (now - this.lastMs) / (float) Animations.ms(DURATION_MS));

            this.lastMs = now;
            this.hover = approach(this.hover, hovered ? 1F : 0F, step);
            this.focus = approach(this.focus, active ? 1F : 0F, step);
        }

        private static float approach(float value, float target, float step)
        {
            return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
        }
    }

    public static void render(Batcher2D batcher, Area area, float radius, State state, boolean hovered, boolean active)
    {
        state.update(hovered, active);

        IRoundedBatcher rounded = (IRoundedBatcher) batcher;
        int container = Colors.setA(BBSSettings.lightSurfaces() ? 0x000000 : 0xffffff, CONTAINER_ALPHA);

        rounded.roundedBox(area.x, area.y, area.w, area.h, radius, container);

        if (state.hover > 0F)
        {
            rounded.roundedBox(area.x, area.y, area.w, area.h, radius, Colors.setA(HOVER_RGB, HOVER_ALPHA * state.hover));
        }

        float focus = Easings.outCubic(state.focus);
        /* No resting line (user pick): the indicator only exists while active, fading in with focus. */
        if (focus <= 0F)
        {
            return;
        }

        int color = Colors.setA(BBSSettings.primaryColor.get(), focus);

        /* Indicator thickness in FRAMEBUFFER pixels (1px rest -> 2px focused), not GUI pixels, so it stays a
         * hairline at any UI scale. */
        float scale = BBSModClient.getGUIScale();
        float pixel = scale > 0F ? 1F / scale : 1F;

        rounded.roundedBoxBottomBand(area.x, area.y, area.w, area.h, radius, pixel * (1F + focus), color);
    }

    private MaterialField()
    {}
}
