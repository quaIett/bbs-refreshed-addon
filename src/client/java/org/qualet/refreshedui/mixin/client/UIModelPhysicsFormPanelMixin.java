package org.qualet.refreshedui.mixin.client;

import java.util.function.Consumer;

import mchorse.bbs_mod.ui.forms.editors.panels.UIModelPhysicsFormPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import org.qualet.refreshedui.client.ui.UISliderTrackpadAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Turns the bounded-range trackpads in the Physics form-properties panel into sliders. Five are built
 * directly in the constructor ({@code gravity} 0..10, {@code stiffness}/{@code damping}/{@code radius}
 * 0..1, {@code iterations} 1..20); the three relative-gravity rotation fields (±180) are built by the
 * private static helper {@code axisTrackpad(...)}. Hence two redirects: one instance handler on
 * {@code <init>}, one static handler on {@code axisTrackpad}.
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

    @Redirect(
        method = "axisTrackpad",
        at = @At(value = "NEW", target = "mchorse/bbs_mod/ui/framework/elements/input/UITrackpad")
    )
    private static UITrackpad refreshedui$boundedSliderAxis(Consumer<Double> callback)
    {
        return new UISliderTrackpadAdapter(callback);
    }
}
