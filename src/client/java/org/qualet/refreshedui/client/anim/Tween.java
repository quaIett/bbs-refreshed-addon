package org.qualet.refreshedui.client.anim;

/**
 * A number that eases from where it currently is toward a target — what a marker shows while it catches
 * up with the value it already has (a selection pill between rows, the highlight between tabs).
 *
 * <p>Pull-based like {@link Animator}: nothing ticks it, the value is worked out from the clock whenever it
 * is read, so a tween nobody reads costs nothing. Retargeting mid-flight starts the new run from the
 * value on screen, so a marker sent somewhere else halfway through turns around smoothly instead of
 * jumping.</p>
 *
 * <p>Time is {@link #now()}, a monotonic millisecond clock (the wall clock on Windows can tick in 15 ms
 * steps, which shows on a 150 ms slide). Honours the {@link Animations#enabled()} switch: with animations
 * off a retarget lands at once.</p>
 */
public final class Tween
{
    private float from;
    private float to;
    private long start;
    private long duration;
    private Easing easing = Easings.OUT_CUBIC;

    public Tween(float value)
    {
        this.from = this.to = value;
    }

    /** Monotonic milliseconds, for every timing in the motion helpers built on this class. */
    public static long now()
    {
        return System.nanoTime() / 1_000_000L;
    }

    public float value()
    {
        return this.value(now());
    }

    public float value(long now)
    {
        if (this.duration <= 0L)
        {
            return this.to;
        }

        float t = (now - this.start) / (float) this.duration;

        if (t >= 1F)
        {
            this.duration = 0L;

            return this.to;
        }

        if (t <= 0F)
        {
            return this.from;
        }

        return this.from + (this.to - this.from) * this.easing.ease(t);
    }

    public float target()
    {
        return this.to;
    }

    public boolean isAnimating(long now)
    {
        return this.duration > 0L && now - this.start < this.duration;
    }

    /** Head for {@code target} from the value shown right now; a no-op when already heading there. */
    public void animateTo(float target, long durationMs, Easing easing, long now)
    {
        if (target == this.to)
        {
            return;
        }

        if (durationMs <= 0L || !Animations.enabled())
        {
            this.snap(target);

            return;
        }

        this.from = this.value(now);
        this.to = target;
        this.start = now;
        this.duration = durationMs;
        this.easing = easing;
    }

    public void snap(float value)
    {
        this.from = this.to = value;
        this.duration = 0L;
    }
}
