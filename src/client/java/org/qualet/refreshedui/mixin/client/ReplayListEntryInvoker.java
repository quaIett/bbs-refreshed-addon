package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.replays.ReplayListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@link ReplayListEntry}'s private constructor so the nested-folder list can build FOLDER rows
 * with a non-zero indent. Upstream's public {@code ReplayListEntry.folder(String)} factory hardcodes
 * indent 0 (flat folders), and the constructor is private — this {@code @Invoker} static factory lets the
 * subclass pass the per-depth indent.
 */
@Mixin(ReplayListEntry.class)
public interface ReplayListEntryInvoker
{
    @Invoker("<init>")
    static ReplayListEntry refreshed$create(ReplayListEntry.Kind kind, String folderName, Replay replay, int indent)
    {
        throw new AssertionError();
    }
}
