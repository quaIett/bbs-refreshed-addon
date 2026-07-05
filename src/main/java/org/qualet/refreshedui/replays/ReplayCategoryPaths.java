package org.qualet.refreshedui.replays;

import mchorse.bbs_mod.film.replays.Replay;

/**
 * Nested-folder path algebra for replay categories, ported from the bbs-refreshed fork's static helpers
 * on {@code Replay} (which upstream 2.3.1 lacks — its {@code Replay.normalizeCategory} truncates at the
 * first {@code /}). Kept as a plain addon utility instead of mixed into {@code Replay} because these are
 * new self-contained helpers with no need to touch {@code Replay} state.
 *
 * <p>All methods route through {@link Replay#normalizeCategory(String)}, which the addon's
 * {@code ReplayMixin} {@code @Overwrite}s to be nested-aware (trim, {@code \\}->{@code /}, drop empty
 * segments, keep {@code /} separators). So every call site — including the private ones inside base
 * {@code UIReplayList} — sees consistent nested paths.</p>
 */
public final class ReplayCategoryPaths
{
    /** Number of path segments ({@code ""}=0, {@code "A"}=1, {@code "A/B"}=2). */
    public static int categoryDepth(String category)
    {
        String c = Replay.normalizeCategory(category);

        if (c.isEmpty())
        {
            return 0;
        }

        int depth = 1;

        for (int i = 0, len = c.length(); i < len; i++)
        {
            if (c.charAt(i) == '/')
            {
                depth += 1;
            }
        }

        return depth;
    }

    /** Parent path ({@code "A/B/C"} -> {@code "A/B"}, {@code "A"} -> {@code ""}). */
    public static String categoryParent(String category)
    {
        String c = Replay.normalizeCategory(category);

        if (c.isEmpty())
        {
            return "";
        }

        int lastSlash = c.lastIndexOf('/');

        return lastSlash < 0 ? "" : c.substring(0, lastSlash);
    }

    /** Leaf segment ({@code "A/B/C"} -> {@code "C"}). */
    public static String categoryLeaf(String category)
    {
        String c = Replay.normalizeCategory(category);

        if (c.isEmpty())
        {
            return "";
        }

        int lastSlash = c.lastIndexOf('/');

        return lastSlash < 0 ? c : c.substring(lastSlash + 1);
    }

    /** True if {@code candidate} equals {@code category} or is nested under it (root matches everything). */
    public static boolean isSameOrChildCategory(String category, String candidate)
    {
        String parent = Replay.normalizeCategory(category);
        String c = Replay.normalizeCategory(candidate);

        if (parent.isEmpty())
        {
            return true;
        }

        if (c.equals(parent))
        {
            return true;
        }

        return c.startsWith(parent + "/");
    }

    /**
     * Rewrite the {@code fromPrefix} of {@code category} to {@code toPrefix} (moving/renaming a subtree).
     * Leaves paths outside the prefix untouched.
     */
    public static String rewriteCategoryPrefix(String category, String fromPrefix, String toPrefix)
    {
        String c = Replay.normalizeCategory(category);
        String from = Replay.normalizeCategory(fromPrefix);
        String to = Replay.normalizeCategory(toPrefix);

        if (from.isEmpty())
        {
            return c;
        }

        if (c.equals(from))
        {
            return to;
        }

        String fromWithSlash = from + "/";

        if (!c.startsWith(fromWithSlash))
        {
            return c;
        }

        String suffix = c.substring(fromWithSlash.length());

        if (to.isEmpty())
        {
            return suffix;
        }

        return to + "/" + suffix;
    }

    private ReplayCategoryPaths()
    {}
}
