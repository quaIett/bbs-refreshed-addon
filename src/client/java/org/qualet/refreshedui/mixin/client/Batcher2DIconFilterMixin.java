package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.qualet.refreshedui.client.batcher.IconAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mipmaps the icon atlas so the addon's high-resolution (2048&times;2048, 8&times; the logical 256
 * cells) SVG-sourced {@code icons.png} downsamples <em>smoothly</em>.
 *
 * <p>BBS loads icon textures with {@code GL_NEAREST}. Simply switching to linear is <em>not</em> enough: an
 * icon is drawn at ~16&ndash;32 px from a 128 px cell (4&ndash;8&times; minification), and bilinear samples
 * only a 2&times;2 footprint out of 8&times;8, so it aliases almost like nearest. Proper minification needs a
 * mipmap chain plus trilinear sampling.</p>
 *
 * <p>{@link IconAtlasTexture#MIP_LEVELS} caps the chain at levels where each 128 px cell is still &ge; 8 px,
 * which covers the on-screen minification range while keeping neighbouring cells from bleeding together; the
 * glyph edges are colour-bled in the atlas itself ({@code tools/icons/bleed_atlas.js}), because
 * {@code glGenerateMipmap} averages straight alpha and black transparent texels grey the edges.</p>
 *
 * <p>MC 1.21.11: sampling is decided by the sampler object and texture view bound per draw, not by the
 * texture's own GL filter. The mips are still generated on BBS's texture (and its Java-side filter flipped to
 * linear, which keeps it off the pixel-art pipeline), but the draw has to go through {@link IconAtlasTexture}
 * — a view over all the levels with an unclamped LOD — which is what the redirect in {@code texturedBox}
 * does. Applied once per {@link Texture} object (the "reload textures" button recreates it), scoped to
 * {@link Icons#ATLAS}.</p>
 */
@Mixin(Batcher2D.class)
public class Batcher2DIconFilterMixin
{
    @Unique
    private static Texture rui$mipmapped;

    @Inject(method = "icon(Lmchorse/bbs_mod/ui/utils/icons/Icon;IFFFF)V", at = @At("HEAD"))
    private void rui$mipmapIconAtlas(Icon icon, int color, float x, float y, float ax, float ay, CallbackInfo ci)
    {
        rui$mipmap(icon);
    }

    @Inject(method = "iconArea(Lmchorse/bbs_mod/ui/utils/icons/Icon;IFFFF)V", at = @At("HEAD"))
    private void rui$mipmapIconAreaAtlas(Icon icon, int color, float x, float y, float w, float h, CallbackInfo ci)
    {
        rui$mipmap(icon);
    }

    @Redirect(
        method = "texturedBox(Lmchorse/bbs_mod/graphics/texture/Texture;IFFFFFFFFII)V",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/graphics/texture/AdoptedTexture;identifier(Lmchorse/bbs_mod/graphics/texture/Texture;)Lnet/minecraft/util/Identifier;")
    )
    private Identifier rui$mipmappedAtlas(Texture texture)
    {
        return IconAtlasTexture.identifier(texture);
    }

    @Unique
    private static void rui$mipmap(Icon icon)
    {
        if (icon == null || icon.texture == null || !icon.texture.equals(Icons.ATLAS))
        {
            return;
        }

        Texture tex = BBSModClient.getTextures().getTexture(icon.texture);

        if (tex == null || tex.id <= 0 || tex == rui$mipmapped)
        {
            return;
        }

        tex.bind();
        tex.setFilter(GL11.GL_LINEAR);
        tex.generateMipmap();
        tex.setParameter(GL12.GL_TEXTURE_MAX_LEVEL, IconAtlasTexture.MIP_LEVELS - 1);
        IconAtlasTexture.adopt(tex);

        rui$mipmapped = tex;
    }
}
