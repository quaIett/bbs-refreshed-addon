package org.qualet.refreshedui.mixin;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.qualet.refreshedui.RefreshedUiAddon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
        ValueBoolean tooltips = new ValueBoolean("show_tooltips", false);
        ValueBoolean animations = new ValueBoolean("animations", true);
        ValueBoolean alternativeTrackpads = new ValueBoolean("alternative_trackpads", true);
        ValueBoolean ikControllerOverlay = new ValueBoolean("ik_controller_overlay", true);

        ValueGroup group = new ValueGroup("refreshed");
        group.icon = Icons.GEAR;
        group.add(tooltips);
        group.add(animations);
        group.add(alternativeTrackpads);
        group.add(ikControllerOverlay);

        builder.getCategory().add(group);

        RefreshedUiAddon.showTooltips = tooltips;
        RefreshedUiAddon.animations = animations;
        RefreshedUiAddon.alternativeTrackpads = alternativeTrackpads;
        RefreshedUiAddon.ikControllerOverlay = ikControllerOverlay;
        RefreshedUiAddon.refreshedGroup = group;
    }
}
