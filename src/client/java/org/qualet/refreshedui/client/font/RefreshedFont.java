package org.qualet.refreshedui.client.font;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/**
 * Lazily builds a dedicated {@link TextRenderer} pinned to the addon's own font id
 * ({@code refreshedui:default}) so BBS UI text renders in our Caxton-backed font, without
 * touching vanilla's global {@code minecraft:default} (the rest of the game stays untouched).
 *
 * <p>The renderer reuses vanilla's {@code fontStorageAccessor} (opened via the addon access
 * widener) but forces every lookup to our font id. Vanilla's closure reads the live
 * {@code FontManager} storage map, so this stays correct across resource reloads. Caxton mixes a
 * per-instance {@code CaxtonTextRenderer} into our {@link TextRenderer} at construction, so drawing
 * through it goes down Caxton's MSDF path automatically.</p>
 *
 * <p>Our {@code assets/refreshedui/font/default.json} layers the Caxton (Inter) provider over the
 * vanilla {@code space}/{@code default}/{@code unifont} references, so glyphs Inter lacks still
 * fall back through this single storage.</p>
 */
public final class RefreshedFont
{
    public static final Identifier FONT_ID = new Identifier("refreshedui", "default");

    private static TextRenderer renderer;

    private RefreshedFont()
    {
    }

    /**
     * @return the addon's pinned text renderer, or {@code null} until vanilla's renderer exists
     *         (very early boot, before any UI draws).
     */
    public static TextRenderer get()
    {
        if (renderer != null)
        {
            return renderer;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer vanilla = mc == null ? null : mc.textRenderer;

        if (vanilla == null)
        {
            return null;
        }

        Function<Identifier, FontStorage> base = vanilla.fontStorageAccessor;
        renderer = new TextRenderer(id -> base.apply(FONT_ID), false);

        return renderer;
    }
}
