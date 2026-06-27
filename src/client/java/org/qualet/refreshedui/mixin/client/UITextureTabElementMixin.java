package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.dashboard.textures.UITextureTabElement;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Texture editor tab strip ({@link UITextureTabElement} — one open document = one tab). Its
 * {@code renderSkin} draws the active tab the engine's old way: a dim square primary box behind a
 * plain white icon + label. Bring it to our selected-item style (same as the dock tabs / UIIcons):
 * <ul>
 *   <li>the active tab becomes a single rounded full-primary fill;</li>
 *   <li>the active tab's icon and label use the adaptive contrast color so they read on the fill.</li>
 * </ul>
 */
@Mixin(UITextureTabElement.class)
public abstract class UITextureTabElementMixin
{
    @Redirect(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V")
    )
    private void refreshedui$roundedActiveTab(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        RoundedAreas.roundedBox(batcher, x1, y1, x2 - x1, y2 - y1, UICornerRadii.buttonsAndTrackpads(), BBSSettings.primaryColor(Colors.A100));
    }

    @ModifyArg(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;icon(Lmchorse/bbs_mod/ui/utils/icons/Icon;IFFFF)V"),
        index = 1
    )
    private int refreshedui$blackenActiveIcon(int color, @Local(ordinal = 0) boolean active)
    {
        return active ? UIContrastColor.onPrimary() : color;
    }

    @ModifyArg(
        method = "renderSkin",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;text(Ljava/lang/String;FFI)V"),
        index = 3
    )
    private int refreshedui$blackenActiveLabel(int color, @Local(ordinal = 0) boolean active)
    {
        return active ? UIContrastColor.onPrimary() : color;
    }
}
