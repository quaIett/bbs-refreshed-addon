package org.qualet.refreshedui.client.anim;

/**
 * Built-in {@link Easing} curves for the animation core. All operate on a normalized {@code t} in
 * [0, 1]; inputs are clamped so callers may pass raw (possibly out-of-range) progress safely.
 */
public final class Easings
{
    private Easings()
    {}

    public static final Easing LINEAR = Easings::linear;
    public static final Easing OUT_CUBIC = Easings::outCubic;
    public static final Easing OUT_QUART = Easings::outQuart;
    public static final Easing OUT_EXPO = Easings::outExpo;
    public static final Easing IN_OUT_CUBIC = Easings::inOutCubic;

    private static float clamp01(float t)
    {
        return t < 0F ? 0F : (t > 1F ? 1F : t);
    }

    public static float linear(float t)
    {
        return clamp01(t);
    }

    /** Decelerating cubic — fast start, soft landing. The default for UI reveals. */
    public static float outCubic(float t)
    {
        float u = 1F - clamp01(t);

        return 1F - u * u * u;
    }

    public static float outQuart(float t)
    {
        float u = 1F - clamp01(t);

        return 1F - u * u * u * u;
    }

    public static float outExpo(float t)
    {
        t = clamp01(t);

        return t >= 1F ? 1F : 1F - (float) Math.pow(2F, -10F * t);
    }

    public static float inOutCubic(float t)
    {
        t = clamp01(t);

        return t < 0.5F
            ? 4F * t * t * t
            : 1F - (float) Math.pow(-2F * t + 2F, 3F) / 2F;
    }
}
