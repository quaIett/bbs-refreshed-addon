package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.context.ColorfulContextAction;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.SelectionMerge;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Coloured context-menu entries ({@link ColorfulContextAction}, built via
 * {@code ContextMenuManager.action(icon, label, color, runnable)}) come in two flavours that stock BBS draws
 * identically (square 2px accent bar + short gradient):
 *
 * <ul>
 *     <li><b>Active toggles</b> — bind-to-editor, lock-layout, the current option of a {@code UIChoiceMenu}
 *     (e.g. the clip's current type in the convert menu). Their colour is the accent
 *     ({@code BBSSettings.primaryColor(0)}) and the marker means "this one is ON", so it must stay visible
 *     at rest: a persistent selection frame (bright stroke + muted fill), adjacent ones merged into one block
 *     via {@link SelectionMerge} / {@link UIActionListMixin}.</li>
 *     <li><b>Tagged rows</b> — every clip type in the add/convert menus, colour-tagged copy actions. The
 *     colour is a category, not a state, so a whole list of them lit at once was noise (see the design
 *     mockup): at rest they draw NOTHING here — the tint lives in the icon instead
 *     ({@link ContextActionMixin#refreshedui$tintIcon}) — and the hovered row gets a plain neutral grey
 *     rounded wash, no stroke, no colour (the icon already says which row it is) — the same hover every
 *     plain entry gets ({@link ContextActionMixin}).</li>
 * </ul>
 */
@Mixin(ColorfulContextAction.class)
public abstract class ColorfulContextActionMixin
{
    @Shadow
    public int color;

    /**
     * @author refreshedui
     * @reason Rounded selection frame (bright stroke + muted fill) instead of the square accent bar +
     *         gradient; tagged (non-accent) rows get a plain grey hover wash instead.
     */
    @Overwrite
    protected void renderBackground(UIContext context, int x, int y, int w, int h, boolean hover, boolean selected)
    {
        float radius = UICornerRadii.buttonsAndTrackpads();

        if (refreshedui$isActiveToggle(this.color))
        {
            RoundedAreas.renderSelectionFrameVertical(context.batcher, x, y, w, h, this.color, radius,
                !SelectionMerge.top(), !SelectionMerge.bottom(), hover);
        }
        else if (hover)
        {
            RoundedAreas.renderMenuHover(context.batcher, x, y, w, h, radius);
        }
    }

    /** The accent colour is what {@code ContextMenuManager.action(..., highlight=true, ...)} stamps on ON entries. */
    private static boolean refreshedui$isActiveToggle(int color)
    {
        return (color & Colors.RGB) == (BBSSettings.primaryColor(0) & Colors.RGB);
    }
}
