package org.qualet.refreshedui.replays;

/**
 * Accessor implemented on {@code Film} by {@code FilmMixin} to expose the addon-added persisted folder
 * order ({@code replay_category_order}). The nested-folder replay list reads/writes it via
 * {@code ((IReplayFolderOrder) (Object) film).refreshed$getReplayCategoryOrder()}.
 */
public interface IReplayFolderOrder
{
    ValueStringListAddon refreshed$getReplayCategoryOrder();
}
