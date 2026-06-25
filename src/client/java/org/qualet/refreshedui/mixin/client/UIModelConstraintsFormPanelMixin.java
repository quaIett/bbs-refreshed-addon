package org.qualet.refreshedui.mixin.client;

import java.util.function.Consumer;

import mchorse.bbs_mod.ui.forms.editors.panels.UIModelConstraintsFormPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import org.qualet.refreshedui.client.ui.UISliderTrackpadAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Turns the constraint min/max angle trackpads (all six minX/Y/Z, maxX/Y/Z, -180..180) into sliders.
 * They are all built by the private static helper {@code axisTrackpad(...)}, so the redirect targets
 * that method and the handler must be {@code static}.
 */
@Mixin(UIModelConstraintsFormPanel.class)
public abstract class UIModelConstraintsFormPanelMixin
{
    @Redirect(
        method = "axisTrackpad",
        at = @At(value = "NEW", target = "mchorse/bbs_mod/ui/framework/elements/input/UITrackpad")
    )
    private static UITrackpad refreshedui$boundedSlider(Consumer<Double> callback)
    {
        return new UISliderTrackpadAdapter(callback);
    }
}
