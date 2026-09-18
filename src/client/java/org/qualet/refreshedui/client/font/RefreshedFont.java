package org.qualet.refreshedui.client.font;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.EffectGlyph;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.GlyphProvider;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;

/**
 * Lazily builds a dedicated {@link TextRenderer} pinned to the addon's own font id
 * ({@code refreshedui:default}) so BBS UI text renders in our Caxton-backed font, without
 * touching vanilla's global {@code minecraft:default} (the rest of the game stays untouched).
 *
 * <p>The renderer reuses vanilla's glyph provider ({@code TextRenderer.fonts}, opened via the addon
 * access widener) but answers every font lookup with our font id; sprite and player-head glyphs
 * (1.21.9+) pass through. Vanilla's provider reads the live {@code FontManager} storages, so this
 * stays correct across resource reloads.</p>
 *
 * <p>Our {@code assets/refreshedui/font/default.json} layers the Caxton provider over the vanilla
 * {@code space}/{@code default}/{@code unifont} references, so glyphs our font lacks still
 * fall back through this single storage.</p>
 */
public final class RefreshedFont
{
    public static final Identifier FONT_ID = Identifier.of("refreshedui", "default");

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

        TextRenderer.GlyphsProvider base = vanilla.fonts;

        /* Caxton 0.9 casts TextRenderer.fonts to FontManager.Fonts (for its outer FontManager), so the
         * pinned provider has to be a Fonts subclass rather than a bare GlyphsProvider. */
        if (!(base instanceof FontManager.Fonts))
        {
            return vanilla;
        }

        StyleSpriteSource pinned = new StyleSpriteSource.Font(FONT_ID);

        renderer = new TextRenderer(mc.fontManager.new Fonts(false)
        {
            @Override
            public GlyphProvider getGlyphs(StyleSpriteSource source)
            {
                return base.getGlyphs(source instanceof StyleSpriteSource.Font ? pinned : source);
            }

            @Override
            public EffectGlyph getRectangleGlyph()
            {
                return base.getRectangleGlyph();
            }
        });

        CaxtonFontPin.pin(renderer, FONT_ID);

        return renderer;
    }
}
