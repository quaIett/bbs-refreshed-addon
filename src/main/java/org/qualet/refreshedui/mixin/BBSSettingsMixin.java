package org.qualet.refreshedui.mixin;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.qualet.refreshedui.LockedValueBoolean;
import org.qualet.refreshedui.RefreshedUiAddon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

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
        ValueBoolean refreshedBlur = new ValueBoolean("refreshed_blur", true);
        ValueBoolean greyClips = new ValueBoolean("grey_clips", true);

        ValueGroup group = new ValueGroup("refreshed");
        group.icon = Icons.GEAR;
        group.add(tooltips);
        group.add(animations);
        group.add(alternativeTrackpads);
        group.add(refreshedBlur);
        group.add(greyClips);

        builder.getCategory().add(group);

        RefreshedUiAddon.showTooltips = tooltips;
        RefreshedUiAddon.animations = animations;
        RefreshedUiAddon.alternativeTrackpads = alternativeTrackpads;
        RefreshedUiAddon.refreshedBlur = refreshedBlur;
        RefreshedUiAddon.greyClips = greyClips;
        RefreshedUiAddon.refreshedGroup = group;
    }

    /**
     * Pins BBS 2.6's personalization "Shadows", "Highlights" and "Glow" off: they are registered as
     * {@link LockedValueBoolean} so every reader (Batcher2D bevel, dock inset shadow, dropShadow glow)
     * sees false and the settings toggle is locked.
     */
    @Redirect(
        method = "register",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/settings/SettingsBuilder;getBoolean(Ljava/lang/String;Z)Lmchorse/bbs_mod/settings/values/numeric/ValueBoolean;")
    )
    private static ValueBoolean refreshedui$lockInterfaceDepth(SettingsBuilder builder, String id, boolean defaultValue)
    {
        if (!refreshedui$LOCKED.contains(id) || !"personalization".equals(builder.getCategory().getId()))
        {
            return builder.getBoolean(id, defaultValue);
        }

        ValueBoolean value = new LockedValueBoolean(id);

        builder.register(value);

        return value;
    }

    @Unique
    private static final Set<String> refreshedui$LOCKED = Set.of("interface_shadows", "interface_highlights", "interface_glow");
}
