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
        set(l10n, "ui_corner_radius", "Corner rounding intensity", "Интенсивность скругления", ru);
        set(l10n, "ui_corner_radius-comment",
            "Corner radius of GUI elements (buttons, panels, etc.) in pixels. 0 disables rounding.",
            "Радиус скругления углов элементов интерфейса (кнопок, панелей и т.д.) в пикселях. 0 — отключить скругление.", ru);
        set(l10n, "show_tooltips", "Show tooltips", "Показывать подсказки", ru);
        set(l10n, "show_tooltips-comment",
            "When enabled, hint tooltips appear when hovering the mouse over interface elements. When disabled, they stay hidden.",
            "Если включено, подсказки появляются при наведении мыши на элементы интерфейса. Если выключено — остаются скрытыми.", ru);
        set(l10n, "animations", "Interface animations", "Анимации интерфейса", ru);
        set(l10n, "animations-comment",
            "When enabled, UI animations play (e.g. the per-letter text reveal when switching editors). Disable to turn all interface animations off.",
            "Если включено, проигрываются анимации интерфейса (например, посимвольное появление текста при переключении редакторов). Выключите, чтобы отключить все анимации интерфейса.", ru);
    }

    private static void set(L10n l10n, String suffix, String en, String ru, boolean useRu)
    {
        l10n.getKey(PREFIX + suffix).content = useRu ? ru : en;
    }

    // Public — the BBS EventBus invokes @Subscribe methods via reflection without setAccessible.
    @Subscribe
    public void onL10nReload(L10nReloadEvent event)
    {
        apply(event.l10n);
    }
}
