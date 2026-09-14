package org.qualet.refreshedui.mixin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.qualet.refreshedui.client.anim.OverlayReveal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mipmaps the icon atlas so the addon's high-resolution (2048&times;2048, 8&times; the logical 256
 * cells) SVG-sourced {@code icons.png} downsamples <em>smoothly</em>.
 *
 * <p>BBS loads icon textures with {@code GL_NEAREST} ({@code TextureManager#getTexture}). Simply
 * switching to {@code GL_LINEAR} is <em>not</em> enough: an icon is drawn at ~16&ndash;32 px from a
 * 128 px cell (4&ndash;8&times; minification), and bilinear samples only a 2&times;2 footprint out of
 * 8&times;8, so it aliases almost like nearest. Proper minification needs a mipmap chain plus a
 * mipmap min-filter ({@code GL_LINEAR_MIPMAP_LINEAR} = trilinear). Note BBS's own
 * {@code Texture#setFilterMipmap} sets a plain {@code GL_LINEAR} min-filter, which never samples the
 * generated mips &mdash; so we set the parameters directly.</p>
 *
 * <p>{@code MAX_LEVEL = 4} caps the chain at levels where each 128 px cell is still &ge; 8 px, which
 * covers the on-screen minification range while keeping neighbouring cells from bleeding together;
 * the glyphs' transparent margins mean any residual blur pulls in transparency, not a colour halo.</p>
 *
 * <p>BBS 2.6: {@code Batcher2D#texturedProgram(Texture)} routes every texture whose Java-side
 * {@code filter}/{@code mipmap} fields still say NEAREST through the pixel-art shader (setting
 * {@code pixel_art_smoothing}, on by default), which reads level 0 via {@code texelFetch} and ignores
 * the mips entirely. So the mips go through {@link Texture#generateMipmap()} and the filter through
 * {@link Texture#setFilter(int)} to flip those fields, then the real min-filter/max-level are set on
 * top. The glyph edges are colour-bled in the atlas itself ({@code tools/icons/bleed_atlas.js}),
 * because {@code glGenerateMipmap} averages straight alpha and black transparent texels grey the edges.</p>
 *
 * <p>UVs are normalised against {@code icon.textureW/H} (256), so the hi-res atlas needs no coordinate
 * changes &mdash; only the filter/mips. Applied once per {@link Texture} object (the "reload textures"
 * button deletes and recreates it, and the new one usually gets the old GL id back &mdash; so the id
 * can't be the key), scoped to {@link Icons#ATLAS}. Kept separate from {@link Batcher2DMixin} (the sole
 * MC-render-version-divergent file) to avoid entangling it.</p>
 */
@Mixin(Batcher2D.class)
public class Batcher2DIconFilterMixin
{
    @org.spongepowered.asm.mixin.Unique
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

    /**
     * Fade icons drawn inside an animating overlay panel. Icons go through {@code texturedBox} and do not
     * honour the panel reveal's shader-colour fade the way boxes/text do, so we fade them here by scaling
     * the draw colour's alpha by the panel's current visibility ({@link OverlayReveal#iconAlpha()}; 1 = no
     * fade, the normal case). Covers both {@code icon} and {@code iconArea}.
     */
    @ModifyVariable(method = "icon(Lmchorse/bbs_mod/ui/utils/icons/Icon;IFFFF)V", at = @At("HEAD"), index = 2, argsOnly = true)
    private int rui$fadeIcon(int color)
    {
        float alpha = OverlayReveal.iconAlpha();

        return alpha >= 1F ? color : Colors.mulA(color, alpha);
    }

    @ModifyVariable(method = "iconArea(Lmchorse/bbs_mod/ui/utils/icons/Icon;IFFFF)V", at = @At("HEAD"), index = 2, argsOnly = true)
    private int rui$fadeIconArea(int color)
    {
        float alpha = OverlayReveal.iconAlpha();

        return alpha >= 1F ? color : Colors.mulA(color, alpha);
    }

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

        GlStateManager._bindTexture(tex.id);
        tex.setFilter(GL11.GL_LINEAR);
        tex.generateMipmap();
        tex.setParameter(GL12.GL_TEXTURE_MAX_LEVEL, 4);
        tex.setParameter(GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);

        rui$mipmapped = tex;
    }
}
