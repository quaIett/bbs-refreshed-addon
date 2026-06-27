package org.qualet.refreshedui.client.font;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Gate for the addon's custom UI font. Text drawing only switches to {@link RefreshedFont} when this
 * returns {@code true}; otherwise BBS UI text stays on the vanilla renderer.
 *
 * <p>The custom font is rendered through Caxton (MSDF), which is only a {@code recommends} dependency,
 * so the addon can run without it. Caxton's presence is the toggle: install Caxton to get the custom
 * font, leave it out to keep the vanilla Minecraft font. No separate setting — the mod list decides.</p>
 */
public final class Fonts
{
    private static final boolean CAXTON_PRESENT = FabricLoader.getInstance().isModLoaded("caxton");

    private Fonts()
    {}

    /** Whether BBS UI text should render in the addon's Caxton-backed custom font. */
    public static boolean customFontEnabled()
    {
        return CAXTON_PRESENT;
    }
}
