package org.qualet.refreshedui.client.replays;

/**
 * Bridge so a mixin on the base {@code UIReplayList} can route its (flat) private context-menu folder
 * removal into the nested {@link UIReplayListRefreshed} implementation without duplicating the menu item.
 * See {@code UIReplayListMixin}: when the live instance is one of ours it cancels the base flat removal and
 * calls {@link #refreshed$removeFolder(String)} (lift-contents-to-parent) instead.
 */
public interface INestedFolderList
{
    void refreshed$removeFolder(String normalizedCategory);

    /**
     * Nested "Add folder" overlay (persists folder order + moves the current selection — replays AND
     * selected folder rows — into the new folder). Routed from the base flat {@code openAddCategoryOverlay}.
     */
    void refreshed$openAddCategory();
}
