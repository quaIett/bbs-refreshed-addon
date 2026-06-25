package org.qualet.refreshedui.client.ui;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Adaptive foreground color: pick white or black, whichever reads better over a given fill.
 *
 * <p>Single source of truth for the refreshed theme's two-mode contrast rule. Button labels and
 * active icon-button glyphs sit directly on the BBS primary color (or a custom color); a fixed
 * black/white foreground becomes unreadable once the fill drifts to the other end of the brightness
 * range. {@link #contrastOn(int)} resolves the readable tone instead of hard-coding {@code A100}.</p>
 */
public final class UIContrastColor
{
    /** Perceived-brightness midpoint. Fills brighter than this want black text, darker ones white. */
    private static final float LUMA_THRESHOLD = 0.5F;

    /**
     * {@link Colors#A100 black} for a light fill, {@link Colors#WHITE white} for a dark one, based on
     * the perceived brightness of {@code background} (its alpha is ignored).
     */
    public static int contrastOn(int background)
    {
        return isLight(background) ? Colors.A100 : Colors.WHITE;
    }

    /** Readable foreground over the current BBS primary color (the active-highlight fill). */
    public static int onPrimary()
    {
        return contrastOn(BBSSettings.primaryColor.get() | Colors.A100);
    }

    /**
     * Whether {@code color} is perceptually light (ITU-R BT.601 luma over the sRGB channels above the
     * {@link #LUMA_THRESHOLD midpoint}).
     */
    public static boolean isLight(int color)
    {
        float luma = 0.299F * Colors.getR(color)
            + 0.587F * Colors.getG(color)
            + 0.114F * Colors.getB(color);

        return luma > LUMA_THRESHOLD;
    }

    private UIContrastColor()
    {}
}
