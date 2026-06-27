package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.qualet.refreshedui.client.anim.Animator;
import org.qualet.refreshedui.client.anim.PanelTransitions;
import org.qualet.refreshedui.client.anim.StaggerText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts all UI text drawing to play the per-letter appear stagger. Every text path in BBS funnels
 * through {@code Batcher2D.text(String, float, float, int, boolean)} ({@code textCard} and the other
 * overloads all delegate to it), so a single hook here covers labels, buttons, section titles, list rows —
 * everything the appearing panel draws.
 *
 * <p>When a panel reveal is active and this draw is happening inside that panel's subtree
 * ({@link PanelTransitions#activeAnimator()}), the full-string draw is replaced by a staggered one and
 * cancelled. {@link StaggerText#render} re-emits each glyph through this same method, so the
 * {@link StaggerText#isRendering()} guard lets those per-glyph draws pass straight through.</p>
 */
@Mixin(Batcher2D.class)
public abstract class Batcher2DTextStaggerMixin
{
    @Inject(
        method = "text(Ljava/lang/String;FFIZ)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void refreshedui$staggerText(String label, float x, float y, int color, boolean shadow, CallbackInfo ci)
    {
        if (StaggerText.isRendering())
        {
            return;
        }

        Animator animator = PanelTransitions.activeAnimator();

        if (animator == null)
        {
            return;
        }

        Batcher2D self = (Batcher2D) (Object) this;

        StaggerText.render(self, self.getFont(), label, x, y, color, shadow, animator);

        ci.cancel();
    }
}
