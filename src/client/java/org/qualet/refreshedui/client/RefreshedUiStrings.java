package org.qualet.refreshedui.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.events.L10nReloadEvent;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.l10n.L10n;

/**
 * Supplies localized labels for the addon's "refreshed" personalization group at runtime instead of
 * shipping a string source pack. On every {@link L10nReloadEvent} (and once at client init) it sets
 * the {@code content} of our lang keys directly on the loaded string map, picking en/ru by the current
 * language. Keeps base bbs string files untouched and follows language switches automatically.
 */
public class RefreshedUiStrings
{
    private static final String PREFIX = "bbs.config.personalization.refreshed.";

    public static void apply(L10n l10n)
    {
        if (l10n == null)
        {
            return;
        }

        boolean ru = "ru_ru".equals(BBSSettings.language.get());

        set(l10n, "title", "refreshed", "refreshed", ru);
        set(l10n, "show_tooltips", "Show tooltips", "Показывать подсказки", ru);
        set(l10n, "show_tooltips-comment",
            "When enabled, hint tooltips appear when hovering the mouse over interface elements. When disabled, they stay hidden.",
            "Если включено, подсказки появляются при наведении мыши на элементы интерфейса. Если выключено — остаются скрытыми.", ru);
        set(l10n, "animations", "Interface animations", "Анимации интерфейса", ru);
        set(l10n, "animations-comment",
            "When enabled, UI animations play (e.g. the per-letter text reveal when switching editors). Disable to turn all interface animations off.",
            "Если включено, проигрываются анимации интерфейса (например, посимвольное появление текста при переключении редакторов). Выключите, чтобы отключить все анимации интерфейса.", ru);
        set(l10n, "alternative_trackpads", "Alternative trackpad layout", "Альтернативный вид трекпадов", ru);
        set(l10n, "alternative_trackpads-comment",
            "When enabled, the transform editor uses the alternative layout: a mode selector (translate / scale / rotate) on top with just the active mode's X/Y/Z trackpads below, instead of all groups at once. Disable to restore the classic always-visible rows. Reopen the editor to apply.",
            "Если включено, редактор трансформации использует альтернативный вид: сверху селектор режима (перемещение / масштаб / поворот), а ниже только X/Y/Z трекпады активного режима, вместо всех групп сразу. Выключите, чтобы вернуть классические всегда видимые ряды. Переоткройте редактор, чтобы применить.", ru);
        set(l10n, "ik_controller_overlay", "IK controller-only overlay", "Оверлей IK только по контроллерам", ru);
        set(l10n, "ik_controller_overlay-comment",
            "When enabled, the IK debug overlay shows only markers on target bones named controller_* (the reworked minimal overlay). Disable to restore BBS's full chain overlay (skeleton, effector, pole).",
            "Если включено, отладочный оверлей IK показывает только маркеры на целевых костях с именем controller_* (переработанный минимальный оверлей). Выключите, чтобы вернуть полный оверлей цепочки BBS (скелет, эффектор, полюс).", ru);

        /* Nested-folder replay list actions (upstream BBS has no equivalents). */
        setKey(l10n, "bbs.ui.scene.replays.refreshed.move_folder_to_category", "Move folder to…", "Переместить папку в…", ru);
        setKey(l10n, "bbs.ui.scene.replays.refreshed.rename_folder", "Rename folder…", "Переименовать папку…", ru);
        setKey(l10n, "bbs.ui.scene.replays.refreshed.rename_folder.title", "Rename folder", "Переименовать папку", ru);
        setKey(l10n, "bbs.ui.scene.replays.refreshed.rename_folder.description",
            "Enter a new name for this folder. Only the last path segment is changed; nested contents move with it.",
            "Введите новое имя этой папки. Меняется только последний сегмент пути; вложенное содержимое перемещается вместе с ней.", ru);
        setKey(l10n, "bbs.ui.scene.replays.refreshed.rename_folder.placeholder", "Folder name…", "Имя папки…", ru);

        /* Terminology unification: upstream "category" -> "folder" ("папка") across the whole replay-list
         * menu, so the base BBS actions read consistently with our own nested-folder additions above. */
        setKey(l10n, "bbs.ui.scene.replays.context.add_category", "Add folder", "Добавить папку", ru);
        setKey(l10n, "bbs.ui.scene.replays.context.remove_category", "Remove folder", "Удалить папку", ru);
        setKey(l10n, "bbs.ui.scene.replays.context.move_to_category", "Move to folder…", "Переместить в папку…", ru);
        setKey(l10n, "bbs.ui.scene.replays.category.none", "Root (no folder)", "Корень (без папки)", ru);
        setKey(l10n, "bbs.ui.scene.replays.add_category.title", "New folder", "Новая папка", ru);
        setKey(l10n, "bbs.ui.scene.replays.add_category.description",
            "Enter a name for the new folder (empty folders are kept in the film). Use / for nesting.",
            "Введи имя новой папки (пустые папки сохраняются в фильме). Используй / для вложенности.", ru);
        setKey(l10n, "bbs.ui.scene.replays.add_category.placeholder", "Folder name…", "Имя папки…", ru);
    }

    private static void set(L10n l10n, String suffix, String en, String ru, boolean useRu)
    {
        l10n.getKey(PREFIX + suffix).content = useRu ? ru : en;
    }

    /** Like {@link #set} but takes a full lang key (for strings outside the personalization prefix). */
    private static void setKey(L10n l10n, String key, String en, String ru, boolean useRu)
    {
        l10n.getKey(key).content = useRu ? ru : en;
    }

    // Public — the BBS EventBus invokes @Subscribe methods via reflection without setAccessible.
    @Subscribe
    public void onL10nReload(L10nReloadEvent event)
    {
        apply(event.l10n);
    }
}
