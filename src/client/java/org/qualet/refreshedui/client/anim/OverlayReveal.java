package org.qualet.refreshedui.client.anim;

import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Drives the popup overlay appear/close animation. When a {@code UIOverlayPanel} overlay opens, its panel
 * <b>slides up</b> a few pixels into place while fading in and the full-screen backdrop dims from
 * transparent to its resting alpha; when it closes, the reverse plays — the panel slides back down and
 * fades out, the backdrop lightens. A single {@link Animator} is shared by an overlay and its panel so the
 * backdrop and panel stay in lock-step.
 *
 * <p>Both directions reduce to one {@link #visibility} value (0 hidden &rarr; 1 shown for appear, 1 &rarr;
 * 0 for close); the panel slide+fade and backdrop fade are the same math either way. The slide is a matrix
 * translate and the fade is the global shader colour (see {@code UIOverlayPanelMixin} / {@code UIOverlayMixin}).</p>
 *
 * <p><b>Close defers the detach.</b> BBS {@code UIOverlay.closeItself} removes the overlay from the tree at
 * once, leaving nothing to animate. Instead the close logic runs immediately but the overlay is kept in the
 * tree, armed with a reverse reveal, and only detached once {@link #finishClosed} sees the animation
 * through — called from the overlay container's render head (in {@code UIElementRenderMixin}), outside its
 * children iteration so the structural removal cannot {@code ConcurrentModify}.</p>
 *
 * <p>Built on the animation core and gated on {@link Animations#enabled()}. All access is from the render
 * thread, so no synchronization is needed.</p>
 */
public final class OverlayReveal
{
    /** How long the appear/close animation lasts. */
    public static final long DURATION_MS = 240L;
    /** How far below its resting position the panel sits when hidden, in pixels (slides up / down by this). */
    public static final float SLIDE_PX = 14F;

    /**
     * Overlays/panels currently animating, keyed by identity. Both an overlay and its panel(s) map to the
     * same {@link Reveal}. Detached (abandoned) entries are swept on the next {@link #arm}/{@link #armClose}.
     */
    private static final Map<UIElement, Reveal> active = new IdentityHashMap<>();

    /**
     * Alpha to apply to icon draws while rendering inside an animating overlay panel (1 = no fade). Icons do
     * <em>not</em> honour the panel's shader-colour fade in BBS's textured draw path (unlike boxes and text),
     * so they are faded explicitly via their vertex colour — {@code Batcher2DIconFilterMixin} multiplies the
     * icon colour by this while {@code UIOverlayPanelMixin} has it set for the panel currently rendering.
     */
    private static float iconAlpha = 1F;

    private OverlayReveal()
    {}

    /** Begin fading icons drawn by the overlay panel now rendering to {@code alpha}. Paired with {@link #endIconFade}. */
    public static void beginIconFade(float alpha)
    {
        iconAlpha = alpha;
    }

    /** Stop fading icons (back to opaque) once the overlay panel has finished rendering. */
    public static void endIconFade()
    {
        iconAlpha = 1F;
    }

    /** Current icon fade alpha (1 = no fade); see {@link #iconAlpha}. */
    public static float iconAlpha()
    {
        return iconAlpha;
    }

    /** Arm an appear reveal shared by an overlay and its panel. No-op while animations are disabled. */
    public static void arm(UIElement overlay, UIElement panel)
    {
        if (!Animations.enabled())
        {
            return;
        }

        sweep();

        Reveal reveal = new Reveal(Animator.now(), false);

        if (overlay != null)
        {
            active.put(overlay, reveal);
        }

        if (panel != null)
        {
            active.put(panel, reveal);
        }
    }

    /**
     * Arm a close reveal for an overlay (and its panels). The caller has already run the overlay's close
     * logic and cancelled the immediate detach; the overlay stays in the tree until {@link #finishClosed}
     * tears it down. No-op while animations are disabled.
     */
    public static void armClose(UIElement overlay)
    {
        if (overlay == null || !Animations.enabled())
        {
            return;
        }

        sweep();

        Reveal reveal = new Reveal(Animator.now(), true);

        active.put(overlay, reveal);

        for (UIOverlayPanel panel : overlay.getChildren(UIOverlayPanel.class))
        {
            active.put(panel, reveal);
        }
    }

    /** Whether {@code overlay} is currently playing its close animation (so repeat close requests are ignored). */
    public static boolean isClosing(UIElement overlay)
    {
        Reveal reveal = active.get(overlay);

        return reveal != null && reveal.closing;
    }

    /**
     * Visibility 0..1 of an overlay or its panel right now: rising for an appear, falling for a close, and 1
     * (fully shown) when nothing is animating. The panel translates by {@code (1 - vis) * SLIDE_PX} and is
     * drawn at alpha {@code vis}; the backdrop alpha is multiplied by {@code vis}.
     */
    public static float visibility(UIElement element)
    {
        Reveal reveal = reveal(element);

        if (reveal == null)
        {
            return 1F;
        }

        float p = reveal.anim.progress(0L, DURATION_MS, Easings.OUT_CUBIC);

        return reveal.closing ? 1F - p : p;
    }

    /**
     * Detach every overlay under {@code container} whose close animation has finished. Called from the
     * container's render head, before its children are iterated, so removing an overlay cannot corrupt that
     * iteration. No-op until a close finishes.
     */
    public static void finishClosed(UIElement container)
    {
        if (active.isEmpty())
        {
            return;
        }

        List<UIElement> done = null;

        for (IUIElement child : container.getChildren())
        {
            if (child instanceof UIElement overlay)
            {
                Reveal reveal = active.get(overlay);

                if (reveal != null && reveal.closing && reveal.anim.finished(DURATION_MS))
                {
                    if (done == null)
                    {
                        done = new ArrayList<>();
                    }

                    done.add(overlay);
                }
            }
        }

        if (done == null)
        {
            return;
        }

        for (UIElement overlay : done)
        {
            active.remove(overlay);

            for (UIOverlayPanel panel : overlay.getChildren(UIOverlayPanel.class))
            {
                active.remove(panel);
                panel.removeFromParent();
            }

            overlay.removeFromParent();
        }
    }

    /**
     * The active reveal for {@code element}, or null when nothing applies. A finished <em>appear</em> is
     * dropped here so the element renders normally afterwards; a finished <em>close</em> is kept (visibility
     * computes to 0) until {@link #finishClosed} detaches it.
     */
    private static Reveal reveal(UIElement element)
    {
        if (active.isEmpty() || !Animations.enabled())
        {
            return null;
        }

        Reveal reveal = active.get(element);

        if (reveal == null)
        {
            return null;
        }

        if (reveal.anim.finished(DURATION_MS) && !reveal.closing)
        {
            active.remove(element);

            return null;
        }

        return reveal;
    }

    /** Drop entries whose element is no longer in the tree (abandoned closes / screen changes). Safe: a
     * detached element is not rendered, so its visibility is never queried and it cannot revive. */
    private static void sweep()
    {
        if (!active.isEmpty())
        {
            active.keySet().removeIf(element -> !element.hasParent());
        }
    }

    /** One overlay's animation: a start time plus a direction (appear vs close). */
    private static final class Reveal
    {
        final Animator anim;
        final boolean closing;

        Reveal(Animator anim, boolean closing)
        {
            this.anim = anim;
            this.closing = closing;
        }
    }
}
