package org.qualet.refreshedui.client.anim;

/**
 * A stack of opacity multipliers applied to every GUI element recorded while it is pushed (see
 * {@code GuiRenderStateAlphaMixin}).
 *
 * <p>1.21.11 has no global shader colour for the GUI and records draws for a later composite, so neither
 * {@code RenderSystem.setShaderColor} nor an off-screen snapshot of the panel can fade a subtree any more.
 * Scaling the alpha of each element as it is recorded is what is left: the overlay reveal and the section
 * unfold push their visibility here around the subtree's render.</p>
 *
 * <p>Each level can also darken ({@code shade} multiplies the RGB) — what a pressed button uses in place of
 * a shader colour.</p>
 */
public final class GuiAlpha
{
    private static final float[] STACK = new float[32];
    private static final float[] SHADES = new float[32];

    private static int depth;
    private static float current = 1F;
    private static float shade = 1F;

    private GuiAlpha()
    {}

    public static void push(float alpha)
    {
        push(alpha, 1F);
    }

    /** Push an opacity AND a brightness multiplier (0..1 each). */
    public static void push(float alpha, float shadeFactor)
    {
        if (depth < STACK.length)
        {
            STACK[depth] = current;
            SHADES[depth] = shade;
        }

        depth++;
        current = Math.max(0F, Math.min(1F, current * alpha));
        shade = Math.max(0F, Math.min(1F, shade * shadeFactor));
    }

    public static void pop()
    {
        if (depth <= 0)
        {
            return;
        }

        depth--;

        if (depth < STACK.length)
        {
            current = STACK[depth];
            shade = SHADES[depth];
        }
    }

    /** Record {@code body} at {@code alpha} of its opacity (the 1.21.11 stand-in for a composited fade). */
    public static void fade(float alpha, Runnable body)
    {
        if (alpha >= 1F)
        {
            body.run();

            return;
        }

        push(alpha);

        try
        {
            body.run();
        }
        finally
        {
            pop();
        }
    }

    public static boolean active()
    {
        return current < 1F || shade < 1F;
    }

    public static int apply(int argb)
    {
        if (!active())
        {
            return argb;
        }

        int alpha = Math.round((argb >>> 24) * current);

        return (alpha << 24) | shadeRgb(argb);
    }

    /**
     * Text variant: vanilla draws a colour whose alpha is below 4 as fully OPAQUE, so a fade must never
     * push a visible text colour into that range.
     */
    public static int applyText(int argb)
    {
        if (!active() || (argb >>> 24) < 4)
        {
            return argb;
        }

        int alpha = Math.max(4, Math.round((argb >>> 24) * current));

        return (alpha << 24) | shadeRgb(argb);
    }

    public static int[] apply(int[] colors, int count)
    {
        int[] faded = new int[colors.length];

        for (int i = 0; i < count; i++)
        {
            faded[i] = apply(colors[i]);
        }

        return faded;
    }

    private static int shadeRgb(int argb)
    {
        if (shade >= 1F)
        {
            return argb & 0xFFFFFF;
        }

        int r = Math.round(((argb >> 16) & 0xFF) * shade);
        int g = Math.round(((argb >> 8) & 0xFF) * shade);
        int b = Math.round((argb & 0xFF) * shade);

        return (r << 16) | (g << 8) | b;
    }
}
