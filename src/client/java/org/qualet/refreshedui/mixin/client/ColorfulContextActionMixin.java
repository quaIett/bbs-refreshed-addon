package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.context.ColorfulContextAction;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Context-menu "active toggle" entries (bind-to-editor, lock-layout, … — anything built via
 * {@code ContextMenuManager.action(icon, label, highlight, runnable)}) are {@link ColorfulContextAction}s.
 * Their active marker is drawn by an OWN {@code renderBackground} override — a square 2px accent bar plus a
 * short horizontal gradient — which {@link ContextActionMixin} (it only rounds the base
 * {@code ContextAction.renderBackground} hover box) never reaches. So in menus like the film menu the
 * active entries stayed square while everything else rounded.
 *
 * <p>Replace that accent bar + gradient with our flat rounded selection fill over the whole row, matching
 * the rest of the refreshed selection style. The fill reads stronger on hover so the two states stay
 * distinct. {@code color} is the action's tint ({@code primaryColor(0)} for the highlight overload), so its
 * own alpha is ignored in favour of our A50 / A75.</p>
 */
@Mixin(ColorfulContextAction.class)
public abstract class ColorfulContextActionMixin
{
    @Shadow
    public int color;

    /**
     * @author refreshedui
     * @reason Flat rounded active fill instead of the square accent bar + gradient, for parity with the
     *         engine-wide rounded selection style.
     */
    @Overwrite
    protected void renderBackground(UIContext context, int x, int y, int w, int h, boolean hover, boolean selected)
    {
        int alpha = hover ? Colors.A75 : Colors.A50;

        RoundedAreas.roundedBox(context.batcher, x, y, w, h, UICornerRadii.buttonsAndTrackpads(), alpha | this.color);
    }
}
