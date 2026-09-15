package org.qualet.refreshedui.client.batcher;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import mchorse.bbs_mod.graphics.texture.AdoptedTexture;
import mchorse.bbs_mod.graphics.texture.Texture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.util.Identifier;

/**
 * The icon atlas adopted into a vanilla GPU texture WITH its mip chain.
 *
 * <p>On 1.21.11 the GL filter set on a texture object no longer decides how it is sampled: every draw binds a
 * sampler object (which overrides the min filter and the LOD range) and the texture view's level range (written
 * into {@code BASE_LEVEL}/{@code MAX_LEVEL} on each bind). BBS's {@code AdoptedTexture} declares one level and a
 * sampler clamped to LOD 0, so a mipmapped atlas would still be read from level 0 only. This wrapper declares
 * {@link #MIP_LEVELS} levels and takes the sampler without the LOD clamp, which makes the draw trilinear.</p>
 *
 * <p>BBS still owns the GL id; nothing here ever frees it.</p>
 */
public final class IconAtlasTexture extends AbstractTexture
{
    public static final Identifier ID = Identifier.of("refreshedui", "icons_mipmapped");

    /** Level 0 plus four halvings: each 128 px cell of the 2048 atlas stays at least 8 px. */
    public static final int MIP_LEVELS = 5;

    private static Texture adopted;

    /** Register {@code texture} (its mips already generated) as the mipmapped atlas. */
    public static void adopt(Texture texture)
    {
        MinecraftClient.getInstance().getTextureManager().registerTexture(ID, new IconAtlasTexture(texture));
        adopted = texture;
    }

    /** The id to sample {@code texture} through: the mipmapped atlas when it is the adopted one, BBS's own adoption otherwise. */
    public static Identifier identifier(Texture texture)
    {
        if (texture != null && texture == adopted && texture.isValid())
        {
            return ID;
        }

        return AdoptedTexture.identifier(texture);
    }

    private IconAtlasTexture(Texture texture)
    {
        Gl gl = new Gl(texture.id, texture.width, texture.height);

        this.glTexture = gl;
        this.glTextureView = new View(gl);
        this.sampler = RenderSystem.getSamplerCache().get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, true);
    }

    @Override
    public void close()
    {}

    private static final class Gl extends GlTexture
    {
        private Gl(int glId, int width, int height)
        {
            super(GpuTexture.USAGE_TEXTURE_BINDING, "refreshedui_icons_mipmapped", TextureFormat.RGBA8, Math.max(1, width), Math.max(1, height), 1, MIP_LEVELS, glId);
        }

        @Override
        public void close()
        {}
    }

    private static final class View extends GlTextureView
    {
        private View(Gl texture)
        {
            super(texture, 0, MIP_LEVELS);
        }

        @Override
        public void close()
        {}
    }
}
