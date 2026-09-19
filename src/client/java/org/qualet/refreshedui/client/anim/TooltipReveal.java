package org.qualet.refreshedui.client.anim;

/**
 * Hover tooltips wait a moment before they show, then fade in while sliding the last few pixels out of
 * the element they belong to.
 *
 * <p>Stock BBS shows a tooltip the frame the cursor lands on its element, so sweeping the mouse across a
 * toolbar flashes a box per icon. Here a tooltip first waits {@link #DELAY_MS}; once one has been on
 * screen, the next one follows without waiting or fading for {@link #GRACE_MS} — running along a row of
 * icons reads their names one after another, the way desktop toolbars behave.</p>
 *
 * <p>State is one "current tooltip owner" — BBS shows one tooltip at a time and resets the owner every
 * frame before the UI re-registers it, so identity of the owner is what tells "still the same tooltip"
 * from "a new one".</p>
 */
public final class TooltipReveal
{
    public static final long DELAY_MS = 350L;
    public static final long FADE_MS = 120L;
    public static final long GRACE_MS = 300L;
    public static final float SLIDE_PX = 3F;

    /** Returned by {@link #visibility} while the tooltip is still waiting its turn. */
    public static final float HIDDEN = -1F;

    private static Object current;
    private static long revealAt;
    private static long lastShownMs = Long.MIN_VALUE / 2L;

    private TooltipReveal()
    {}

    /**
     * How visible the tooltip of {@code owner} is this frame: {@link #HIDDEN} while it waits, then 0..1
     * through the fade (1 = draw as stock).
     */
    public static float visibility(Object owner)
    {
        if (!Animations.enabled())
        {
            current = owner;

            return 1F;
        }

        long now = Tween.now();
        long fade = Animations.ms(FADE_MS);

        if (owner != current)
        {
            current = owner;
            /* Straight after another tooltip: no wait, no fade — it is the same act of reading */
            revealAt = now - lastShownMs <= GRACE_MS ? now - fade : now + DELAY_MS;
        }

        if (now < revealAt)
        {
            return HIDDEN;
        }

        lastShownMs = now;

        float t = (now - revealAt) / (float) fade;

        return t >= 1F ? 1F : Easings.outCubic(t);
    }

    /** No tooltip this frame. The next one starts over (unless it follows within the grace window). */
    public static void hidden()
    {
        current = null;
    }
}
