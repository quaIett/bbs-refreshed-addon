package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import org.objectweb.asm.Opcodes;
import org.qualet.refreshedui.client.font.RefreshedFont;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Routes all BBS UI text through the addon's pinned {@link RefreshedFont} renderer.
 *
 * <p>{@code Batcher2D.getDefaultTextRenderer()} is the single chokepoint for UI text: every call it
 * sets the shared {@code FontRenderer} wrapper's delegate to
 * {@code MinecraftClient.getInstance().textRenderer}. We redirect that field read to our renderer,
 * so both drawing AND measurement (the wrapper's {@code getWidth}/{@code wrap}/{@code limitToWidth}
 * all delegate to the same renderer) switch to our Caxton-backed font in one shot.</p>
 *
 * <p>The target is {@code static}, so the handler is {@code static} too (see mixin gotchas). The
 * redirected {@code GETFIELD} passes the field owner as parameter 0.</p>
 */
@Mixin(Batcher2D.class)
public abstract class Batcher2DFontMixin
{
    @Redirect(
        method = "getDefaultTextRenderer",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;textRenderer:Lnet/minecraft/client/font/TextRenderer;",
            opcode = Opcodes.GETFIELD
        )
    )
    private static TextRenderer refreshedui$useCustomFont(MinecraftClient instance)
    {
        TextRenderer custom = RefreshedFont.get();

        return custom != null ? custom : instance.textRenderer;
    }
}
