package org.qualet.refreshedui.client.anim;

/**
 * When the left mouse button last went down in a BBS screen (stamped from {@code UIBaseMenu.mouseClicked}).
 * Lets a render-time effect tell a jump the user clicked for from one that happened on its own — the
 * timeline cursor glides to a click, but follows playback and scrubbing as it always did.
 */
public final class PointerClock
{
    private static long lastLeftPressMs = Long.MIN_VALUE / 2L;

    private PointerClock()
    {}

    public static void leftPressed()
    {
        lastLeftPressMs = Tween.now();
    }

    /** Whether the left button went down within the last {@code ms} milliseconds. */
    public static boolean leftPressedWithin(long ms)
    {
        return Tween.now() - lastLeftPressMs <= ms;
    }
}
