package org.qualet.refreshedui.client.anim;

/**
 * The "chosen" mark of a row of segments (a mode strip, the task bar) slides to the newly chosen one
 * instead of reappearing there.
 *
 * <p>Works in segment units — the mark stands at a fractional segment index — so a strip that is laid out
 * again mid-slide (resized, scrolled) keeps sliding between the right places. The owner draws the mark
 * at {@link #update}'s answer and tints each segment's icon by how much of it the mark covers
 * ({@link #coverage}), so a glyph only turns "chosen" as the mark arrives under it.</p>
 */
public final class SegmentSlide
{
    public static final long DURATION_MS = 180L;

    private final Tween position = new Tween(0F);
    private int value = Integer.MIN_VALUE;

    /** Where the mark stands this frame (segment index, fractional mid-slide) for the chosen {@code value}. */
    public float update(int value)
    {
        long now = Tween.now();

        if (this.value == Integer.MIN_VALUE || !Animations.enabled())
        {
            this.value = value;
            this.position.snap(value);

            return value;
        }

        if (value != this.value)
        {
            this.value = value;
            this.position.animateTo(value, DURATION_MS, Easings.OUT_CUBIC, now);
        }

        return this.position.value(now);
    }

    /** How much of segment {@code index} a mark standing at {@code shown} covers, 0..1. */
    public static float coverage(float shown, int index)
    {
        return Math.max(0F, 1F - Math.abs(index - shown));
    }

    /** Straight per-channel blend of two ARGB colours. */
    public static int mix(int a, int b, float t)
    {
        if (t <= 0F)
        {
            return a;
        }

        if (t >= 1F)
        {
            return b;
        }

        int aa = a >>> 24, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = b >>> 24, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;

        return (Math.round(aa + (ba - aa) * t) << 24)
            | (Math.round(ar + (br - ar) * t) << 16)
            | (Math.round(ag + (bg - ag) * t) << 8)
            | Math.round(ab + (bb - ab) * t);
    }
}
