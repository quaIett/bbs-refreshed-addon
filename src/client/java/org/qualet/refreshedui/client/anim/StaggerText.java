package org.qualet.refreshedui.client.anim;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Staggered text reveal: each glyph animates in independently, offset from the previous by a small
 * delay. Per glyph the effect is a fade-in (alpha 0 -&gt; 1) combined with a slide-up (the glyph starts
 * a few pixels below its resting baseline and rises into place), eased with {@link Easings#OUT_CUBIC}.
 *
 * <p>Each glyph is drawn on its own and positioned at the layout advance of the prefix before it, so the
 * letters land where the renderer would place them. Only alpha and vertical offset animate. Driven by
 * {@link PanelTransitions} when an editor/panel appears.</p>
 */
public final class StaggerText
{
    /** Per-glyph reveal duration. */
    public static final long LETTER_DURATION_MS = 300L;
    /** Delay between consecutive glyphs starting. */
    public static final long LETTER_STAGGER_MS = 30L;
    /** How far below its baseline a glyph begins, in pixels. */
    public static final float SLIDE_PX = 6F;

    /**
     * Re-entrancy guard: {@link #render} draws each glyph through {@code Batcher2D.text}, which is itself
     * intercepted to trigger the stagger. While this flag is set, that interceptor must let the glyph
     * draw pass through unchanged instead of re-staggering it.
     */
    private static boolean rendering;

    private StaggerText()
    {}

    /** True while {@link #render} is emitting its per-glyph draws (see {@link #rendering}). */
    public static boolean isRendering()
    {
        return rendering;
    }

    /** Total time from start until the last glyph of a {@code length}-char string has fully settled. */
    public static long totalDurationMs(int length)
    {
        if (length <= 0)
        {
            return 0L;
        }

        return (long) (length - 1) * LETTER_STAGGER_MS + LETTER_DURATION_MS;
    }

    /**
     * Render {@code text} with each glyph staggered according to {@code animator}'s elapsed time. The
     * {@code x}/{@code y} are the resting top-left position the text would normally be drawn at.
     */
    public static void render(Batcher2D batcher, FontRenderer font, String text, float x, float y, int color, boolean shadow, Animator animator)
    {
        long elapsed = animator.elapsed();

        rendering = true;

        try
        {
            for (int i = 0; i < text.length(); i++)
            {
                float t = (elapsed - (long) i * LETTER_STAGGER_MS) / (float) LETTER_DURATION_MS;

                if (t > 0F)
                {
                    float eased = Easings.outCubic(t);
                    float slide = (1F - eased) * SLIDE_PX;
                    int glyphColor = Colors.mulA(color, eased);

                    /* Position the glyph at the layout advance of the prefix before it — the exact x the
                     * renderer places it at in the full string. Summing per-glyph getWidth() instead would
                     * over-space the letters (side bearings / kerning are not additive). batcher.text()
                     * forces opacity when alpha hits exactly 0, so the t > 0 guard keeps not-yet-started
                     * glyphs unrendered rather than popping in at full opacity. */
                    float glyphX = x + font.getWidth(text.substring(0, i));

                    batcher.text(String.valueOf(text.charAt(i)), glyphX, y + slide, glyphColor, shadow);
                }
            }
        }
        finally
        {
            rendering = false;
        }
    }
}
