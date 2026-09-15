package org.qualet.refreshedui.client.batcher;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import mchorse.bbs_mod.graphics.texture.Texture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;

/**
 * Records POSITION_TEXTURE_COLOR quads in screen space and submits them into the deferred GUI as one
 * element — the textured counterpart of BBS's {@code GuiQuadMesh}.
 *
 * <p>1.21.11 composites the GUI after {@code Screen.render} returns, so the addon's rounded primitives can
 * no longer draw their mask-sampled 9-slices immediately through a {@code BufferBuilder}; they record them
 * here instead. Vertices come in groups of four (the GUI renderer draws every simple element with the QUADS
 * index buffer), fed through {@code vertex(Matrix3x2fc, x, y).texture(u, v).color(argb)} so the pose is
 * already folded in.</p>
 */
public final class GuiTexturedMesh implements VertexConsumer
{
    private float[] xs = new float[64];
    private float[] ys = new float[64];
    private float[] us = new float[64];
    private float[] vs = new float[64];
    private int[] colors = new int[64];
    private int count;

    private float minX = Float.POSITIVE_INFINITY;
    private float minY = Float.POSITIVE_INFINITY;
    private float maxX = Float.NEGATIVE_INFINITY;
    private float maxY = Float.NEGATIVE_INFINITY;

    public boolean isEmpty()
    {
        return this.count == 0;
    }

    /**
     * Submit the recorded quads sampling {@code texture} (adopted into a vanilla GPU texture, the icon atlas
     * through its mipmapped wrapper). Clipped by the live GUI scissor; nothing happens for an empty or fully
     * clipped mesh.
     */
    public void draw(DrawContext context, RenderPipeline pipeline, Texture texture)
    {
        if (this.count == 0 || texture == null)
        {
            return;
        }

        Identifier id = IconAtlasTexture.identifier(texture);

        if (id == null)
        {
            return;
        }

        AbstractTexture gpu = MinecraftClient.getInstance().getTextureManager().getTexture(id);
        ScreenRect scissor = context.scissorStack.peekLast();
        ScreenRect bounds = this.computeBounds(scissor);

        if (bounds == null)
        {
            return;
        }

        context.state.addSimpleElement(new State(pipeline, TextureSetup.of(gpu.getGlTextureView(), gpu.getSampler()),
            this.xs, this.ys, this.us, this.vs, this.colors, this.count, scissor, bounds));
    }

    private ScreenRect computeBounds(ScreenRect scissor)
    {
        int x = (int) Math.floor(this.minX);
        int y = (int) Math.floor(this.minY);
        int w = (int) Math.ceil(this.maxX) - x;
        int h = (int) Math.ceil(this.maxY) - y;
        ScreenRect bounds = new ScreenRect(x, y, Math.max(0, w), Math.max(0, h));

        return scissor != null ? scissor.intersection(bounds) : bounds;
    }

    private void ensureCapacity()
    {
        if (this.count < this.xs.length)
        {
            return;
        }

        int next = this.xs.length * 2;

        this.xs = java.util.Arrays.copyOf(this.xs, next);
        this.ys = java.util.Arrays.copyOf(this.ys, next);
        this.us = java.util.Arrays.copyOf(this.us, next);
        this.vs = java.util.Arrays.copyOf(this.vs, next);
        this.colors = java.util.Arrays.copyOf(this.colors, next);
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z)
    {
        this.ensureCapacity();

        this.xs[this.count] = x;
        this.ys[this.count] = y;
        this.us[this.count] = 0F;
        this.vs[this.count] = 0F;
        this.colors[this.count] = -1;
        this.count++;

        if (x < this.minX) this.minX = x;
        if (y < this.minY) this.minY = y;
        if (x > this.maxX) this.maxX = x;
        if (y > this.maxY) this.maxY = y;

        return this;
    }

    @Override
    public VertexConsumer color(int argb)
    {
        if (this.count > 0)
        {
            this.colors[this.count - 1] = argb;
        }

        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        return this.color((alpha << 24) | (red << 16) | (green << 8) | blue);
    }

    @Override
    public VertexConsumer texture(float u, float v)
    {
        if (this.count > 0)
        {
            this.us[this.count - 1] = u;
            this.vs[this.count - 1] = v;
        }

        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v)
    {
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v)
    {
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z)
    {
        return this;
    }

    @Override
    public VertexConsumer lineWidth(float width)
    {
        return this;
    }

    /** The recorded mesh as a deferred GUI element. Positions are already in screen space. */
    public record State(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        float[] xs,
        float[] ys,
        float[] us,
        float[] vs,
        int[] colors,
        int count,
        ScreenRect scissorArea,
        ScreenRect bounds
    ) implements SimpleGuiElementRenderState
    {
        @Override
        public void setupVertices(VertexConsumer vertices)
        {
            for (int i = 0; i < this.count; i++)
            {
                vertices.vertex(this.xs[i], this.ys[i], 0F).texture(this.us[i], this.vs[i]).color(this.colors[i]);
            }
        }

        public State withColors(int[] colors)
        {
            return new State(this.pipeline, this.textureSetup, this.xs, this.ys, this.us, this.vs, colors, this.count, this.scissorArea, this.bounds);
        }
    }
}
