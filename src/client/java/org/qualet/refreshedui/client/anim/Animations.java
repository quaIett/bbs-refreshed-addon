package org.qualet.refreshedui.client.anim;

import org.qualet.refreshedui.RefreshedUiAddon;

/**
 * Master switch for the animation core. Every effect built on top of the core should gate on
 * {@link #enabled()} so the single "refreshed &gt; Interface animations" setting turns them all off at
 * once. Backed by {@link RefreshedUiAddon#animations}; defaults to on until that setting is registered.
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
}
