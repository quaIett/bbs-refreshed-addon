package org.qualet.refreshedui.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIClickable;
import mchorse.bbs_mod.ui.utils.Area;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import org.qualet.refreshedui.client.anim.Animations;
import org.qualet.refreshedui.client.anim.Easings;
import org.qualet.refreshedui.client.anim.Tween;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Text buttons give under the pointer: while pressed a {@link UIButton} sinks a little (scaled about its
 * centre) and darkens, and springs back when let go.
 *
 * <p>Wraps the {@code renderSkin} call of {@link UIClickable#render} — the only place a button's face is
 * drawn. "Pressed" is BBS's own flag AND the physical button: the flag is only cleared by a release that
 * reaches the button, and a button whose press opened an overlay never hears that release — it would stay
 * sunk forever.</p>
 */
@Mixin(UIClickable.class)
public abstract class UIClickableMixin
{
    @Unique
    private static final long PRESS_IN_MS = 50L;
    @Unique
    private static final long PRESS_OUT_MS = 170L;
    @Unique
    private static final float PRESS_SCALE = 0.955F;
    @Unique
    private static final float PRESS_DARKEN = 0.85F;

    @Shadow
    protected boolean pressed;

    /** How far in the press is, 0..1, and when it was last advanced. */
    @Unique
    private float refreshedui$press;
    @Unique
    private long refreshedui$pressAt;

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/buttons/UIClickable;renderSkin(Lmchorse/bbs_mod/ui/framework/UIContext;)V")
    )
    private void refreshedui$pressSkin(UIClickable<?> self, UIContext context)
    {
        if (!(self instanceof UIButton button))
        {
            this.refreshedui$renderSkin(self, context);

            return;
        }

        long now = Tween.now();
        boolean down = this.pressed && button.isEnabled() && Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT) && Animations.enabled();
        long dt = Math.min(100L, Math.max(0L, now - this.refreshedui$pressAt));

        this.refreshedui$pressAt = now;
        this.refreshedui$press = down
            ? Math.min(1F, this.refreshedui$press + dt / (float) PRESS_IN_MS)
            : Math.max(0F, this.refreshedui$press - dt / (float) PRESS_OUT_MS);

        if (this.refreshedui$press <= 0F)
        {
            this.refreshedui$renderSkin(self, context);

            return;
        }

        float p = Easings.outCubic(this.refreshedui$press);
        float scale = 1F - (1F - PRESS_SCALE) * p;
        float shade = 1F - (1F - PRESS_DARKEN) * p;
        Area area = button.area;
        MatrixStack matrices = context.batcher.getContext().getMatrices();

        context.batcher.flush();
        matrices.push();
        matrices.translate(area.x + area.w / 2F, area.y + area.h / 2F, 0F);
        matrices.scale(scale, scale, 1F);
        matrices.translate(-(area.x + area.w / 2F), -(area.y + area.h / 2F), 0F);
        RenderSystem.setShaderColor(shade, shade, shade, 1F);

        this.refreshedui$renderSkin(self, context);

        context.batcher.flush();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        matrices.pop();
    }

    /** The redirected call is {@code this.renderSkin(context)}, so {@code self} is always this element. */
    @Unique
    private void refreshedui$renderSkin(UIClickable<?> self, UIContext context)
    {
        this.renderSkin(context);
    }

    @Shadow
    protected abstract void renderSkin(UIContext context);
}
