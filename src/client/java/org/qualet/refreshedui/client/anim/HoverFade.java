package org.qualet.refreshedui.client.anim;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Row hover washes come up quickly and die down gently instead of blinking on and off: running the
 * pointer down a list leaves a short fading trail rather than a strobe.
 *
 * <p>A row asks for its hover {@link #level} every frame it is drawn, hovered or not, and paints its wash
 * at that strength. Rows are told apart by an (owner, key) pair compared by identity — a list and its row
 * object, a menu entry — or, for painters that only know a rectangle, by {@link #levelRect}. Only rows
 * with a non-zero level are remembered, so a list at rest costs a map lookup per visible row.</p>
 *
 * <p>A rectangle is not a row: when a list scrolls its rows' rectangles change every frame, and the row
 * under a resting pointer would start its fade over each time. So a rectangle that is hovered for the
 * first time inherits the level of the one hovered a frame ago in the same column (same x, width and
 * height) — the same row, moved.</p>
 */
public final class HoverFade
{
    public static final long IN_MS = 70L;
    public static final long OUT_MS = 220L;

    /** A row not drawn for this long starts over — it was scrolled away, or its list was hidden. */
    private static final long STALE_MS = 250L;
    private static final long PRUNE_EVERY_MS = 2000L;

    /** Owner of context-menu entries: each entry is its own object, so one owner covers them all. */
    public static final Object MENU = new Object();

    private static final Object RECT = new Object();

    private static final Map<Key, State> states = new HashMap<>();
    private static final Key probe = new Key();
    private static long lastPrune;

    /* The rectangle hovered most recently, for the scroll hand-over */
    private static int lastX;
    private static int lastW;
    private static int lastH;
    private static float lastLevel;
    private static long lastHoverMs = Long.MIN_VALUE / 2L;

    private HoverFade()
    {}

    /** Hover strength 0..1 of the row {@code key} of {@code owner} this frame. */
    public static float level(Object owner, Object key, boolean hover)
    {
        return level(owner, key, 0L, hover, -1F);
    }

    /** The same for a row known only by its rectangle. */
    public static float levelRect(int x, int y, int w, int h, boolean hover)
    {
        long rect = ((long) (x & 0xFFFF) << 48) | ((long) (y & 0xFFFF) << 32) | ((long) (w & 0xFFFF) << 16) | (h & 0xFFFF);
        float inherit = -1F;

        if (hover && Tween.now() - lastHoverMs <= 50L && lastX == x && lastW == w && lastH == h)
        {
            inherit = lastLevel;
        }

        float level = level(RECT, null, rect, hover, inherit);

        if (hover)
        {
            lastX = x;
            lastW = w;
            lastH = h;
            lastLevel = level;
            lastHoverMs = Tween.now();
        }

        return level;
    }

    /** A row was rebuilt as a new object: let the new one carry on where the old one was. */
    public static void transfer(Object owner, Object from, Object to)
    {
        if (from == to || states.isEmpty())
        {
            return;
        }

        probe.set(owner, from, 0L);

        State state = states.remove(probe);

        if (state != null)
        {
            states.put(new Key().set(owner, to, 0L), state);
        }
    }

    private static float level(Object owner, Object key, long rect, boolean hover, float inherit)
    {
        if (!Animations.enabled())
        {
            return hover ? 1F : 0F;
        }

        long now = Tween.now();

        probe.set(owner, key, rect);

        State state = states.get(probe);

        if (state != null && now - state.updated > STALE_MS)
        {
            states.remove(probe);
            state = null;
        }

        if (state == null)
        {
            if (!hover)
            {
                return 0F;
            }

            state = new State();
            state.level = inherit >= 0F ? inherit : 0F;
            state.updated = now;
            states.put(new Key().set(owner, key, rect), state);
        }

        long dt = now - state.updated;

        state.updated = now;
        state.level = hover
            ? Math.min(1F, state.level + dt / (float) IN_MS)
            : Math.max(0F, state.level - dt / (float) OUT_MS);

        if (state.level <= 0F && !hover)
        {
            states.remove(probe);
        }

        prune(now);

        return Easings.outCubic(state.level);
    }

    private static void prune(long now)
    {
        if (now - lastPrune < PRUNE_EVERY_MS)
        {
            return;
        }

        lastPrune = now;

        Iterator<State> it = states.values().iterator();

        while (it.hasNext())
        {
            if (now - it.next().updated > STALE_MS)
            {
                it.remove();
            }
        }
    }

    private static final class State
    {
        float level;
        long updated;
    }

    private static final class Key
    {
        Object owner;
        Object key;
        long rect;

        Key set(Object owner, Object key, long rect)
        {
            this.owner = owner;
            this.key = key;
            this.rect = rect;

            return this;
        }

        @Override
        public boolean equals(Object o)
        {
            return o instanceof Key other && other.owner == this.owner && other.key == this.key && other.rect == this.rect;
        }

        @Override
        public int hashCode()
        {
            return System.identityHashCode(this.owner) * 31 + System.identityHashCode(this.key) * 17 + Long.hashCode(this.rect);
        }
    }
}
