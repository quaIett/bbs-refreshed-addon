package org.qualet.refreshedui.client.ui;

/**
 * Rounded-rectangle corner radii for the UI.
 *
 * <p>A single place to resolve the UI corner radius so call sites read intent
 * ({@code interfaceChrome()} vs {@code buttonsAndTrackpads()}) instead of poking the constant
 * directly. Both currently return the same value, but keeping them separate lets us tune
 * categories independently later.</p>
 *
 * <p>The radius is fixed at {@link #RADIUS}px — the corner-rounding intensity setting was removed,
 * so rounding is no longer user-configurable.</p>
 */
public final class UICornerRadii
{
    /** Fixed UI corner radius in px (rounding intensity is no longer configurable). */
    private static final int RADIUS = 5;

    private static int radius()
    {
        return RADIUS;
    }

    /** Buttons and trackpads. */
    public static int buttonsAndTrackpads()
    {
        return radius();
    }

    /** Panels, text fields, overlays, cards, etc. */
    public static int interfaceChrome()
    {
        return radius();
    }

    /**
     * {@link #interfaceChrome()} capped so the radius never exceeds half of the shorter side
     * of a {@code w}×{@code h} widget (minus a 0.5px margin to keep the anti-aliased edge inside).
     */
    public static float interfaceChromeClamped(int w, int h)
    {
        if (w <= 0 || h <= 0)
        {
            return 0.5F;
        }

        float r = Math.min((float) interfaceChrome(), Math.min(w * 0.5F, h * 0.5F) - 0.5F);

        return r < 0.5F ? 0.5F : r;
    }

    private UICornerRadii()
    {}
}
