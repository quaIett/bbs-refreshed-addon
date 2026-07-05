package org.qualet.refreshedui.client.replays;

import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;

/**
 * Localized keys the addon adds for nested-folder actions upstream BBS 2.3.1 does not have ("Move folder
 * to…", "Rename folder…" + the rename overlay strings). Content is registered at runtime in
 * {@code RefreshedUiStrings} (the addon ships no lang JSON); {@link L10n#lang(String)} resolves each key
 * against the live string map at render time. All other replay-folder labels reuse upstream {@code UIKeys}.
 */
public final class ReplayFolderKeys
{
    public static final String PREFIX = "bbs.ui.scene.replays.refreshed.";

    public static final IKey MOVE_FOLDER_TO_CATEGORY = L10n.lang(PREFIX + "move_folder_to_category");
    public static final IKey RENAME_FOLDER = L10n.lang(PREFIX + "rename_folder");
    public static final IKey RENAME_FOLDER_TITLE = L10n.lang(PREFIX + "rename_folder.title");
    public static final IKey RENAME_FOLDER_DESCRIPTION = L10n.lang(PREFIX + "rename_folder.description");
    public static final IKey RENAME_FOLDER_PLACEHOLDER = L10n.lang(PREFIX + "rename_folder.placeholder");

    private ReplayFolderKeys()
    {}
}
