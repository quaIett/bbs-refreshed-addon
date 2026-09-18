package org.qualet.refreshedui.client.font;

import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * Pins Caxton's own font lookup of a {@link TextRenderer} to one font id.
 *
 * <p>Caxton 0.9 (MC 1.21.9+) no longer asks the {@link TextRenderer} for font storages: its
 * {@code CaxtonTextRenderer} and {@code CaxtonTextHandler} resolve storages straight through
 * {@code FontManager.getStorageInternal(styleFontId)}, so the pinned {@code GlyphsProvider} of
 * {@link RefreshedFont} never sees Caxton text. This swaps both {@code fontStorageAccessor} fields for a
 * wrapper that always asks for the pinned id.</p>
 *
 * <p>Caxton is a soft dependency and not on the compile classpath, so this goes through reflection
 * against Caxton 0.9.0 names. Any mismatch is logged once and the renderer keeps working unpinned
 * (Caxton text then falls back to the vanilla font) instead of crashing.</p>
 */
final class CaxtonFontPin
{
    private static final Logger LOG = LoggerFactory.getLogger("refreshedui");

    private CaxtonFontPin()
    {}

    static void pin(TextRenderer renderer, Identifier fontId)
    {
        try
        {
            Class<?> has = Class.forName("xyz.flirora.caxton.render.HasCaxtonTextRenderer");

            if (!has.isInstance(renderer))
            {
                return;
            }

            Object caxton = has.getMethod("getCaxtonTextRenderer").invoke(renderer);
            Object handler = caxton.getClass().getMethod("getHandler").invoke(caxton);

            pinField(caxton, fontId);
            pinField(handler, fontId);
        }
        catch (ClassNotFoundException e)
        {
            /* Older Caxton (or none): it resolves fonts through the TextRenderer itself. */
        }
        catch (Throwable e)
        {
            LOG.warn("Could not pin Caxton to font {}, UI text will use the default font", fontId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void pinField(Object target, Identifier fontId) throws ReflectiveOperationException
    {
        Field field = target.getClass().getDeclaredField("fontStorageAccessor");

        field.setAccessible(true);

        Function<Identifier, FontStorage> base = (Function<Identifier, FontStorage>) field.get(target);

        field.set(target, (Function<Identifier, FontStorage>) id -> base.apply(fontId));
    }
}
