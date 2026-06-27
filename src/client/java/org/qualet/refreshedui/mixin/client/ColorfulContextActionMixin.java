package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.context.ColorfulContextAction;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.SelectionMerge;
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
 * <p>Replace that accent bar + gradient with our selection-frame style over the whole row (bright stroke +
 * muted darker fill, see {@link RoundedAreas#renderSelectionFrameVertical}), matching the refreshed look.
 * The stroke keeps the action's OWN tint {@code color} (custom per action), so colorful actions stay their
 * colour. Adjacent same-colour entries merge into one block via {@link SelectionMerge} flags stamped by
 * {@link UIActionListMixin}.</p>
 */
@Mixin(ColorfulContextAction.class)
public abstract class ColorfulContextActionMixin
{
    @Shadow
    public int color;

    /**
     * @author refreshedui
     * @reason Rounded selection frame (bright stroke + muted fill) instead of the square accent bar +
     *         gradient, for parity with the engine-wide rounded selection style.
     */
    @Overwrite
    protected void renderBackground(UIContext context, int x, int y, int w, int h, boolean hover, boolean selected)
    {
        RoundedAreas.renderSelectionFrameVertical(context.batcher, x, y, w, h, this.color, UICornerRadii.buttonsAndTrackpads(),
            !SelectionMerge.top(), !SelectionMerge.bottom(), hover);
    }
}
