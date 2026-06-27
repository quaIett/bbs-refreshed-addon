package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.input.text.utils.Textbox;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Rounds the text field background (3.2a), adds the hairline field border (design overhaul, 3),
 *  and rounds the focused-field accent underline. */
@Mixin(Textbox.class)
public abstract class TextboxMixin
{
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$roundBackground(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderField(area, batcher, color, UICornerRadii.interfaceChrome());
    }

    /**
     * The focused-field accent (drawn when {@code border && focused}) is a flat full-width box at the
     * very bottom of the field, so its sharp corners overshoot the rounded field's bottom corners.
     * Inset it by the field radius — so it spans exactly the field's straight bottom run — and round
     * its ends into a pill where the corners start to curve. Ordinal 0 = this accent box; the later
     * {@code box} calls (selection highlight, caret) are left untouched.
     */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V", ordinal = 0)
    )
    private void refreshedui$roundAccent(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        float radius = UICornerRadii.interfaceChrome();
        float thickness = Math.max(y2 - y1, 1.5F);
        float by = y2 - thickness;

        float bx = x1 + radius;
        float bw = (x2 - x1) - radius * 2F;

        if (bw < 1F)
        {
            bx = x1;
            bw = x2 - x1;
        }

        ((IRoundedBatcher) batcher).roundedBox(bx, by, bw, thickness, thickness / 2F, color);
    }
}
