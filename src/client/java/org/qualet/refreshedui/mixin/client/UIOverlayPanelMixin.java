package org.qualet.refreshedui.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.util.math.MatrixStack;
import org.qualet.refreshedui.client.anim.OverlayReveal;
import org.qualet.refreshedui.client.anim.OverlaySnapshot;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;

/**
 * Overlay panel background (3.2a rounding, 3.6 shadow->outline) and the appear/close animation.
 * {@code renderBackground} draws panel (ordinal 0), icons strip (ordinal 1), close button (ordinal 2):
 * the panel becomes a rounded frame with a muted primary border (or fill+outline when rounding is off),
 * and the icons strip gets a 1px inset to sit inside that border. The drop shadow is dropped.
 *
 * <p>The whole {@code render} is also wrapped to play the {@link OverlayReveal} slide + fade: the panel
 * starts a few pixels low and transparent and settles into place (and leaves the same way). The slide is
 * a matrix translate; the fade goes through {@link OverlaySnapshot} — the panel is drawn opaque into a
 * copy of the screen that is then blended over it — so the rounded frame, icon strip, icons, text and
 * content fade as one flat picture instead of as stacked translucent layers. Without a snapshot target
 * the fade falls back to the shader colour.</p>
 */
@Mixin(UIOverlayPanel.class)
public abstract class UIOverlayPanelMixin
{
    /** True while {@link #refreshedui$revealHead} pushed a matrix (and opened a fade) that the tail must undo. */
    @Unique
    private boolean refreshedui$revealing;

    /** Whether the open fade is a snapshot capture (else the shader-colour fallback). */
    @Unique
    private boolean refreshedui$snapshot;

    /** Visibility sampled at the head, so the tail blends at the same value the slide was placed at. */
    @Unique
    private float refreshedui$visibility;

    @Inject(method = "render", at = @At("HEAD"))
    private void refreshedui$revealHead(UIContext context, CallbackInfo ci)
    {
        this.refreshedui$revealing = false;

        float vis = OverlayReveal.visibility((UIElement) (Object) this);

        if (vis >= 1F)
        {
            return;
        }

        this.refreshedui$visibility = vis;
        this.refreshedui$snapshot = OverlaySnapshot.begin(context);

        if (!this.refreshedui$snapshot)
        {
            RenderSystem.setShaderColor(1F, 1F, 1F, vis);
        }

        MatrixStack matrices = context.batcher.getContext().getMatrices();

        matrices.push();
        matrices.translate(0F, (1F - vis) * OverlayReveal.SLIDE_PX, 0F);

        this.refreshedui$revealing = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void refreshedui$revealTail(UIContext context, CallbackInfo ci)
    {
        if (!this.refreshedui$revealing)
        {
            return;
        }

        /* Vertices are baked with the matrix as they are emitted, so popping before the flush inside end() is fine */
        context.batcher.getContext().getMatrices().pop();

        if (this.refreshedui$snapshot)
        {
            OverlaySnapshot.end(context, this.refreshedui$visibility);
        }
        else
        {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }

        this.refreshedui$revealing = false;
    }

    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;dropShadow(IIIIIII)V")
    )
    private void refreshedui$noShadow(Batcher2D batcher, int left, int top, int right, int bottom, int offset, int opaque, int shadow)
    {
    }

    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V", ordinal = 0)
    )
    private void refreshedui$panelFrame(Area area, Batcher2D batcher, int color)
    {
        int radius = UICornerRadii.interfaceChrome();
        int border = Colors.mulRGB(BBSSettings.primaryColor.get() | Colors.A100, 0.7F);

        if (radius > 0)
        {
            ((IRoundedBatcher) batcher).roundedFrame(area.x, area.y, area.w, area.h, radius, 1F, border, color);
        }
        else
        {
            area.render(batcher, color);
            batcher.outline(area.x, area.y, area.ex(), area.ey(), border);
        }
    }

    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V", ordinal = 1)
    )
    private void refreshedui$iconsInset(Area area, Batcher2D batcher, int color)
    {
        batcher.box(area.x, area.y + 1, area.ex() - 1, area.ey() - 1, color);
    }
}
