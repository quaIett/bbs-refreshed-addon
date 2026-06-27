package org.qualet.refreshedui.client.ui;

/**
 * Render-thread scratch for grouped selection rounding in context menus. {@code ContextAction}'s
 * {@code renderBackground} has no access to its list index or neighbours, so the list-level mixin
 * ({@code UIActionListMixin}) stamps the merge flags here just before each entry draws, and the entry's
 * frame mixin reads them. Single-threaded UI rendering, so a plain static pair is enough.
 *
 * <p>{@code top}/{@code bottom} mean "this entry merges with the neighbour on that side" — i.e. that edge
 * should be squared and its border hidden so the run reads as one block.</p>
 */
public final class SelectionMerge
{
    private static boolean top;
    private static boolean bottom;

    public static void set(boolean mergeTop, boolean mergeBottom)
    {
        top = mergeTop;
        bottom = mergeBottom;
    }

    public static void clear()
    {
        top = false;
        bottom = false;
    }

    public static boolean top()
    {
        return top;
    }

    public static boolean bottom()
    {
        return bottom;
    }

    private SelectionMerge()
    {}
}
