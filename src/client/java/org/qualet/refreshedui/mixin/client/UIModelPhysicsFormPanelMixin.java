package org.qualet.refreshedui.mixin.client;

import java.util.function.Consumer;

import mchorse.bbs_mod.ui.forms.editors.panels.UIModelPhysicsFormPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import org.qualet.refreshedui.client.ui.UISliderTrackpadAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Turns the bounded-range trackpads in the Physics form-properties panel into sliders.
 *
 * <p>BBS 2.4 grew its own {@code UISliderTrackpad} and converted most of this panel to it natively
 * ({@code gravity}, {@code stiffness}, {@code damping}, {@code radius}, the wind fields, and the
 * {@code axisTrackpad(...)} helper behind the relative-gravity rotations). The only bounded field
 * still built as a plain {@code UITrackpad} is {@code iterations} (1..20) in the constructor, so
 * that is all this redirect has left to convert.</p>
 */
@Mixin(UIModelPhysicsFormPanel.class)
public abstract class UIModelPhysicsFormPanelMixin
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
