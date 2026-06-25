package org.qualet.refreshedui.mixin.client;

import java.util.function.Consumer;

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
 */
@Mixin(UIFramebufferFormPanel.class)
public abstract class UIFramebufferFormPanelMixin
{
    @Redirect(
        method = "<init>",
        at = @At(value = "NEW", target = "mchorse/bbs_mod/ui/framework/elements/input/UITrackpad")
    )
    private UITrackpad refreshedui$boundedSlider(Consumer<Double> callback)
    {
        return new UISliderTrackpadAdapter(callback);
    }
}
