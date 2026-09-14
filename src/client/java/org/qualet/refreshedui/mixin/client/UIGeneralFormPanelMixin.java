package org.qualet.refreshedui.mixin.client;

import java.util.function.Supplier;

import mchorse.bbs_mod.settings.values.base.BaseValueNumber;
import mchorse.bbs_mod.ui.forms.editors.panels.UIGeneralFormPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import org.qualet.refreshedui.client.ui.UISliderTrackpadAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Turns the bounded-range trackpads in the General form-properties panel into sliders. The adapter
 * only renders as a slider for finite {@code limit(min, max)} fields: {@code uiScale} (0.01..100).
 * The one-sided fields ({@code hitboxWidth}/{@code hitboxHeight}/{@code hp}/{@code speed}/{@code stepHeight})
 * stay normal trackpads.
 *
 * <p>BBS 2.6 builds these fields through {@code UIValues.trackpad(Supplier)} rather than
 * {@code new UITrackpad(...)}, so the helper call is redirected to the adapter's equivalent.
 * {@code hitboxSneakMultiplier} / {@code hitboxEyeHeight} (0..1) are native {@code UISliderTrackpad}s
 * upstream now and get our skin from {@code UISliderTrackpadMixin}.</p>
 */
@Mixin(UIGeneralFormPanel.class)
public abstract class UIGeneralFormPanelMixin
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
