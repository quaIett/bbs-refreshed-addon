package org.qualet.refreshedui.mixin.client;

import java.util.function.Supplier;

import mchorse.bbs_mod.settings.values.base.BaseValueNumber;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFramebufferFormPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import org.qualet.refreshedui.client.ui.UISliderTrackpadAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Turns the bounded-range trackpads in the Framebuffer form-properties panel into sliders:
 * {@code width} and {@code height} (2..4096). {@code scale} is unbounded, so the adapter leaves it
 * as a normal trackpad.
 *
 * <p>BBS 2.6 builds these fields through {@code UIValues.trackpad(Supplier)} rather than
 * {@code new UITrackpad(...)}, so the helper call is redirected to the adapter's equivalent.</p>
 */
@Mixin(UIFramebufferFormPanel.class)
public abstract class UIFramebufferFormPanelMixin
{
    @Redirect(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/values/UIValues;trackpad(Ljava/util/function/Supplier;)Lmchorse/bbs_mod/ui/framework/elements/input/UITrackpad;")
    )
    private UITrackpad refreshedui$boundedSlider(Supplier<? extends BaseValueNumber<?>> value)
    {
        return UISliderTrackpadAdapter.boundTrackpad(value);
    }
}
