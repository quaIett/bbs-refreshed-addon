package org.qualet.refreshedui.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.api.client.events.L10nReloadEvent;
import mchorse.bbs_mod.api.Subscribe;
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
        set(l10n, "animation_duration", "Animation duration", "Длительность анимаций", ru);
        set(l10n, "animation_duration-comment",
            "Stretches or shortens every interface animation together, keeping their proportions: 100% plays them as designed, 50% twice as fast, 200% twice as slow. Delays (such as the wait before a tooltip) are not affected.",
            "Растягивает или сжимает все анимации интерфейса разом, сохраняя их пропорции: 100% — как задумано, 50% — вдвое быстрее, 200% — вдвое медленнее. Задержки (например, ожидание перед подсказкой) не меняются.", ru);
        set(l10n, "alternative_trackpads", "Alternative trackpad layout", "Альтернативный вид трекпадов", ru);
        set(l10n, "alternative_trackpads-comment",
            "When enabled, the transform editor uses the alternative layout: a mode selector (translate / scale / rotate) on top with just the active mode's X/Y/Z trackpads below, instead of all groups at once. Disable to restore the classic always-visible rows. Reopen the editor to apply.",
            "Если включено, редактор трансформации использует альтернативный вид: сверху селектор режима (перемещение / масштаб / поворот), а ниже только X/Y/Z трекпады активного режима, вместо всех групп сразу. Выключите, чтобы вернуть классические всегда видимые ряды. Переоткройте редактор, чтобы применить.", ru);
        set(l10n, "grey_clips", "Grey clips", "Серые клипы", ru);
        set(l10n, "grey_clips-comment",
            "When enabled, clips on the camera and action timelines get a neutral grey fill and their type colour moves to the outline; hovered and selected clips light up instead of getting a white frame. Disable to restore BBS's coloured fills.",
            "Если включено, клипы на таймлайнах камеры и действий получают нейтральную серую заливку, а цвет их типа переходит в обводку; при наведении и выделении клип светлеет вместо белой рамки. Выключите, чтобы вернуть цветные заливки BBS.", ru);

        setKey(l10n, "refreshedui.caxton_notice.title", "Heads up!", "Внимание!", ru);
        setKey(l10n, "refreshedui.caxton_notice.body",
            "BBS Refreshed looks much better with the Caxton mod: the design was built around a smooth font, not the pixel one. I recommend installing it.",
            "BBS Refreshed выглядит намного лучше с модом Caxton: дизайн изначально проектировался под гладкий шрифт, а не под пиксельный. Рекомендую его установить.", ru);
        setKey(l10n, "refreshedui.caxton_notice.link", "Download Caxton", "Скачать Caxton", ru);
        setKey(l10n, "refreshedui.caxton_notice.dismiss", "Don't show again", "Больше не показывать", ru);
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
