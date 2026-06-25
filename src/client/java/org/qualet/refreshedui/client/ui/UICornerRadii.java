package org.qualet.refreshedui.client.ui;

import org.qualet.refreshedui.RefreshedUiAddon;

/**
 * Rounded-rectangle corner radii sourced from the addon settings (appearance).
 *
 * <p>A single place to resolve the UI corner radius so call sites read intent
 * ({@code interfaceChrome()} vs {@code buttonsAndTrackpads()}) instead of poking the setting
 * directly. Both currently return the same value, but keeping them separate lets us tune
 * categories independently later.</p>
 *
 * <p>Ported from {@code mchorse.bbs_mod.ui.utils.UICornerRadii}; reads
 * {@link RefreshedUiAddon#uiCornerRadius} (addon-owned) instead of {@code BBSSettings}.</p>
 */
public final class UICornerRadii
{
    /** Fallback used before {@link RefreshedUiAddon#uiCornerRadius} is registered. Matches its default. */
    private static final int DEFAULT_RADIUS = 4;

    private static int radius()
    {
        return RefreshedUiAddon.uiCornerRadius == null
            ? DEFAULT_RADIUS
            : RefreshedUiAddon.uiCornerRadius.get();
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
