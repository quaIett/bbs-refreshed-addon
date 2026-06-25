package org.qualet.refreshedui.mixin.client;

import java.util.function.Consumer;

import mchorse.bbs_mod.ui.forms.editors.panels.UIModelIKFormPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import org.qualet.refreshedui.client.ui.UISliderTrackpadAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Turns the bounded-range trackpads in the IK form-properties panel into sliders by swapping every
 * {@code new UITrackpad(callback)} for a {@link UISliderTrackpadAdapter}. The adapter only renders as
 * a slider when the field gets a finite {@code limit(min, max)}; here that's {@code poleAngle}
 * (-180..180), {@code softness} (0..1) and {@code weight} (0..1). {@code chainLength} is min-only, so
 * the adapter leaves it as a normal trackpad.
 */
@Mixin(UIModelIKFormPanel.class)
public abstract class UIModelIKFormPanelMixin
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
