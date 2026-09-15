package org.qualet.refreshedui;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
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

    /** Render bounded-range trackpads as the alternative slider view (rail + knob + entry box).
     *  Default true. Read via {@code client.ui.UISliderTrackpadAdapter}. */
    public static ValueBoolean alternativeTrackpads;

    /** When on, BBS's blur behind overlay panels runs as dual Kawase instead of its box blur; BBS's own
     *  on/off and radius settings still apply. Default true. Read via {@code client.blur.RefreshedBlur}. */
    public static ValueBoolean refreshedBlur;

    /** Timeline clips (camera and action) get a neutral grey fill with the type colour moved to the outline.
     *  Default true. Read via {@code UIClipRendererMixin}. */
    public static ValueBoolean greyClips;

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
