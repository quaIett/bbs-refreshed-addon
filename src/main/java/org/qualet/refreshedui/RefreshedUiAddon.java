package org.qualet.refreshedui;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import org.qualet.refreshedui.resources.RefreshedUiAssetsSourcePack;

/**
 * BBS addon entry point (server/common side) and holder for the addon's settings.
 *
 * <p>The appearance settings live in BBS's own <b>personalization</b> category (matching the
 * original bbs-fs layout) — they are registered there by {@code BBSSettingsMixin}, which injects into
 * {@code BBSSettings.register} and stores the returned values here. Read by {@code UITooltipMixin}
 * and others. The corner radius is fixed (see {@code client.ui.UICornerRadii}), not a setting.</p>
 */
public class RefreshedUiAddon implements BBSAddonMod
{
    /** Show hover tooltips. Default false (hidden unless the user opts in). */
    public static ValueBoolean showTooltips;

    /** Master switch for all animation-core effects. Default true. Read via {@code client.anim.Animations}. */
    public static ValueBoolean animations;

    /** Length of every interface animation, percent of the designed one (50–200, slider). Default 100.
     *  Read via {@code client.anim.Animations#ms}. */
    public static ValueInt animationDuration;

    /** Render bounded-range trackpads as the alternative slider view (rail + knob + entry box).
     *  Default true. Read via {@code client.ui.UISliderTrackpadAdapter}. */
    public static ValueBoolean alternativeTrackpads;

    /** Timeline clips (camera and action) get a neutral grey fill with the type colour moved to the outline.
     *  Default true. Read via {@code UIClipRendererMixin}. */
    public static ValueBoolean greyClips;

    /** Hidden: the user picked "don't show again" on the Caxton recommendation dialog. Default false.
     *  Read via {@code client.ui.CaxtonNotice}. */
    public static ValueBoolean caxtonNoticeDismissed;

    /** Nested "refreshed" group under personalization holding the settings above. */
    public static ValueGroup refreshedGroup;

    /**
     * Override BBS's own {@code bbs}-namespace icon atlas + menu banner with the refreshed versions.
     *
     * <p>{@code registerFirst} is required: BBS already added its {@code InternalAssetsSourcePack} to the
     * {@code assets} list before posting this event, and {@code AssetProvider} serves the first matching
     * pack — so ours must precede it. {@code @Subscribe} methods must be public (invoked via reflection).</p>
     */
    @Subscribe
    public void registerSourcePacks(RegisterSourcePacksEvent event)
    {
        event.provider.registerFirst(new RefreshedUiAssetsSourcePack());
    }
}
