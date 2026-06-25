package org.qualet.refreshedui.mixin;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.qualet.refreshedui.RefreshedUiAddon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers the addon's appearance settings inside BBS's own <b>personalization</b> category as a
 * nested "refreshed" group (3.1 + 3.14). The group is added to personalization for persistence, but
 * the settings UI does not auto-render nested groups (no factory) — {@code UISettingsOverlayPanelMixin}
 * renders the group's header + values at the bottom of the personalization view. Injected right after
 * {@code builder.category("personalization", ...)} (ordinal 1 of {@code category(String, Icon)}).
 */
@Mixin(BBSSettings.class)
public abstract class BBSSettingsMixin
{
    @Inject(
        method = "register",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/settings/SettingsBuilder;category(Ljava/lang/String;Lmchorse/bbs_mod/ui/utils/icons/Icon;)Lmchorse/bbs_mod/settings/SettingsBuilder;",
            ordinal = 1,
            shift = At.Shift.AFTER
        )
    )
    private static void refreshedui$registerAppearanceSettings(SettingsBuilder builder, CallbackInfo ci)
    {
        ValueInt radius = new ValueInt("ui_corner_radius", 4, 0, 16);
        ValueBoolean tooltips = new ValueBoolean("show_tooltips", true);

        ValueGroup group = new ValueGroup("refreshed");
        group.icon = Icons.GEAR;
        group.add(radius);
        group.add(tooltips);

        builder.getCategory().add(group);

        RefreshedUiAddon.uiCornerRadius = radius;
        RefreshedUiAddon.showTooltips = tooltips;
        RefreshedUiAddon.refreshedGroup = group;
    }

    /*
     * Palette overhaul (design): retarget the DARK surface values of the theme. Each surface getter
     * folds its dark constant in as a literal, so @ModifyConstant swaps only that literal — the
     * light-theme values and the brightness/theme machinery (getThemeSurface -> applyBackgroundBrightness)
     * stay intact, so the brightness slider and light theme keep working.
     *
     *   deepSurface   -> 181b1f   (the global panel background)
     *   baseSurface   -> 171a1e   ("group" background; ~matches deep so it currently reads as one)
     *   raisedSurface -> 14171b   (the recessed input fill: text fields, trackpads, ...)
     */
    @ModifyConstant(method = "baseSurface", constant = @Constant(intValue = 0xff171a1f))
    private static int refreshedui$baseDark(int original)
    {
        return 0xff171a1e;
    }

    @ModifyConstant(method = "deepSurface", constant = @Constant(intValue = 0xff0f1217))
    private static int refreshedui$deepDark(int original)
    {
        return 0xff181b1f;
    }

    @ModifyConstant(method = "raisedSurface", constant = @Constant(intValue = 0xff1d2127))
    private static int refreshedui$raisedDark(int original)
    {
        return 0xff14171b;
    }
}
