package org.qualet.refreshedui.client.anim;

/**
 * Strength of BBS's background blur for the blur applied right now (1 = full).
 *
 * <p>BBS has no close transition for overlay panels, so while our close animation keeps the overlay alive
 * the blur behind it would stay at full strength and vanish at once on detach. {@code UIOverlayMixin} scales
 * it by the overlay's visibility instead, so it fades out together with the panel and the dimming.</p>
 *
 * <p>BBS takes the blur strength as a whole number of pixels, so the fade lands on that grid.</p>
 */
public final class BlurFade
{
    private static float strength = 1F;

    private BlurFade()
    {}

    public static void begin(float value)
    {
        strength = Math.max(0F, Math.min(1F, value));
    }

    public static void end()
    {
        strength = 1F;
    }

    public static int radius(int radius)
    {
        return strength >= 1F ? radius : Math.round(radius * strength);
    }
}
