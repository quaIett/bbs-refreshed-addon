package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

/**
 * Clip section headers (3.11): drop the primary-color background and center the label instead.
 * The static {@code label(IKey)} factory builds {@code UI.label(key).background(...)}; redirect that
 * {@code background(Supplier)} call to apply {@code labelAnchor(0.5, 0)} and skip the fill.
 */
@Mixin(UIClip.class)
public abstract class UIClipMixin
{
    @Redirect(
        method = "label",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/UILabel;background(Ljava/util/function/Supplier;)Lmchorse/bbs_mod/ui/framework/elements/utils/UILabel;")
    )
    private static UILabel refreshedui$centerHeaderNoBackground(UILabel label, Supplier<Integer> color)
    {
        return label.labelAnchor(0.5F, 0F);
    }
}
