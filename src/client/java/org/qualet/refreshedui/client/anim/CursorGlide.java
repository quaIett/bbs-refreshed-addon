package org.qualet.refreshedui.client.anim;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The time cursor of a timeline glides to where it was clicked instead of jumping there.
 *
 * <p>Only a jump that follows a click glides: playback, dragging the cursor along, zooming or panning the
 * view all move the cursor too, and those must stay glued to the value. So a change of the cursor's
 * column starts a glide only when the left button went down a moment ago ({@link PointerClock}); while a
 * glide runs its end point is the live column, so a click that turns into a drag catches up with the
 * pointer smoothly instead of lagging behind it.</p>
 *
 * <p>Keyed by the canvas area the cursor is drawn over — each timeline (clips, keyframes, audio) passes
 * its own — held weakly so a closed editor lets go of its entry. There are only ever a handful.</p>
 */
public final class CursorGlide
{
    public static final long DURATION_MS = 140L;
    /** A click only counts as the cause of a jump seen this soon after it. */
    private static final long CLICK_WINDOW_MS = 150L;
    /** Jumps shorter than this (in pixels) are not worth gliding. */
    private static final int MIN_DISTANCE = 3;

    /* By identity, not equals: an Area compares by its rectangle, and two timelines can sit on equal ones */
    private static final List<State> states = new ArrayList<>();

    private CursorGlide()
    {}

    /** The column to draw the cursor at this frame for a cursor whose real column is {@code x}. */
    public static int display(Object canvas, int x)
    {
        if (!Animations.enabled())
        {
            return x;
        }

        State state = find(canvas);
        long now = Tween.now();

        if (state == null)
        {
            states.add(new State(canvas, x));

            return x;
        }

        if (x != state.target)
        {
            float shown = state.shown(now);

            if (PointerClock.leftPressedWithin(CLICK_WINDOW_MS) && Math.abs(x - shown) >= MIN_DISTANCE && !state.gliding(now))
            {
                state.from = shown;
                state.start = now;
            }

            state.target = x;
        }

        return Math.round(state.shown(now));
    }

    private static State find(Object canvas)
    {
        State found = null;
        Iterator<State> it = states.iterator();

        while (it.hasNext())
        {
            State state = it.next();
            Object key = state.canvas.get();

            if (key == null)
            {
                it.remove();
            }
            else if (key == canvas)
            {
                found = state;
            }
        }

        return found;
    }

    private static final class State
    {
        final WeakReference<Object> canvas;
        float from;
        int target;
        long start = Long.MIN_VALUE / 2L;

        State(Object canvas, int x)
        {
            this.canvas = new WeakReference<>(canvas);
            this.from = this.target = x;
        }

        boolean gliding(long now)
        {
            return now - this.start < Animations.ms(DURATION_MS);
        }

        float shown(long now)
        {
            float t = (now - this.start) / (float) Animations.ms(DURATION_MS);

            if (t >= 1F)
            {
                return this.target;
            }

            return this.from + (this.target - this.from) * Easings.outCubic(Math.max(0F, t));
        }
    }
}
