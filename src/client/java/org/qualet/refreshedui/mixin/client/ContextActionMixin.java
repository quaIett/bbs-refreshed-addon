package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.context.ColorfulContextAction;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Context-menu entry hover highlight: a plain rounded neutral grey wash
 * ({@link RoundedAreas#renderMenuHover}) instead of the engine's accent gradient + stripe. No stroke and no
 * colour, so hover reads as a cursor hint and the frame look is reserved for active toggles
 * ({@link ColorfulContextActionMixin}).
 *
 * <p>BBS 2.6 (upstream 29cdbb4cd) draws the hover through {@code RowStyle.hover(batcher, x, y, w, h, color)};
 * the colour argument is ignored on purpose — a hover is the same grey whatever the row is.</p>
 *
 * <p>Coloured entries ({@link ColorfulContextAction}) also get their icon tinted in the entry's own colour —
 * that is where a tagged row (clip type, colour-coded copy action) now carries its colour at rest, since
 * {@link ColorfulContextActionMixin} no longer paints a resting background for it. A tag too dark to read
 * on the menu surface (the remapper clip is {@code 0x222222}) falls back to white — a colour that only
 * worked as a background stripe is no colour for a glyph. Every entry's icon is also drawn a touch smaller
 * than its cell ({@link #ICON_SCALE}), centred where the full-size one sat.</p>
 */
@Mixin(ContextAction.class)
public abstract class ContextActionMixin
{
    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/RowStyle;hover(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;IIIII)V")
    )
    private void refreshedui$roundHighlight(Batcher2D batcher, int x, int y, int w, int h, int color)
    {
        RoundedAreas.renderMenuHover(batcher, x, y, w, h, UICornerRadii.buttonsAndTrackpads());
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;icon(Lmchorse/bbs_mod/ui/utils/icons/Icon;FFFF)V")
    )
    private void refreshedui$tintIcon(Batcher2D batcher, Icon icon, float x, float y, float ax, float ay)
    {
        int color = Colors.WHITE;

        if ((Object) this instanceof ColorfulContextAction colorful && refreshedui$isReadable(colorful.color))
        {
            color = Colors.A100 | (colorful.color & Colors.RGB);
        }

        if (icon.texture == null)
        {
            return;
        }

        /* The stock call anchors at (ax, ay) of the icon's own size; keep that anchor point and shrink
         * around it, so the glyph stays centred in the row and left-aligned with the label gutter. */
        float size = Math.min(icon.w, icon.h) * ICON_SCALE;
        float cx = x - icon.w * ax + icon.w / 2F;
        float cy = y - icon.h * ay + icon.h / 2F;

        batcher.scaledIcon(icon, color, cx - size / 2F, cy - size / 2F, size);
    }

    /** A tint counts as readable when at least one channel clears this; below it the glyph would vanish. */
    private static boolean refreshedui$isReadable(int color)
    {
        int max = Math.max(Math.max((color >> 16) & 0xFF, (color >> 8) & 0xFF), color & 0xFF);

        return max >= MIN_READABLE_CHANNEL;
    }

    /** Context-menu icon size relative to its atlas cell (16px -> ~14px). */
    private static final float ICON_SCALE = 0.85F;
    private static final int MIN_READABLE_CHANNEL = 0x60;
}
