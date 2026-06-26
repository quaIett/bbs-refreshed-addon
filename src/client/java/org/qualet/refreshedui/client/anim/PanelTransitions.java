package org.qualet.refreshedui.client.anim;

import mchorse.bbs_mod.ui.framework.elements.UIElement;

/**
 * Drives the "panel appear" reveal. When the user switches editors — a top-level dashboard panel
 * (taskbar) or a Film sub-editor (camera / replays / actions) — the freshly shown panel is registered
 * here with a fresh {@link Animator}. For a short window afterwards, every piece of text drawn
 * <em>inside that panel's subtree</em> is rendered with a per-letter stagger (see {@code StaggerText} and
 * {@code Batcher2DTextStaggerMixin}); text elsewhere on screen is untouched.
 *
 * <p>Subtree scoping is tracked by {@link #enter(UIElement)} / {@link #exit(UIElement)}, called around
 * {@code UIElement.render}: while the registered root is on the render stack, {@link #insideRoot} is true.
 * The whole mechanism is inert (a couple of reference compares) whenever no transition is armed.</p>
 *
 * <p>All access is from the render thread, so no synchronization is needed.</p>
 */
public final class PanelTransitions
{
    /**
     * How long the reveal lasts. Comfortably longer than the longest realistic per-text stagger
     * (letters * {@code LETTER_STAGGER_MS} + {@code LETTER_DURATION_MS}); after it elapses, text renders
     * normally again.
     */
    private static final long WINDOW_MS = 1500L;

    private static UIElement appearRoot;
    private static Animator appearAnim;
    private static int depth;

    private PanelTransitions()
    {}

    /** Begin a reveal for {@code root}'s subtree, starting now. A null root ends any active reveal. */
    public static void onPanelAppear(UIElement root)
    {
        appearRoot = root;
        appearAnim = root == null ? null : Animator.now();
        depth = 0;
    }

    /** Called at the head of {@code UIElement.render}: note when the appearing root enters the stack. */
    public static void enter(UIElement element)
    {
        if (element == appearRoot)
        {
            depth++;
        }
    }

    /** Called at the return of {@code UIElement.render}: pop the appearing root off the stack. */
    public static void exit(UIElement element)
    {
        if (element == appearRoot && depth > 0)
        {
            depth--;
        }
    }

    /**
     * The reveal animator if a transition is active, still within its window, and the current draw is
     * happening inside the appearing panel's subtree; otherwise null (the caller renders text normally).
     */
    public static Animator activeAnimator()
    {
        if (appearAnim == null)
        {
            return null;
        }

        if (appearAnim.finished(WINDOW_MS))
        {
            appearRoot = null;
            appearAnim = null;
            depth = 0;

            return null;
        }

        return depth > 0 ? appearAnim : null;
    }
}
