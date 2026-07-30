package org.qualet.refreshedui.mixin.client;

import org.qualet.refreshedui.client.ui.IDefaultValue;
import org.qualet.refreshedui.client.ui.RefreshedSlider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UINumericInput;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import mchorse.bbs_mod.ui.utils.Area;

/**
 * Puts our slider look ({@link RefreshedSlider}) on BBS 2.4's own {@code UISliderTrackpad}, which by
 * now backs every native slider in the UI — the settings screen ({@code UIValueFactory} builds one
 * for each value declared {@code .slider()}), keyframe factories, the pose and model editors, the
 * texture painter, film player settings, context menus and most form panels. One mixin therefore
 * covers all of them; {@code UISliderTrackpadAdapter} covers the remaining plain trackpads.
 *
 * <p><b>Geometry.</b> The element is carved into [ rail | gap | number box ], so all the slider
 * measurements have to be taken against the rail rather than the whole element. Rather than rewrite
 * the drag, only the four geometry primitives it is built on are redirected
 * ({@code getHandleWidth}, {@code getTrackWidth}, {@code getHandleCenter},
 * {@code getValueFromMouse}) — BBS's own dragging, its ctrl snapping and its shift/alt precision
 * then land on the rail unchanged.</p>
 *
 * <p><b>Gestures.</b> Left click on the number box focuses native text editing (BBS keeps that on
 * the middle button, which still works), and right click outside a drag resets the field to the
 * value its panel loaded ({@link IDefaultValue}). Everything else stays stock.</p>
 */
@Mixin(UISliderTrackpad.class)
public abstract class UISliderTrackpadMixin
{
    @Shadow @Final protected Area handleArea;

    @Shadow protected boolean dragging;

    @Shadow protected abstract boolean hasSliderRange();

    @Shadow protected abstract float getProgress();

    @Shadow protected abstract int getHandleCenter();

    @Shadow protected abstract void updateHandleArea();

    @Shadow protected abstract void updateDragging(int mouseX);

    /** Lazily built (a mixin field initialiser would have to run inside the target's constructors). */
    @Unique
    private Area refreshedui$railArea;

    @Unique
    private UISliderTrackpad refreshedui$self()
    {
        return (UISliderTrackpad) (Object) this;
    }

    /** Recomputed on demand — it is a pure function of the element area, so it can never go stale. */
    @Unique
    private Area refreshedui$rail()
    {
        if (this.refreshedui$railArea == null)
        {
            this.refreshedui$railArea = new Area();
        }

        RefreshedSlider.rail(this.refreshedui$self().area, this.refreshedui$railArea);

        return this.refreshedui$railArea;
    }

    /* Geometry — the four primitives BBS's drag is expressed in, moved onto the rail */

    /** Knob diameter; {@code getHandlePadding} halves it, which lands on our knob radius. */
    @Inject(method = "getHandleWidth", at = @At("HEAD"), cancellable = true)
    private void refreshedui$handleWidth(CallbackInfoReturnable<Integer> cir)
    {
        cir.setReturnValue(RefreshedSlider.knobRadius(this.refreshedui$self().area) * 2);
    }

    @Inject(method = "getTrackWidth", at = @At("HEAD"), cancellable = true)
    private void refreshedui$trackWidth(CallbackInfoReturnable<Integer> cir)
    {
        cir.setReturnValue(RefreshedSlider.trackWidth(this.refreshedui$self().area, this.refreshedui$rail()));
    }

    @Inject(method = "getHandleCenter", at = @At("HEAD"), cancellable = true)
    private void refreshedui$handleCenter(CallbackInfoReturnable<Integer> cir)
    {
        cir.setReturnValue(RefreshedSlider.handleCenter(this.refreshedui$self().area, this.refreshedui$rail(), this.getProgress()));
    }

    @Inject(method = "getValueFromMouse", at = @At("HEAD"), cancellable = true)
    private void refreshedui$valueFromMouse(int mouseX, CallbackInfoReturnable<Double> cir)
    {
        UISliderTrackpad self = this.refreshedui$self();

        cir.setReturnValue(RefreshedSlider.valueFromMouse(self.area, this.refreshedui$rail(), mouseX, self.min, self.max));
    }

    /** Whether this field is drawn as a slider at all — without limits there is no track to lay out. */
    @Unique
    private boolean refreshedui$hasBox()
    {
        return this.hasSliderRange() && RefreshedSlider.hasNumberBox(this.refreshedui$self().area);
    }

    /** Hand the text field the number box — {@code UINumericInput.resize} gives it the whole element. */
    @Inject(method = "resize", at = @At("TAIL"))
    private void refreshedui$layout(CallbackInfo ci)
    {
        if (!this.refreshedui$hasBox())
        {
            /* Leave the whole element to the text field, as stock does */
            return;
        }

        UISliderTrackpad self = this.refreshedui$self();

        RefreshedSlider.box(self.area, self.textbox.area);
    }

    /* Gestures */

    @Inject(method = "subMouseClicked", at = @At("HEAD"), cancellable = true)
    private void refreshedui$click(UIContext context, CallbackInfoReturnable<Boolean> cir)
    {
        if (this.dragging)
        {
            /* Mid-drag the stock cancel bindings own the buttons */
            return;
        }

        UISliderTrackpad self = this.refreshedui$self();

        /* Number box — focus native text editing and place the caret */
        if (context.mouseButton == 0 && this.refreshedui$hasBox() && self.textbox.area.isInside(context))
        {
            if (!self.isFocused())
            {
                context.focus(self);
            }

            self.textbox.mouseClicked(context.mouseX, context.mouseY, context.mouseButton);

            cir.setReturnValue(true);

            return;
        }

        /* Right click outside a drag — back to the value the panel loaded */
        if (context.mouseButton == 1 && self.area.isInside(context))
        {
            IDefaultValue value = (IDefaultValue) (Object) self;

            if (value.refreshedui$hasDefault())
            {
                if (self.isFocused())
                {
                    context.unfocus();
                }

                self.setValueAndNotify(value.refreshedui$getDefault());
                this.updateHandleArea();

                cir.setReturnValue(true);
            }
        }
    }

    /* Rendering */

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void refreshedui$render(UIContext context, CallbackInfo ci)
    {
        ci.cancel();

        UISliderTrackpad self = this.refreshedui$self();

        if (this.dragging)
        {
            if (self.isFocused())
            {
                context.unfocus();
            }

            this.updateDragging(context.mouseX);
        }

        this.updateHandleArea();

        if (self.isFocused())
        {
            /* Focused: native editing — background, caret and focus underline come from TextboxMixin */
            self.textbox.render(context);
        }
        else
        {
            /* The value text follows the textbox's color, so axis-tinted sliders stay tinted */
            String label = self.forcedLabel == null ? UINumericInput.format(self.getValue()) : self.forcedLabel.get();
            int color = self.textbox.getColor();

            if (!this.hasSliderRange())
            {
                RefreshedSlider.renderPlainField(context, self.area, label, color);
            }
            else
            {
                boolean knobHot = this.dragging || this.handleArea.isInside(context);

                RefreshedSlider.renderTrack(context, self.area, this.refreshedui$rail(), true, this.getHandleCenter(), knobHot);

                if (this.refreshedui$hasBox())
                {
                    RefreshedSlider.renderNumberBox(context, self.textbox.area, label, color);
                }
                else
                {
                    RefreshedSlider.renderInlineValue(context, self.area, label, color);
                }
            }
        }

        self.renderLockedArea(context);

        /* Reproduce UIElement.render's tail, since HEAD + cancel never reaches super */
        if (self.tooltip != null && self.area.isInside(context))
        {
            context.tooltip.set(context, self);
        }

        for (IUIElement element : ((UIElement) (Object) self).getChildren())
        {
            if (element.isVisible() && element.canBeRendered(context.getViewport()))
            {
                element.render(context);
            }
        }
    }
}
