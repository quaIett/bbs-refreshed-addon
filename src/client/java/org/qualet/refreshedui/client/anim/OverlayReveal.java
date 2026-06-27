package org.qualet.refreshedui.client.anim;

import mchorse.bbs_mod.ui.framework.elements.UIElement;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Drives the "overlay appear" reveal. When a popup overlay is added — confirm / prompt / list / colour
 * picker and every other {@code UIOverlayPanel} — its panel <b>slides up</b> a few pixels into place while
 * fading in, and the full-screen backdrop dims from transparent to its resting alpha. A single
 * {@link Animator} is shared by the overlay (backdrop) and its panel, so the two stay in lock-step.
 *
 * <p>Armed from {@code UIOverlay.setupPanel} (see {@code UIOverlayMixin}); the panel slide+fade is applied
 * in {@code UIOverlayPanelMixin}, the backdrop fade in {@code UIOverlayMixin}. Built on the animation core
 * and gated on {@link Animations#enabled()}. All access is from the render thread, so no synchronization is
 * needed.</p>
 */
public final class OverlayReveal
{
    /** How long the appear animation lasts. */
    public static final long DURATION_MS = 240L;
    /** How far below its resting position the panel starts, in pixels, while it slides up into place. */
    public static final float SLIDE_PX = 14F;

    /**
     * Overlays/panels currently animating, keyed by identity. Both the overlay and its panel map to the same
     * animator. Finished/abandoned entries are swept on the next {@link #arm}.
     */
    private static final Map<UIElement, Animator> active = new IdentityHashMap<>();

    private OverlayReveal()
    {}

    /**
     * Arm a fresh reveal shared by an overlay and its panel. No-op while animations are disabled. Sweeps any
     * finished entries first so overlays closed before their reveal completed do not accumulate (the next
     * open clears the previous one once its window has elapsed).
     */
    public static void arm(UIElement overlay, UIElement panel)
    {
        if (!Animations.enabled())
        {
            return;
        }

        active.values().removeIf(a -> a.finished(DURATION_MS));

        Animator anim = Animator.now();

        if (overlay != null)
        {
            active.put(overlay, anim);
        }

        if (panel != null)
        {
            active.put(panel, anim);
        }
    }

    /**
     * The active reveal animator for an overlay or its panel, or null when nothing applies (settled,
     * disabled, or never armed). A finished reveal drops its entry so the element renders normally after.
     */
    public static Animator animator(UIElement element)
    {
        if (active.isEmpty() || !Animations.enabled())
        {
            return null;
        }

        Animator anim = active.get(element);

        if (anim == null)
        {
            return null;
        }

        if (anim.finished(DURATION_MS))
        {
            active.remove(element);

            return null;
        }

        return anim;
    }

    /** Eased reveal progress in [0, 1] for an overlay/panel; 1 when nothing is animating (fully shown). */
    public static float progress(UIElement element)
    {
        Animator anim = animator(element);

        return anim == null ? 1F : anim.progress(0L, DURATION_MS, Easings.OUT_CUBIC);
    }
}
