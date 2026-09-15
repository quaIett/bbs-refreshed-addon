package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.graphics.GuiQuadMesh;
import net.minecraft.client.gui.render.state.ColoredQuadGuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.gui.render.state.TextGuiElementRenderState;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import org.qualet.refreshedui.client.anim.GuiAlpha;
import org.qualet.refreshedui.client.batcher.GuiTexturedMesh;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Applies {@link GuiAlpha} to GUI elements as they are recorded: solid and gradient fills, textured quads
 * (icons, textures), BBS's per-vertex coloured meshes, the addon's rounded meshes and text. Elements the
 * fade cannot reach (items, 3D previews) are left as they are. Inert while no fade is pushed.
 */
@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateAlphaMixin
{
    @ModifyVariable(method = "addSimpleElement", at = @At("HEAD"), argsOnly = true)
    private SimpleGuiElementRenderState refreshedui$fadeSimple(SimpleGuiElementRenderState state)
    {
        if (!GuiAlpha.active())
        {
            return state;
        }

        if (state instanceof ColoredQuadGuiElementRenderState q)
        {
            return new ColoredQuadGuiElementRenderState(q.pipeline(), q.textureSetup(), q.pose(), q.x0(), q.y0(), q.x1(), q.y1(),
                GuiAlpha.apply(q.col1()), GuiAlpha.apply(q.col2()), q.scissorArea(), q.bounds());
        }

        if (state instanceof TexturedQuadGuiElementRenderState t)
        {
            return new TexturedQuadGuiElementRenderState(t.pipeline(), t.textureSetup(), t.pose(), t.x1(), t.y1(), t.x2(), t.y2(),
                t.u1(), t.u2(), t.v1(), t.v2(), GuiAlpha.apply(t.color()), t.scissorArea(), t.bounds());
        }

        if (state instanceof GuiQuadMesh.State m)
        {
            return new GuiQuadMesh.State(m.pipeline(), m.textureSetup(), m.xs(), m.ys(), GuiAlpha.apply(m.colors(), m.count()), m.count(), m.scissorArea(), m.bounds());
        }

        if (state instanceof GuiTexturedMesh.State m)
        {
            return m.withColors(GuiAlpha.apply(m.colors(), m.count()));
        }

        return state;
    }

    @ModifyVariable(method = "addText", at = @At("HEAD"), argsOnly = true)
    private TextGuiElementRenderState refreshedui$fadeText(TextGuiElementRenderState state)
    {
        if (!GuiAlpha.active())
        {
            return state;
        }

        return new TextGuiElementRenderState(state.textRenderer, state.orderedText, state.matrix, state.x, state.y,
            GuiAlpha.applyText(state.color), GuiAlpha.apply(state.backgroundColor), state.shadow, state.trackEmpty, state.clipBounds);
    }
}
