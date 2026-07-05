package org.qualet.refreshedui.mixin;

import mchorse.bbs_mod.film.replays.Replay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Makes {@link Replay#normalizeCategory(String)} nested-aware.
 *
 * <p>Upstream 2.3.1's implementation is single-level: it truncates a category at the first {@code /}, so
 * only flat folders are possible. This {@code @Overwrite} replaces it with the fork's nested normalize
 * (trim, {@code \\}->{@code /}, drop empty segments, KEEP inner {@code /} separators). Overwriting the
 * static makes ALL upstream call sites — including the private ones inside base {@code UIReplayList}
 * (e.g. {@code handleSwap} -> {@code assignReplayCategoryValue}) — preserve nested paths.</p>
 */
@Mixin(Replay.class)
public abstract class ReplayMixin
{
    /**
     * @author refreshedui
     * @reason Replace single-level (truncate-at-first-slash) category normalization with nested-path
     *         normalization so replays can live in {@code A/B/C} folders.
     */
    @Overwrite
    public static String normalizeCategory(String raw)
    {
        if (raw == null)
        {
            return "";
        }

        String s = raw.replace('\\', '/').trim();

        if (s.isEmpty())
        {
            return "";
        }

        StringBuilder out = new StringBuilder();
        int len = s.length();
        int i = 0;

        while (i < len)
        {
            while (i < len && s.charAt(i) == '/')
            {
                i += 1;
            }

            if (i >= len)
            {
                break;
            }

            int start = i;

            while (i < len && s.charAt(i) != '/')
            {
                i += 1;
            }

            String part = s.substring(start, i).trim();

            if (part.isEmpty())
            {
                continue;
            }

            if (out.length() > 0)
            {
                out.append('/');
            }

            out.append(part);
        }

        return out.toString();
    }
}
