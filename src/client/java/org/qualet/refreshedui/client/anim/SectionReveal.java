package org.qualet.refreshedui.client.anim;

import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UISection;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Drives the collapsible {@code UISection} unfold/fold animation. When a section expands or collapses, its
 * body (the {@code fields} element) is armed here with a fresh {@link Animator}; for a short window
 * afterwards the body's direct children — the rows — animate one after another, and the card background
 * grows or shrinks to follow them.
 *
 * <p><b>Expand</b>: rows cascade in top-to-bottom — each wipes in (clipped to a window growing from its
 * top), slides a few pixels into place and fades from transparent, staggered by index. <b>Collapse</b> is
 * the reverse: rows retract bottom-to-top, and only once the animation finishes is the body actually
 * detached from the tree (BBS removes it instantly otherwise). The reserved space is held for the whole
 * animation, so sibling sections do not move until the collapse completes, then settle in one step.</p>
 *
 * <p>The animation is applied by wrapping each child's {@code render} call inside the body's render loop (a
 * {@code @Redirect} in {@code UISectionBodyRevealMixin}), not by hooking each element's own render — many
 * widgets (trackpads, toggles, buttons) override {@code render} without calling {@code super}, so a per-row
 * inject would miss their own drawing. The background growth is in {@code UISectionMixin}; the deferred
 * detach is finalized from that same mixin's render head ({@link #finishCollapseIfDone}) — outside the
 * children iteration, so removing the body cannot corrupt it. All access is from the render thread.</p>
 */
public final class SectionReveal
{
    /** Per-row reveal duration. */
    public static final long ROW_DURATION_MS = 94L;
    /** Delay between consecutive rows starting — the "row by row" cascade. */
    public static final long ROW_STAGGER_MS = 19L;
    /** How far a row starts from its resting position, in pixels, while it slides into / out of place. */
    public static final float SLIDE_PX = 7F;

    /** Bodies currently animating, keyed by identity. Finished expands are dropped lazily in {@link #reveal}. */
    private static final Map<UIElement, Reveal> active = new IdentityHashMap<>();

    private SectionReveal()
    {}

    /** Total time until the last of {@code rows} rows has fully settled (same for expand and collapse). */
    public static long totalMs(int rows)
    {
        if (rows <= 0)
        {
            return ROW_DURATION_MS;
        }

        return (long) (rows - 1) * ROW_STAGGER_MS + ROW_DURATION_MS;
    }

    /** Arm an expand reveal for a section body. No-op while animations are disabled. */
    public static void onExpand(UIElement body)
    {
        if (body == null || !Animations.enabled())
        {
            return;
        }

        active.put(body, new Reveal(Animator.now(), false));
    }

    /**
     * Arm a collapse reveal for a section body. No-op while animations are disabled (the caller then lets
     * BBS detach the body immediately, as usual). When armed, the body is kept in the tree and detached only
     * once {@link #finishCollapseIfDone} sees the animation through.
     */
    public static boolean onCollapse(UIElement body)
    {
        if (body == null || !Animations.enabled())
        {
            return false;
        }

        active.put(body, new Reveal(Animator.now(), true));

        return true;
    }

    /** Whether {@code body} is currently kept alive by a deferred collapse (so a re-expand must not re-add it). */
    public static boolean isCollapsing(UIElement body)
    {
        Reveal r = active.get(body);

        return r != null && r.collapsing;
    }

    /**
     * Finalize a deferred collapse once its animation is done: detach the body for real and reflow the
     * section's container so the sections below settle. Called from the section's render head, outside the
     * children iteration, so the structural change is safe. No-op until the collapse finishes.
     */
    public static void finishCollapseIfDone(UIElement body)
    {
        if (body == null || active.isEmpty())
        {
            return;
        }

        Reveal r = active.get(body);

        if (r == null || !r.collapsing || !r.anim.finished(totalMs(body.getChildren().size())))
        {
            return;
        }

        UIElement section = body.getParent();

        active.remove(body);
        body.removeFromParent();

        if (section != null && section.getParent() != null)
        {
            section.getParent().resize();
        }
    }

    /**
     * The active reveal for {@code body}, or null if none applies. A finished <em>expand</em> is dropped
     * here so settled rows render normally; a finished <em>collapse</em> is left for
     * {@link #finishCollapseIfDone} to tear down on the render head.
     */
    public static Reveal reveal(UIElement body)
    {
        if (active.isEmpty() || !Animations.enabled())
        {
            return null;
        }

        Reveal r = active.get(body);

        if (r == null)
        {
            return null;
        }

        if (r.anim.finished(totalMs(body.getChildren().size())))
        {
            if (!r.collapsing)
            {
                active.remove(body);
            }

            return null;
        }

        return r;
    }

    /**
     * The animated bottom edge for a section's background while its body is animating. The card grows (or,
     * on collapse, shrinks) in step with how much of the rows is revealed. {@code fullBottom} is the section
     * block's resting bottom; returns it when no animation is active.
     *
     * <p>The edge is driven by the <em>fraction of total row height revealed</em> (Σ&nbsp;h·vis / Σ&nbsp;h),
     * mapped onto the body's span — a continuous quantity. An earlier version took the max of each row's
     * revealed bottom, which jumped by a whole row's height the instant the next (lower) row's stagger
     * kicked in, since a just-started row contributes its top edge well below the current front. The
     * proportional fill has no such discontinuity, so neither the card edge nor the pushed sibling jerks.</p>
     */
    public static float bgBottom(UIElement body, float top, float fullBottom)
    {
        Reveal r = reveal(body);

        if (r == null)
        {
            return fullBottom;
        }

        int count = body.getChildren().size();
        float revealed = 0F;
        float total = 0F;
        int i = 0;

        for (IUIElement child : body.getChildren())
        {
            if (child instanceof UIElement row)
            {
                revealed += row.area.h * r.visibility(i, count);
                total += row.area.h;
            }

            i++;
        }

        float frac = total > 0F ? revealed / total : 1F;

        /* Floor at the body's top so the header always sits on the card; fill down to fullBottom. */
        return body.area.y + (fullBottom - body.area.y) * frac;
    }

    /**
     * Total reserved-but-not-yet-revealed height of every animating section that sits before {@code stop}
     * among {@code parent}'s children. The caller translates {@code stop} (and everything after it) up by
     * this much, so siblings track the visible edge of an unfolding/folding section instead of jumping: on
     * expand they start tucked under the header and slide down to rest; on collapse they slide up, and when
     * the fold finishes and the real layout shrinks, the offset is already zero — no snap.
     */
    public static float precedingMissing(UIElement parent, IUIElement stop)
    {
        if (active.isEmpty())
        {
            return 0F;
        }

        float sum = 0F;

        for (IUIElement child : parent.getChildren())
        {
            if (child == stop)
            {
                break;
            }

            if (child instanceof UISection section)
            {
                sum += missingHeight(section);
            }
        }

        return sum;
    }

    /** How much of a section's reserved height is not yet revealed (0 when it is not animating). */
    private static float missingHeight(UISection section)
    {
        if (active.get(section.fields) == null)
        {
            return 0F;
        }

        float full = section.area.ey();

        return full - bgBottom(section.fields, section.area.y, full);
    }

    /**
     * One section body's animation: a start time plus a direction. A single {@link Animator} feeds every
     * row via {@link #visibility(int, int)}, which maps elapsed time to a 0..1 factor (0 hidden, 1 fully
     * shown) — rising for an expand, falling for a collapse, staggered per row index.
     */
    public static final class Reveal
    {
        final Animator anim;
        final boolean collapsing;

        Reveal(Animator anim, boolean collapsing)
        {
            this.anim = anim;
            this.collapsing = collapsing;
        }

        /** Visibility 0..1 of row {@code index} (of {@code count}) right now. */
        public float visibility(int index, int count)
        {
            if (!this.collapsing)
            {
                return this.anim.progress((long) index * ROW_STAGGER_MS, ROW_DURATION_MS, Easings.OUT_CUBIC);
            }

            /* Collapse retracts bottom-to-top: the last row starts receding first. */
            long delay = (long) (count - 1 - index) * ROW_STAGGER_MS;

            return 1F - this.anim.progress(delay, ROW_DURATION_MS, Easings.OUT_CUBIC);
        }
    }
}
