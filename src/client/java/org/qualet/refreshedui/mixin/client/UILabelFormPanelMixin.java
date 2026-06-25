package org.qualet.refreshedui.mixin.client;

import java.util.function.Consumer;

import mchorse.bbs_mod.ui.forms.editors.panels.UILabelFormPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import org.qualet.refreshedui.client.ui.UISliderTrackpadAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Turns the bounded-range trackpads in the Label form-properties panel into sliders: {@code shadowX}
 * and {@code shadowY} (-100..100). The adapter leaves {@code anchorX}/{@code anchorY}/{@code offset}
 * (unbounded) and {@code max} (pseudo-unbounded {@code limit(-1, Integer.MAX_VALUE)}) as normal
 * trackpads.
 */
@Mixin(UILabelFormPanel.class)
public abstract class UILabelFormPanelMixin
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
