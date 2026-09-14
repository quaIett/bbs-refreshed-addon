package org.qualet.refreshedui.mixin.client;

import java.util.function.Supplier;

import mchorse.bbs_mod.settings.values.base.BaseValueNumber;
import mchorse.bbs_mod.ui.forms.editors.panels.UILabelFormPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import org.qualet.refreshedui.client.ui.UISliderTrackpadAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Turns the bounded-range trackpads in the Label form-properties panel into sliders: {@code shadowX}
 * and {@code shadowY} (-100..100) and, since BBS 2.6, {@code fontSize} ({@code FontManager.MIN_SIZE..MAX_SIZE}).
 * The adapter leaves {@code anchorX}/{@code anchorY} (unbounded) and {@code max} / {@code lineHeight}
 * (pseudo-unbounded {@code Integer.MAX_VALUE} upper limit) as normal trackpads.
 *
 * <p>BBS 2.6 builds these fields through {@code UIValues.trackpad(Supplier)} rather than
 * {@code new UITrackpad(...)}, so the helper call is redirected to the adapter's equivalent.</p>
 */
@Mixin(UILabelFormPanel.class)
public abstract class UILabelFormPanelMixin
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
