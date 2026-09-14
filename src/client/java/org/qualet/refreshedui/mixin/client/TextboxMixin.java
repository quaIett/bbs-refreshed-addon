package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.text.utils.Textbox;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.ui.IMaterialFieldHost;
import org.qualet.refreshedui.client.ui.MaterialField;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MD3 filled text field (design 2026-09-15): container + hover layer + animated bottom indicator via
 *  {@link MaterialField}, primary-coloured caret, flat (shadowless) text and a neutral placeholder. */
@Mixin(Textbox.class)
public abstract class TextboxMixin implements IMaterialFieldHost
{
    @Shadow private boolean focused;
    @Shadow private boolean enabled;
    @Shadow public Area area;

    @Unique
    private final MaterialField.State refreshedui$field = new MaterialField.State();

    @Unique
    private boolean refreshedui$hovered;

    @Override
    public MaterialField.State refreshedui$fieldState()
    {
        return this.refreshedui$field;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void refreshedui$captureHover(UIContext context, CallbackInfo ci)
    {
        this.refreshedui$hovered = this.enabled && this.area.isInside(context.mouseX, context.mouseY);
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V")
    )
    private void refreshedui$materialSurface(Area area, Batcher2D batcher, int color)
    {
        MaterialField.render(batcher, area, UICornerRadii.interfaceChrome(), this.refreshedui$field, this.refreshedui$hovered, this.focused);
    }

    /** Stock focused accent (ordinal 0, {@code border && focused}) — replaced by the surface's own indicator. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V", ordinal = 0)
    )
    private void refreshedui$dropAccent(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {}

    /** Caret (ordinal 2, after the selection highlight) — primary colour, stock blink alpha kept. */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V", ordinal = 2)
    )
    private void refreshedui$primaryCaret(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        batcher.box(x1, y1, x2, y2, (color & 0xff000000) | (BBSSettings.primaryColor.get() & 0xffffff));
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;textShadow(Ljava/lang/String;FFI)V")
    )
    private void refreshedui$flatText(Batcher2D batcher, String label, float x, float y, int color)
    {
        batcher.text(label, x, y, color);
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 0xaaaaaa))
    private int refreshedui$placeholderColor(int color)
    {
        return MaterialField.ON_SURFACE_VARIANT & 0xffffff;
    }
}
