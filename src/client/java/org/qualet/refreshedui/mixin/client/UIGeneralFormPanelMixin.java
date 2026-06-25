package org.qualet.refreshedui.mixin.client;

import java.util.function.Consumer;

import mchorse.bbs_mod.ui.forms.editors.panels.UIGeneralFormPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import org.qualet.refreshedui.client.ui.UISliderTrackpadAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Turns the bounded-range trackpads in the General form-properties panel into sliders. The adapter
 * only renders as a slider for finite {@code limit(min, max)} fields: {@code uiScale} (0.01..100),
 * {@code hitboxSneakMultiplier} and {@code hitboxEyeHeight} (0..1). The one-sided fields
 * ({@code hitboxWidth}/{@code hitboxHeight}/{@code hp}/{@code speed}/{@code stepHeight}) stay normal
 * trackpads.
 */
@Mixin(UIGeneralFormPanel.class)
public abstract class UIGeneralFormPanelMixin
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
