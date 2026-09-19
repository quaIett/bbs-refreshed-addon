package org.qualet.refreshedui.client.anim;

import org.qualet.refreshedui.RefreshedUiAddon;

/**
 * Master switch for the animation core. Every effect built on top of the core should gate on
 * {@link #enabled()} so the single "refreshed &gt; Interface animations" setting turns them all off at
 * once. Backed by {@link RefreshedUiAddon#animations}; defaults to on until that setting is registered.
 *
 * <p>Every animation length also goes through {@link #ms(long)}, so the "refreshed &gt; Animation
 * duration" setting stretches or shortens them all together while keeping their proportions.
 * {@link Tween} and {@link Animator} apply it themselves; helpers doing their own timing math call it.
 * Delays and technical time windows (stale state, frame gaps) stay unscaled.</p>
 */
public final class Animations
{
    private Animations()
    {}

    /** Whether animation-core effects should play. */
    public static boolean enabled()
    {
        return RefreshedUiAddon.animations == null || RefreshedUiAddon.animations.get();
    }

    /** Length multiplier from the "Animation duration" setting (percent); 1 = as designed. */
    public static float durationScale()
    {
        return RefreshedUiAddon.animationDuration == null ? 1F : RefreshedUiAddon.animationDuration.get() / 100F;
    }

    /** A designed animation length in ms, stretched by {@link #durationScale()}. */
    public static long ms(long designMs)
    {
        return Math.round(designMs * durationScale());
    }
}
