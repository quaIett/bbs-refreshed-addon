package org.qualet.refreshedui.client.replays;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.ReplayListEntry;
import mchorse.bbs_mod.ui.film.replays.UIReplayList;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.NaturalOrderComparator;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.qualet.refreshedui.mixin.client.ReplayListEntryInvoker;
import org.qualet.refreshedui.replays.IReplayFolderOrder;
import org.qualet.refreshedui.replays.RefreshedTextUtils;
import org.qualet.refreshedui.replays.ReplayCategoryPaths;
import org.qualet.refreshedui.replays.ValueStringListAddon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Nested-folder replay list. Extends BBS's {@link UIReplayList} (which ships a FLAT single-level folder
 * system in 2.3.1) to support {@code A/B/C} nested paths, per-depth indentation with tree guides, a rotated
 * disclosure icon, folder drag&drop (reorder / reparent / drop-to-root), a persisted folder order, colored
 * folder names via {@code [c} markup, folder rename, and a drop-target highlight on folder rows.
 *
 * <h3>Why a subclass (not a pile of {@code @Overwrite}s)</h3>
 * Upstream keeps its category state private ({@code collapsedCategories}, {@code contextFolderCategoryName},
 * etc.), so this class re-owns that state and overrides the list-build, render, and mouse hooks. Substituted
 * for the base at both {@code new UIReplayList(...)} call sites via {@code @Redirect}-on-NEW mixins.
 *
 * <h3>Context menu</h3>
 * Base {@code UIReplayList} builds its full menu via {@code this.context((menu)->...)}. That overload
 * APPENDS the consumer to a list (it does not replace), and many of its actions ({@code processReplays},
 * {@code offsetTimeReplays}, {@code openRandomTexturesOverlay}, {@code pasteToReplays}, ...) are private and
 * unreachable from a subclass. So instead of re-registering the whole menu (which would force re-owning all
 * those private actions), we KEEP the base menu intact and only APPEND folder-only actions (Move folder to,
 * Rename folder, Remove folder) via a second {@code this.context(...)} consumer. {@link #createContextMenu}
 * sets our own folder-name state before delegating to {@code super}, so both the base consumer (which reads
 * the base's own folder-name field it sets itself) and ours run against the same menu with no duplication.
 * Trade-off: our folder actions render at the BOTTOM of the menu rather than interleaved with the base's
 * folder branch — acceptable, and keeps every upstream item working.
 */
public class UIReplayListRefreshed extends UIReplayList implements INestedFolderList
{
    /** Indent unit per folder depth level, px. Shared by the tree build AND guide rendering. */
    private static final int FOLDER_INDENT_UNIT = 16;

    private final Consumer<Form> refreshedFormConsumer;

    /** Category names whose replay rows are hidden (headers stay visible). Our own copy. */
    private final Set<String> refreshedCollapsedCategories = new HashSet<>();

    /** Folder category name under the cursor while building the context menu (our own copy). */
    private String refreshedContextFolderCategoryName;

    /** Folder row index that should toggle collapse on release (unless the click turned into a drag). */
    private int refreshedPendingFolderToggleIndex = -1;

    /** Last film we refreshed against — collapse state is cleared when the film identity changes. */
    private Film refreshedLastFilm;

    public UIReplayListRefreshed(Consumer<List<Replay>> callback, Consumer<Form> formConsumer, UIFilmPanel panel)
    {
        super(callback, formConsumer, panel);

        this.refreshedFormConsumer = formConsumer;

        /*
         * Append a SECOND context consumer for folder-only actions. Runs alongside (not instead of) the base
         * menu consumer, so all upstream items stay and these are added at the end.
         */
        this.context((menu) ->
        {
            Film film = this.panel.getData();

            if (film != null && this.refreshedContextFolderCategoryName != null)
            {
                String cat = this.refreshedContextFolderCategoryName;

                menu.action(Icons.SHIFT_TO, ReplayFolderKeys.MOVE_FOLDER_TO_CATEGORY, () -> this.openMoveFolderToCategoryContextMenu(cat));
                menu.action(Icons.EDIT, ReplayFolderKeys.RENAME_FOLDER, () -> this.openRenameFolderOverlay(cat));
            }
        });
    }

    /* ---------------------------------------------------------------------------------------------------- */
    /* Context menu                                                                                          */
    /* ---------------------------------------------------------------------------------------------------- */

    @Override
    public UIContextMenu createContextMenu(UIContext context)
    {
        this.refreshedContextFolderCategoryName = null;

        int idx = this.getIndexAtCursor(context);

        if (this.exists(idx))
        {
            ReplayListEntry e = this.list.get(idx);

            if (e.isFolder())
            {
                String cat = Replay.normalizeCategory(e.folderName);

                if (!cat.isEmpty())
                {
                    this.refreshedContextFolderCategoryName = cat;
                }
            }
        }

        try
        {
            /* super sets its own folder-name field and runs both context consumers (base + ours). */
            return super.createContextMenu(context);
        }
        finally
        {
            this.refreshedContextFolderCategoryName = null;
        }
    }

    /* ---------------------------------------------------------------------------------------------------- */
    /* Folder order accessor                                                                                 */
    /* ---------------------------------------------------------------------------------------------------- */

    private static ValueStringListAddon orderValue(Film film)
    {
        return ((IReplayFolderOrder) (Object) film).refreshed$getReplayCategoryOrder();
    }

    private static List<String> orderList(Film film)
    {
        ValueStringListAddon order = orderValue(film);

        return order != null ? order.get() : new ArrayList<>();
    }

    private static void setOrderList(Film film, List<String> values)
    {
        ValueStringListAddon order = orderValue(film);

        if (order != null)
        {
            order.set(values);
        }
    }

    /* ---------------------------------------------------------------------------------------------------- */
    /* List build                                                                                            */
    /* ---------------------------------------------------------------------------------------------------- */

    @Override
    public void refreshReplayList()
    {
        /* List identity changes here — any drag armed before this rebuild refers to stale indices. */
        this.dragging = -1;
        this.refreshedPendingFolderToggleIndex = -1;

        Film film = this.panel.getData();

        if (film == null)
        {
            this.clear();
            this.refreshedLastFilm = null;

            return;
        }

        /* Collapse state is per-film — clear it when the film instance changes (identity compare). */
        if (film != this.refreshedLastFilm)
        {
            this.refreshedCollapsedCategories.clear();
            this.refreshedLastFilm = film;
        }

        List<String> folders = this.collectCategoryNames(film);

        this.refreshedCollapsedCategories.removeIf((name) -> !folders.contains(name));

        List<Replay> all = film.replays.getList();
        Map<String, List<Replay>> byFolder = new HashMap<>();
        List<Replay> root = new ArrayList<>();

        for (Replay r : all)
        {
            String cat = Replay.normalizeCategory(r.category.get());

            if (cat.isEmpty())
            {
                root.add(r);
            }
            else
            {
                byFolder.computeIfAbsent(cat, (k) -> new ArrayList<>()).add(r);
            }
        }

        List<ReplayListEntry> entries = new ArrayList<>();

        for (String folder : folders)
        {
            if (folder.isEmpty())
            {
                continue;
            }

            if (this.isChildOfCollapsedFolder(folder))
            {
                continue;
            }

            int depth = ReplayCategoryPaths.categoryDepth(folder);
            int folderIndent = Math.max(0, (depth - 1) * FOLDER_INDENT_UNIT);

            entries.add(ReplayListEntryInvoker.refreshed$create(ReplayListEntry.Kind.FOLDER, folder, null, folderIndent));

            if (!this.refreshedCollapsedCategories.contains(folder))
            {
                List<Replay> direct = byFolder.get(folder);

                if (direct != null && !direct.isEmpty())
                {
                    int replayIndent = folderIndent + FOLDER_INDENT_UNIT;

                    for (Replay r : direct)
                    {
                        entries.add(ReplayListEntry.replay(r, replayIndent));
                    }
                }
            }
        }

        for (Replay r : root)
        {
            entries.add(ReplayListEntry.replay(r));
        }

        this.setList(entries);
    }

    /**
     * All folder names (explicit empty folders + names used by replays + implied ancestors), ordered:
     * the film's persisted {@code replay_category_order} first, then remaining names in natural order.
     */
    private List<String> collectCategoryNames(Film film)
    {
        LinkedHashSet<String> present = new LinkedHashSet<>();

        for (String s : film.replayCategoryNames.get())
        {
            String c = Replay.normalizeCategory(s);

            if (!c.isEmpty())
            {
                this.addCategoryWithParents(present, c);
            }
        }

        for (Replay r : film.replays.getList())
        {
            String c = Replay.normalizeCategory(r.category.get());

            if (!c.isEmpty())
            {
                this.addCategoryWithParents(present, c);
            }
        }

        List<String> ordered = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();

        /* 1) User-defined order first */
        for (String s : orderList(film))
        {
            String c = Replay.normalizeCategory(s);

            if (c.isEmpty() || !present.contains(c) || !seen.add(c))
            {
                continue;
            }

            ordered.add(c);
        }

        /* 2) Append remaining categories in natural order */
        TreeSet<String> rest = new TreeSet<>((a, b) -> NaturalOrderComparator.compare(true, a, b));

        for (String c : present)
        {
            if (!seen.contains(c))
            {
                rest.add(c);
            }
        }

        ordered.addAll(rest);

        return ordered;
    }

    private void addCategoryWithParents(Set<String> categories, String category)
    {
        String c = Replay.normalizeCategory(category);

        while (!c.isEmpty())
        {
            categories.add(c);
            c = ReplayCategoryPaths.categoryParent(c);
        }
    }

    private boolean isChildOfCollapsedFolder(String folder)
    {
        String p = ReplayCategoryPaths.categoryParent(folder);

        while (!p.isEmpty())
        {
            if (this.refreshedCollapsedCategories.contains(p))
            {
                return true;
            }

            p = ReplayCategoryPaths.categoryParent(p);
        }

        return false;
    }

    /* ---------------------------------------------------------------------------------------------------- */
    /* Selection / scroll                                                                                    */
    /* ---------------------------------------------------------------------------------------------------- */

    @Override
    public void scrollToReplay(Replay replay)
    {
        if (replay == null)
        {
            return;
        }

        this.uncollapseAncestors(Replay.normalizeCategory(replay.category.get()));

        this.refreshReplayList();

        for (int i = 0; i < this.list.size(); i++)
        {
            ReplayListEntry e = this.list.get(i);

            if (e.isReplay() && e.replay == replay)
            {
                this.pick(i);
                this.scroll.setScroll(i * this.scroll.scrollItemSize);

                return;
            }
        }
    }

    /** Uncollapse the category and ALL its ancestors in our collapse set. */
    private void uncollapseAncestors(String category)
    {
        String p = Replay.normalizeCategory(category);

        while (!p.isEmpty())
        {
            this.refreshedCollapsedCategories.remove(p);
            p = ReplayCategoryPaths.categoryParent(p);
        }
    }

    private void restoreReplaySelection(List<Replay> replays)
    {
        this.current.clear();

        for (Replay r : replays)
        {
            for (int i = 0; i < this.list.size(); i++)
            {
                ReplayListEntry e = this.list.get(i);

                if (e.isReplay() && e.replay == r)
                {
                    this.addIndex(i);

                    break;
                }
            }
        }

        if (this.callback != null && !this.current.isEmpty())
        {
            this.callback.accept(this.getCurrent());
        }
    }

    /* ---------------------------------------------------------------------------------------------------- */
    /* Shared subtree-prefix rewrite (used by remove / move / rename)                                        */
    /* ---------------------------------------------------------------------------------------------------- */

    /**
     * Single place for the "rewrite a folder subtree from {@code from} to {@code to}" operation that the
     * fork copy-pasted at six call sites. Rewrites, across the whole film + our collapse set:
     * <ul>
     *   <li>every {@code replay.category} that is {@code from} or a descendant,</li>
     *   <li>the {@code replayCategoryNames} set (dropping {@code from} if it merges into {@code to}),</li>
     *   <li>the persisted {@code replay_category_order} list,</li>
     *   <li>this list's collapsed-categories set.</li>
     * </ul>
     * When {@code to} is empty the subtree is lifted to root. When {@code dropFromName} is true the exact
     * {@code from} name is removed from the name/order sets (used by "remove folder", which lifts contents
     * to the parent and deletes the folder itself); when false it is rewritten like any other (move/rename).
     */
    private void rewriteSubtree(Film film, String from, String to, boolean dropFromName)
    {
        for (Replay r : film.replays.getList())
        {
            String cat = Replay.normalizeCategory(r.category.get());

            if (ReplayCategoryPaths.isSameOrChildCategory(from, cat))
            {
                r.category.set(ReplayCategoryPaths.rewriteCategoryPrefix(cat, from, to));
            }
        }

        Set<String> names = new HashSet<>();

        for (String name : film.replayCategoryNames.get())
        {
            String c = Replay.normalizeCategory(name);

            if (c.isEmpty())
            {
                continue;
            }

            if (dropFromName && c.equals(from))
            {
                continue;
            }

            names.add(ReplayCategoryPaths.isSameOrChildCategory(from, c) ? ReplayCategoryPaths.rewriteCategoryPrefix(c, from, to) : c);
        }

        names.remove("");
        film.replayCategoryNames.set(names);

        List<String> nextOrder = new ArrayList<>();

        for (String s : orderList(film))
        {
            String c = Replay.normalizeCategory(s);

            if (c.isEmpty())
            {
                continue;
            }

            if (dropFromName && c.equals(from))
            {
                continue;
            }

            nextOrder.add(ReplayCategoryPaths.isSameOrChildCategory(from, c) ? ReplayCategoryPaths.rewriteCategoryPrefix(c, from, to) : c);
        }

        setOrderList(film, nextOrder);

        if (!this.refreshedCollapsedCategories.isEmpty())
        {
            Set<String> collapsed = new HashSet<>();

            for (String c : this.refreshedCollapsedCategories)
            {
                String cc = Replay.normalizeCategory(c);

                if (cc.isEmpty())
                {
                    continue;
                }

                if (dropFromName && cc.equals(from))
                {
                    continue;
                }

                collapsed.add(ReplayCategoryPaths.isSameOrChildCategory(from, cc) ? ReplayCategoryPaths.rewriteCategoryPrefix(cc, from, to) : cc);
            }

            this.refreshedCollapsedCategories.clear();
            this.refreshedCollapsedCategories.addAll(collapsed);
        }
    }

    /* ---------------------------------------------------------------------------------------------------- */
    /* Folder ops                                                                                            */
    /* ---------------------------------------------------------------------------------------------------- */

    /**
     * Remove a folder: lift its contents up to the parent, then drop the folder name itself. Invoked from
     * the base {@code UIReplayList}'s "Remove folder" context action, which {@code UIReplayListMixin} routes
     * here (instead of the base flat implementation) for our instances via {@link INestedFolderList}.
     */
    @Override
    public void refreshed$removeFolder(String normalizedCategory)
    {
        Film film = this.panel.getData();

        String normalizedName = Replay.normalizeCategory(normalizedCategory);

        if (film == null || normalizedName.isEmpty())
        {
            return;
        }

        String parent = ReplayCategoryPaths.categoryParent(normalizedName);

        this.rewriteSubtree(film, normalizedName, parent, true);
        this.refreshedCollapsedCategories.remove(normalizedName);

        this.refreshReplayList();
        this.updateFilmEditor();
    }

    private void moveFolder(String rawFrom, String rawToParent)
    {
        Film film = this.panel.getData();

        if (film == null)
        {
            return;
        }

        String from = Replay.normalizeCategory(rawFrom);
        String toParent = Replay.normalizeCategory(rawToParent);

        if (from.isEmpty())
        {
            return;
        }

        if (!toParent.isEmpty() && ReplayCategoryPaths.isSameOrChildCategory(from, toParent))
        {
            return;
        }

        String leaf = ReplayCategoryPaths.categoryLeaf(from);
        String to = toParent.isEmpty() ? leaf : toParent + "/" + leaf;
        to = Replay.normalizeCategory(to);

        if (to.equals(from))
        {
            return;
        }

        this.rewriteSubtree(film, from, to, false);

        this.refreshReplayList();
        this.updateFilmEditor();
    }

    private void reorderFolder(String rawFrom, String rawTo)
    {
        Film film = this.panel.getData();

        if (film == null)
        {
            return;
        }

        String from = Replay.normalizeCategory(rawFrom);
        String to = Replay.normalizeCategory(rawTo);

        if (from.isEmpty() || to.isEmpty() || from.equals(to))
        {
            return;
        }

        List<String> folders = this.collectCategoryNames(film);
        int fromIndex = folders.indexOf(from);
        int toIndex = folders.indexOf(to);

        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex)
        {
            return;
        }

        /* Move the whole subtree as one contiguous block (folder + descendants). */
        ArrayList<String> block = new ArrayList<>();

        for (String c : folders)
        {
            if (ReplayCategoryPaths.isSameOrChildCategory(from, c))
            {
                block.add(c);
            }
        }

        if (block.isEmpty())
        {
            return;
        }

        ArrayList<String> next = new ArrayList<>();

        for (String c : folders)
        {
            if (!ReplayCategoryPaths.isSameOrChildCategory(from, c))
            {
                next.add(c);
            }
        }

        int insertAt = next.indexOf(to);

        if (insertAt < 0)
        {
            insertAt = next.size();
        }

        next.addAll(insertAt, block);

        setOrderList(film, next);
        this.refreshReplayList();
        this.update();
    }

    private void openRenameFolderOverlay(String rawFolder)
    {
        Film film = this.panel.getData();

        if (film == null)
        {
            return;
        }

        String from = Replay.normalizeCategory(rawFolder);

        if (from.isEmpty())
        {
            return;
        }

        String parent = ReplayCategoryPaths.categoryParent(from);
        String rawLeaf = ReplayCategoryPaths.categoryLeaf(from);

        UITextbox box = new UITextbox(1000, (s) -> {});
        box.setText(rawLeaf);
        box.placeholder(ReplayFolderKeys.RENAME_FOLDER_PLACEHOLDER);

        UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(ReplayFolderKeys.RENAME_FOLDER_TITLE, ReplayFolderKeys.RENAME_FOLDER_DESCRIPTION, (ok) ->
        {
            if (!ok)
            {
                return;
            }

            /* New leaf is a single segment: strip any nested separators the user typed. */
            String newLeaf = ReplayCategoryPaths.categoryLeaf(Replay.normalizeCategory(box.getText()));

            if (newLeaf.isEmpty() || newLeaf.equals(rawLeaf))
            {
                return;
            }

            String to = parent.isEmpty() ? newLeaf : parent + "/" + newLeaf;
            to = Replay.normalizeCategory(to);

            if (to.isEmpty() || to.equals(from))
            {
                return;
            }

            /* Colliding with an existing sibling silently merges (same as create). */
            this.rewriteSubtree(film, from, to, false);

            this.refreshReplayList();
            this.updateFilmEditor();
        });

        box.relative(panel.confirm).y(-1F, -5).w(1F).h(20);
        panel.confirm.w(1F, -10);
        panel.content.add(box);

        UIOverlay.addOverlay(this.getContext(), panel);
    }

    @Override
    public void refreshed$openAddCategory()
    {
        Film film = this.panel.getData();

        if (film == null)
        {
            return;
        }

        List<Replay> selectedReplays = new ArrayList<>(this.getSelectedReplays());
        List<String> selectedFolders = new ArrayList<>();

        for (int idx : this.current)
        {
            if (!this.exists(idx))
            {
                continue;
            }

            ReplayListEntry e = this.list.get(idx);

            if (e != null && e.isFolder())
            {
                String folder = Replay.normalizeCategory(e.folderName);

                if (!folder.isEmpty() && !selectedFolders.contains(folder))
                {
                    selectedFolders.add(folder);
                }
            }
        }

        UITextbox box = new UITextbox(1000, (s) -> {});
        box.setText("");
        box.placeholder(UIKeys.SCENE_REPLAYS_ADD_CATEGORY_PLACEHOLDER);

        UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(UIKeys.SCENE_REPLAYS_ADD_CATEGORY_TITLE, UIKeys.SCENE_REPLAYS_ADD_CATEGORY_DESCRIPTION, (ok) ->
        {
            if (!ok)
            {
                return;
            }

            String cat = Replay.normalizeCategory(box.getText());

            if (cat.isEmpty())
            {
                return;
            }

            Set<String> names = new HashSet<>(film.replayCategoryNames.get());

            names.add(cat);
            film.replayCategoryNames.set(names);

            List<String> order = new ArrayList<>(orderList(film));

            if (!order.contains(cat))
            {
                order.add(cat);
                setOrderList(film, order);
            }

            this.uncollapseAncestors(cat);

            /* Move the current selection into the newly created folder. */
            if (!selectedReplays.isEmpty())
            {
                this.applyReplayCategory(selectedReplays, cat);
            }

            if (!selectedFolders.isEmpty())
            {
                for (String folder : selectedFolders)
                {
                    this.moveFolder(folder, cat);
                }
            }

            this.refreshReplayList();
            this.updateFilmEditor();
        });

        box.relative(panel.confirm).y(-1F, -5).w(1F).h(20);
        panel.confirm.w(1F, -10);
        panel.content.add(box);

        UIOverlay.addOverlay(this.getContext(), panel);
    }

    private void openMoveFolderToCategoryContextMenu(String rawFolder)
    {
        Film film = this.panel.getData();

        if (film == null)
        {
            return;
        }

        String from = Replay.normalizeCategory(rawFolder);

        if (from.isEmpty())
        {
            return;
        }

        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        context.replaceContextMenu((add) ->
        {
            add.action(Icons.ARROW_DOWN, UIKeys.SCENE_REPLAYS_CATEGORY_NONE, () -> this.moveFolder(from, ""));

            for (String c : this.collectCategoryNames(film))
            {
                if (c.isEmpty())
                {
                    continue;
                }

                /* Cannot move into itself or its descendants. */
                if (ReplayCategoryPaths.isSameOrChildCategory(from, c))
                {
                    continue;
                }

                final String to = c;

                add.action(Icons.FOLDER, IKey.raw(RefreshedTextUtils.processColoredText(to)), () -> this.moveFolder(from, to));
            }
        });
    }

    private void applyReplayCategory(List<Replay> selected, String rawCategory)
    {
        String cat = Replay.normalizeCategory(rawCategory);

        for (Replay r : selected)
        {
            r.category.set(cat);
        }

        this.uncollapseAncestors(cat);

        this.refreshReplayList();
        this.restoreReplaySelection(selected);
        this.updateFilmEditor();
    }

    private void dropReplaysOntoCategory(int folderIndex)
    {
        ReplayListEntry folderEntry = this.list.get(folderIndex);

        if (!folderEntry.isFolder())
        {
            return;
        }

        ReplayListEntry draggedEntry = this.list.get(this.dragging);

        if (!draggedEntry.isReplay())
        {
            return;
        }

        this.applyReplayCategory(List.of(draggedEntry.replay), folderEntry.folderName);
    }

    private void updateFilmEditor()
    {
        this.panel.getController().createEntities();
        this.panel.replayEditor.updateChannelsList();
    }

    /* ---------------------------------------------------------------------------------------------------- */
    /* Mouse                                                                                                 */
    /* ---------------------------------------------------------------------------------------------------- */

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.isFiltering())
        {
            return super.subMouseClicked(context);
        }

        if (this.scroll.mouseClicked(context))
        {
            return true;
        }

        if (this.area.isInside(context) && context.mouseButton == 0)
        {
            int index = this.scroll.getIndex(context.mouseX, context.mouseY);

            if (this.exists(index))
            {
                ReplayListEntry entry = this.list.get(index);

                if (entry.isFolder())
                {
                    if (this.sorting)
                    {
                        /* Arm a quick-release toggle; a held drag moves the folder instead. */
                        this.refreshedPendingFolderToggleIndex = index;
                        this.dragging = index;
                        this.dragTime = System.currentTimeMillis();
                    }
                    else
                    {
                        this.toggleFolderCollapsed(entry.folderName);
                    }

                    return true;
                }

                this.applySelectionOnClick(index);

                if (this.sorting && entry.isReplay() && this.current.size() == 1)
                {
                    this.dragging = index;
                    this.dragTime = System.currentTimeMillis();
                }

                if (this.callback != null)
                {
                    this.callback.accept(this.getCurrent());

                    return true;
                }
            }
        }

        return super.subMouseClicked(context);
    }

    private void toggleFolderCollapsed(String rawFolder)
    {
        String name = Replay.normalizeCategory(rawFolder);

        if (name.isEmpty())
        {
            return;
        }

        if (this.refreshedCollapsedCategories.contains(name))
        {
            this.refreshedCollapsedCategories.remove(name);
        }
        else
        {
            this.refreshedCollapsedCategories.add(name);
        }

        List<Replay> keep = new ArrayList<>(this.getSelectedReplays());
        this.refreshReplayList();
        this.restoreReplaySelection(keep);
        this.update();
        this.updateFilmEditor();
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.sorting && !this.isFiltering())
        {
            if (this.isDragging())
            {
                int index = this.scroll.getIndex(context.mouseX, context.mouseY);

                /* Past the last row (padding below short lists): move dragged replay/folder to root. */
                if (index == -2)
                {
                    ReplayListEntry dragged = this.list.get(this.dragging);

                    if (dragged.isReplay())
                    {
                        this.applyReplayCategory(List.of(dragged.replay), "");
                    }
                    else if (dragged.isFolder())
                    {
                        this.moveFolder(dragged.folderName, "");
                    }
                }
                else if (index != this.dragging && this.exists(index))
                {
                    ReplayListEntry a = this.list.get(this.dragging);
                    ReplayListEntry b = this.list.get(index);

                    if (a.isReplay() && b.isFolder())
                    {
                        this.dropReplaysOntoCategory(index);
                    }
                    else if (a.isReplay() && b.isReplay())
                    {
                        /* Inherited handleSwap already remaps Anchor/EntityClip indices — reuse it. */
                        this.handleSwap(this.dragging, index);
                    }
                    else if (a.isFolder() && b.isFolder())
                    {
                        String aFolder = Replay.normalizeCategory(a.folderName);
                        String bFolder = Replay.normalizeCategory(b.folderName);
                        String aParent = ReplayCategoryPaths.categoryParent(aFolder);
                        String bParent = ReplayCategoryPaths.categoryParent(bFolder);

                        /* Same parent: reorder siblings. Different parent: reparent into target. */
                        if (aParent.equals(bParent))
                        {
                            this.reorderFolder(aFolder, bFolder);
                        }
                        else
                        {
                            this.moveFolder(a.folderName, b.folderName);
                        }
                    }
                    else if (a.isFolder() && b.isReplay())
                    {
                        /* Reparent the folder into the replay's folder. */
                        this.moveFolder(a.folderName, b.replay.category.get());
                    }
                }
            }
            else if (this.exists(this.refreshedPendingFolderToggleIndex))
            {
                ReplayListEntry entry = this.list.get(this.refreshedPendingFolderToggleIndex);

                if (entry.isFolder())
                {
                    this.toggleFolderCollapsed(entry.folderName);
                }
            }

            this.dragging = -1;
            this.refreshedPendingFolderToggleIndex = -1;
        }

        this.scroll.mouseReleased(context);

        return super.subMouseReleased(context);
    }

    /* ---------------------------------------------------------------------------------------------------- */
    /* Render                                                                                                */
    /* ---------------------------------------------------------------------------------------------------- */

    @Override
    protected String elementToString(UIContext context, int i, ReplayListEntry element)
    {
        if (element.isFolder())
        {
            return RefreshedTextUtils.processColoredText(ReplayCategoryPaths.categoryLeaf(element.folderName));
        }

        int w = this.area.w - 20 - element.indent;

        return context.batcher.getFont().limitToWidth(element.replay.getName(), w);
    }

    @Override
    protected void renderElementPart(UIContext context, ReplayListEntry element, int i, int x, int y, boolean hover, boolean selected)
    {
        if (element.isFolder())
        {
            int h = this.scroll.scrollItemSize;

            /* Highlight a folder as a drop target while dragging a replay onto it. */
            if (hover && this.sorting && this.isDragging() && this.exists(this.dragging))
            {
                ReplayListEntry dragged = this.list.get(this.dragging);

                if (dragged != null && dragged.isReplay())
                {
                    int fill = Colors.setA(BBSSettings.primaryColor(), 0.22F);
                    int border = Colors.setA(BBSSettings.primaryColor(), 0.78F);
                    float r = UICornerRadii.interfaceChromeClamped(this.area.w, h);

                    ((IRoundedBatcher) context.batcher).roundedFrame(this.area.x, y, this.area.w, h, r, 1F, border, fill);
                }
            }

            this.renderTreeGuides(context, element, i, y, x + element.indent);

            x += element.indent;

            boolean collapsed = this.refreshedCollapsedCategories.contains(Replay.normalizeCategory(element.folderName));

            this.renderFolderDisclosureIcon(context, x, y, h, collapsed);

            String leaf = ReplayCategoryPaths.categoryLeaf(element.folderName);
            String legacy = RefreshedTextUtils.processColoredText(leaf);
            Text label = RefreshedTextUtils.legacyToText(legacy);
            int textColor = hover ? Colors.HIGHLIGHT : Colors.WHITE;
            int textY = y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2;

            context.batcher.getContext().drawText(context.batcher.getFont().getRenderer(), label, x + FOLDER_INDENT_UNIT, textY, textColor, false);

            return;
        }

        this.renderTreeGuides(context, element, i, y, x + element.indent);

        x += element.indent;

        Replay replay = element.replay;

        /*
         * Enabled: the plain list-item text render (name in white/highlight). We inline the base
         * UIList.renderElementPart body here rather than calling super.renderElementPart — our direct
         * super is UIReplayList, whose renderElementPart would re-apply element.indent AND re-draw the form
         * thumbnail (double render). The fork could call super because ITS super was UIList; ours cannot.
         */
        if (replay.enabled.get())
        {
            context.batcher.textShadow(this.elementToString(context, i, element), x + 4, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, hover ? Colors.HIGHLIGHT : Colors.WHITE);
        }
        else
        {
            context.batcher.textShadow(this.elementToString(context, i, element), x + 4, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, hover ? Colors.mulRGB(Colors.HIGHLIGHT, 0.75F) : Colors.GRAY);
        }

        Form form = replay.form.get();

        if (form != null)
        {
            int formX = this.area.x + this.area.w - 30;

            context.batcher.clip(formX, y, 40, 20, context);

            int formY = y - 10;

            FormUtilsClient.renderUI(form, context, formX, formY, formX + 40, formY + 40);

            context.batcher.unclip(context);

            if (replay.fp.get())
            {
                context.batcher.outlinedIcon(Icons.ARROW_UP, formX, formY + 20, 0.5F, 0.5F);
            }
        }
    }

    /* --- Tree guides + disclosure icon --- */

    private static String[] splitCategoryParts(String normalizedCategory)
    {
        if (normalizedCategory == null || normalizedCategory.isEmpty())
        {
            return new String[0];
        }

        return normalizedCategory.split("/");
    }

    private static String prefix(String[] parts, int depth)
    {
        if (depth <= 0 || parts.length == 0)
        {
            return "";
        }

        int d = Math.min(depth, parts.length);

        if (d == 1)
        {
            return parts[0];
        }

        StringBuilder b = new StringBuilder();

        for (int i = 0; i < d; i++)
        {
            if (i > 0)
            {
                b.append('/');
            }

            b.append(parts[i]);
        }

        return b.toString();
    }

    private String[] elementCategoryParts(ReplayListEntry element)
    {
        if (element == null)
        {
            return new String[0];
        }

        if (element.isFolder())
        {
            return splitCategoryParts(Replay.normalizeCategory(element.folderName));
        }

        if (element.isReplay())
        {
            return splitCategoryParts(Replay.normalizeCategory(element.replay.category.get()));
        }

        return new String[0];
    }

    private boolean hasNextAtLevel(int startIndex, int levelDepth, String prefixAtLevel)
    {
        if (prefixAtLevel.isEmpty())
        {
            return false;
        }

        for (int j = startIndex + 1; j < this.list.size(); j++)
        {
            ReplayListEntry e = this.list.get(j);
            String[] parts = this.elementCategoryParts(e);

            if (parts.length < levelDepth)
            {
                continue;
            }

            if (prefix(parts, levelDepth).equals(prefixAtLevel))
            {
                return true;
            }
        }

        return false;
    }

    private void renderTreeGuides(UIContext context, ReplayListEntry element, int index, int y, int contentX)
    {
        int h = this.scroll.scrollItemSize;
        int lineColor = Colors.setA(Colors.GRAY, 0.65F);

        String[] parts = this.elementCategoryParts(element);
        int depth = parts.length;

        if (depth <= 0)
        {
            return;
        }

        /* Root category headers (depth=1 folders) don't draw guides; guides start from them. */
        if (element.isFolder() && depth == 1)
        {
            return;
        }

        int cy = y + h / 2;
        int x0 = this.area.x + 6;

        /* Ancestor vertical lines (levels 1..depth-2), only if that branch continues further down. */
        for (int level = 1; level <= depth - 2; level++)
        {
            String pfx = prefix(parts, level);

            if (!this.hasNextAtLevel(index, level, pfx))
            {
                continue;
            }

            int vx = x0 + (level - 1) * FOLDER_INDENT_UNIT;
            context.batcher.box(vx, y, vx + 1, y + h, lineColor);
        }

        int connectorLevel = depth == 1 ? 1 : (depth - 1);
        int vx = x0 + (connectorLevel - 1) * FOLDER_INDENT_UNIT;
        String connectorPrefix = prefix(parts, connectorLevel);
        boolean continues = this.hasNextAtLevel(index, connectorLevel, connectorPrefix);

        /* Vertical connector to the joint; below the joint only if the branch continues. */
        context.batcher.box(vx, y, vx + 1, cy + 1, lineColor);

        if (continues)
        {
            context.batcher.box(vx, cy, vx + 1, y + h, lineColor);
        }

        /* Horizontal connector into the row. */
        int hx2 = Math.max(vx + 8, contentX - 4);
        context.batcher.box(vx, cy, hx2, cy + 1, lineColor);
    }

    private void renderFolderDisclosureIcon(UIContext context, int x, int y, int rowHeight, boolean collapsed)
    {
        /* MOVE_RIGHT is a narrow sub-cell icon (6x16) — center it in the 16px slot so the
         * icon's own center coincides with the rotation pivot (slot center). */
        int ix = x + (FOLDER_INDENT_UNIT - Icons.MOVE_RIGHT.w) / 2;
        int iy = y + (rowHeight - Icons.MOVE_RIGHT.h) / 2;
        DrawContext dc = context.batcher.getContext();

        if (collapsed)
        {
            context.batcher.icon(Icons.MOVE_RIGHT, ix, iy);

            return;
        }

        /* Open state: rotate the same icon 90 degrees clockwise around the slot center. */
        float cx = x + FOLDER_INDENT_UNIT / 2F;
        float cy = iy + Icons.MOVE_RIGHT.h / 2F;

        dc.getMatrices().push();
        dc.getMatrices().translate(cx, cy, 0F);
        dc.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
        dc.getMatrices().translate(-cx, -cy, 0F);

        context.batcher.icon(Icons.MOVE_RIGHT, ix, iy);

        dc.getMatrices().pop();
    }
}
