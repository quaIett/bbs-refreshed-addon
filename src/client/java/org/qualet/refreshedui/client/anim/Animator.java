package org.qualet.refreshedui.client.anim;

/**
 * A wall-clock timeline for driving animations. An {@code Animator} is just a start timestamp;
 * animated objects query it each frame to obtain their own eased progress.
 *
 * <p>The animation core is intentionally pull-based and stateless beyond the start time: there is no
 * per-frame "tick" to forget to call. A single {@code Animator} can drive many independent sub-tweens
 * (e.g. one per glyph in a staggered text reveal) by passing a different {@code delayMs} to
 * {@link #progress(long, long, Easing)}.</p>
 *
 * <p>Time source is {@link System#currentTimeMillis()}, matching the rest of the addon's animations
 * (see {@code UIToggleMixin}). All access is from the render thread, so no synchronization is needed.</p>
 */
public final class Animator
{
    private final long startMs;

    private Animator(long startMs)
    {
        this.startMs = startMs;
    }

    /** Start a fresh timeline at the current time. */
    public static Animator now()
    {
        return new Animator(System.currentTimeMillis());
    }

    /** Milliseconds elapsed since this timeline started. */
    public long elapsed()
    {
        return System.currentTimeMillis() - this.startMs;
    }

    /**
     * Eased progress in [0, 1] for a sub-tween that begins {@code delayMs} into this timeline and lasts
     * {@code durationMs}. Returns 0 before the sub-tween starts and 1 once it has completed.
     */
    public float progress(long delayMs, long durationMs, Easing easing)
    {
        if (durationMs <= 0L)
        {
            return this.elapsed() >= delayMs ? 1F : 0F;
        }

        float t = (this.elapsed() - delayMs) / (float) durationMs;

        if (t <= 0F)
        {
            return 0F;
        }

        if (t >= 1F)
        {
            return 1F;
        }

        return easing.ease(t);
    }

    /** True once {@code totalMs} have elapsed since the timeline started. */
    public boolean finished(long totalMs)
    {
        return this.elapsed() >= totalMs;
    }
}
