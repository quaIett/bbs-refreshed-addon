package org.qualet.refreshedui.client.anim;

/**
 * A stack of opacity multipliers applied to every GUI element recorded while it is pushed (see
 * {@code GuiRenderStateAlphaMixin}).
 *
 * <p>1.21.11 has no global shader colour for the GUI and records draws for a later composite, so neither
 * {@code RenderSystem.setShaderColor} nor an off-screen snapshot of the panel can fade a subtree any more.
 * Scaling the alpha of each element as it is recorded is what is left: the overlay reveal and the section
 * unfold push their visibility here around the subtree's render.</p>
 */
public final class GuiAlpha
{
    private static final float[] STACK = new float[32];

    private static int depth;
    private static float current = 1F;

    private GuiAlpha()
    {}

    public static void push(float alpha)
    {
        if (depth < STACK.length)
        {
            STACK[depth] = current;
        }

        depth++;
        current = Math.max(0F, Math.min(1F, current * alpha));
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
        }
    }

    public static boolean active()
    {
        return current < 1F;
    }

    public static int apply(int argb)
    {
        if (current >= 1F)
        {
            return argb;
        }

        int alpha = Math.round((argb >>> 24) * current);

        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /**
     * Text variant: vanilla draws a colour whose alpha is below 4 as fully OPAQUE, so a fade must never
     * push a visible text colour into that range.
     */
    public static int applyText(int argb)
    {
        if (current >= 1F || (argb >>> 24) < 4)
        {
            return argb;
        }

        int alpha = Math.max(4, Math.round((argb >>> 24) * current));

        return (alpha << 24) | (argb & 0xFFFFFF);
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
}
