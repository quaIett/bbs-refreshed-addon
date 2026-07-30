package org.qualet.refreshedui.client.ui;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.MathUtils;

/**
 * A {@link UITrackpad} that renders and behaves as a horizontal slider whenever it has a finite
 * bounded range on both ends ({@code min} and {@code max}), otherwise it delegates to the normal
 * trackpad behaviour.
 *
 * <p>This exists because BBS form-property panels declare their fields as {@code UITrackpad} and a
 * mixin cannot change a field's type. By subclassing {@code UITrackpad} we stay assignable to those
 * fields, and a {@code @Redirect} on the {@code new UITrackpad(...)} call swaps in this adapter.</p>
 *
 * <p>The look itself lives in {@link RefreshedSlider}, shared with {@code UISliderTrackpadMixin} —
 * BBS 2.4 grew its own {@code UISliderTrackpad} and moved most fields onto it, so the two kinds of
 * slider have to be drawn by the same code to stay identical. In slider mode
 * {@code textbox.area} shrinks to the right number box, so clicking it focuses native text editing
 * (caret, parsing, live value update) — manual entry comes essentially for free. The rail handles
 * slider drag; the box handles typing.</p>
 *
 * <p>When the range is not finite-both ({@code hasSliderRange()} is false) every overridden method
 * falls through to {@code super}, so unbounded / one-sided trackpads keep their original textbox
 * behaviour untouched (and {@code textbox.area} stays the full element).</p>
 */
public class UISliderTrackpadAdapter extends UITrackpad
{
    /** Alt-held drag sensitivity — finer scrubbing (mirrors the trackpad's weak modifier = normal/5). */
    private static final double FINE_DRAG_FACTOR = 0.2D;

    /** Left strip that hosts the rail + knob (the element minus the number box). */
    private final Area railArea = new Area();
    private final Area handleArea = new Area();

    private boolean sliderDragging;
    private double startValue;
    /** Incremental drag accumulator (double, so Alt fine-drag and integer flooring don't lose sub-steps). */
    private double dragValue;
    private int lastMouseX;

    public UISliderTrackpadAdapter()
    {
        super();
    }

    public UISliderTrackpadAdapter(Consumer<Double> callback)
    {
        super(callback);
    }

    /**
     * Slider mode is active only when both bounds are finite and form a real range. Pseudo-unbounded
     * limits ({@code Integer.MIN_VALUE} / {@code Integer.MAX_VALUE} — BBS's "no upper/lower limit"
     * idiom, e.g. {@code UILabelFormPanel.max} = {@code limit(-1, Integer.MAX_VALUE)}) are treated as
     * unbounded so those fields stay normal trackpads instead of a useless billion-wide slider.
     */
    private boolean hasSliderRange()
    {
        return Double.isFinite(this.min) && Double.isFinite(this.max)
            && this.min > Integer.MIN_VALUE && this.max < Integer.MAX_VALUE
            && this.max > this.min;
    }

    @Override
    public boolean isDragging()
    {
        return this.sliderDragging || super.isDragging();
    }

    /**
     * Restore the value the panel last loaded (right click reset), notifying the data model. The
     * baseline itself is recorded for every numeric field by {@code UINumericInputMixin}, which is
     * also what the native sliders reset to.
     */
    private void resetToDefault(UIContext context)
    {
        if (this.textbox.isFocused())
        {
            context.unfocus();
        }

        this.setValueAndNotify(((IDefaultValue) (Object) this).refreshedui$getDefault());
        this.updateHandleArea();
    }

    private boolean hasDefault()
    {
        return ((IDefaultValue) (Object) this).refreshedui$hasDefault();
    }

    /* Slider geometry — all measured against railArea (the left strip), not the full element. */

    /** Carve the element into [ rail strip | gap | number box ]; the box becomes {@code textbox.area}. */
    private void layoutSlider()
    {
        RefreshedSlider.layout(this.area, this.railArea, this.textbox.area);
    }

    private float getProgress()
    {
        if (!this.hasSliderRange())
        {
            return 0F;
        }

        return (float) MathUtils.clamp((this.value - this.min) / (this.max - this.min), 0D, 1D);
    }

    private int getHandleCenter()
    {
        return RefreshedSlider.handleCenter(this.area, this.railArea, this.getProgress());
    }

    private void updateHandleArea()
    {
        if (!this.hasSliderRange())
        {
            this.handleArea.set(this.area.x, this.area.y, 0, this.area.h);

            return;
        }

        int r = RefreshedSlider.knobRadius(this.area);

        this.handleArea.set(this.getHandleCenter() - r, this.area.y, r * 2, this.area.h);
    }

    /* Slider dragging (adapted from UISliderTrackpad) */

    private void applySliderValue(double value)
    {
        if (this.delayedInput)
        {
            this.setValue(value);
        }
        else
        {
            this.setValueAndNotify(value);
        }
    }

    /**
     * Scrub the value by the mouse delta since the last update. Incremental (not absolute) so that
     * holding Alt can scale the per-pixel sensitivity down for fine adjustment — the rest-state
     * factor is 1:1 with pixels, so a normal drag still tracks the cursor. The accumulator
     * {@link #dragValue} stays a double so Alt steps and integer flooring don't lose sub-pixel motion.
     */
    private void updateDragging(int mouseX)
    {
        if (!this.hasSliderRange())
        {
            return;
        }

        int dx = mouseX - this.lastMouseX;

        this.lastMouseX = mouseX;

        if (dx == 0)
        {
            return;
        }

        double width = RefreshedSlider.trackWidth(this.area, this.railArea);
        double valuePerPixel = (this.max - this.min) / width;
        double sensitivity = Window.isAltPressed() ? FINE_DRAG_FACTOR : 1D;

        this.dragValue = MathUtils.clamp(this.dragValue + dx * valuePerPixel * sensitivity, this.min, this.max);
        this.applySliderValue(this.dragValue);
    }

    private void stopDragging()
    {
        this.sliderDragging = false;
    }

    private void cancelDragging()
    {
        this.setValueAndNotify(this.startValue);
        this.stopDragging();
    }

    private void finishDragging(int mouseX)
    {
        this.updateDragging(mouseX);
        this.updateHandleArea();

        if (this.delayedInput)
        {
            this.setValueAndNotify(this.value);
        }

        this.stopDragging();
    }

    private void beginDragging(UIContext context)
    {
        this.sliderDragging = true;
        this.startValue = this.value;
        this.lastMouseX = context.mouseX;

        if (this.handleArea.isInside(context))
        {
            /* Grabbed the knob — keep the current value and scrub relatively from here. */
            this.dragValue = this.value;
        }
        else
        {
            /* Clicked the bare rail — jump the knob to the cursor, then scrub from there. */
            this.dragValue = MathUtils.clamp(RefreshedSlider.valueFromMouse(this.area, this.railArea, context.mouseX, this.min, this.max), this.min, this.max);
            this.applySliderValue(this.dragValue);
        }

        this.updateHandleArea();
    }

    /* Input — slider when bounded, otherwise the inherited trackpad pipeline */

    @Override
    public void resize()
    {
        super.resize();

        if (this.hasSliderRange())
        {
            this.layoutSlider();
            this.updateHandleArea();
        }
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (!this.hasSliderRange())
        {
            return super.subMouseClicked(context);
        }

        if (this.allowCanceling && context.mouseButton == 1 && this.sliderDragging)
        {
            this.cancelDragging();

            return true;
        }

        /* Right click while not dragging — reset to the value loaded from data (default). */
        if (context.mouseButton == 1 && this.hasDefault() && this.area.isInside(context))
        {
            this.resetToDefault(context);

            return true;
        }

        if (context.mouseButton == 2 && this.area.isInside(context))
        {
            this.setValueAndNotify(-this.value);

            return true;
        }

        if (context.mouseButton != 0)
        {
            return false;
        }

        /* Right number box — focus native text editing and place the caret. */
        if (RefreshedSlider.hasNumberBox(this.area) && this.textbox.area.isInside(context))
        {
            if (!this.textbox.isFocused())
            {
                context.focus(this);
            }

            this.textbox.mouseClicked(context.mouseX, context.mouseY, context.mouseButton);

            return true;
        }

        /* Anywhere else (the rail strip) — leave the box if we were editing, then scrub. */
        if (this.textbox.isFocused())
        {
            context.unfocus();
        }

        this.updateHandleArea();

        if (this.area.isInside(context))
        {
            if (Window.isCtrlPressed())
            {
                this.setValueAndNotify(Math.round(this.value));

                return true;
            }

            this.beginDragging(context);

            return true;
        }

        return false;
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (!this.hasSliderRange())
        {
            return super.subMouseReleased(context);
        }

        if (context.mouseButton == 1 && this.sliderDragging)
        {
            this.cancelDragging();

            return true;
        }

        if (context.mouseButton == 0 && this.sliderDragging)
        {
            this.finishDragging(context.mouseX);

            return true;
        }

        return false;
    }

    @Override
    protected boolean subMouseScrolled(UIContext context)
    {
        if (!this.hasSliderRange())
        {
            return super.subMouseScrolled(context);
        }

        if (this.sliderDragging)
        {
            return true;
        }

        if (this.area.isInside(context) && context.hasNotScrolledForMore(500) && BBSSettings.enableTrackpadScrolling.get())
        {
            if (context.mouseWheel > 0)
            {
                this.setValueAndNotify(this.value + this.getValueModifier());
            }
            else
            {
                this.setValueAndNotify(this.value - this.getValueModifier());
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean subKeyPressed(UIContext context)
    {
        if (!this.hasSliderRange())
        {
            return super.subKeyPressed(context);
        }

        /* Editing the number box — let the native textbox handle typing, caret, enter, etc. */
        if (this.textbox.isFocused())
        {
            return super.subKeyPressed(context);
        }

        if (this.sliderDragging && context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.cancelDragging();

            return true;
        }

        if (this.area.isInside(context))
        {
            if (context.isHeld(GLFW.GLFW_KEY_UP))
            {
                this.setValueAndNotify(this.value + this.getValueModifier());

                return true;
            }
            else if (context.isHeld(GLFW.GLFW_KEY_DOWN))
            {
                this.setValueAndNotify(this.value - this.getValueModifier());

                return true;
            }
            else if (context.isPressed(GLFW.GLFW_KEY_MINUS) || context.isPressed(GLFW.GLFW_KEY_KP_SUBTRACT))
            {
                this.setValueAndNotify(-this.value);

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean subTextInput(UIContext context)
    {
        if (!this.hasSliderRange())
        {
            return super.subTextInput(context);
        }

        /* Only the focused number box accepts typed characters (the rail itself is not a text field). */
        if (this.textbox.isFocused())
        {
            return super.subTextInput(context);
        }

        return false;
    }

    /* Rendering */

    @Override
    public void render(UIContext context)
    {
        if (!this.hasSliderRange())
        {
            super.render(context);

            return;
        }

        if (this.railArea.w <= 0)
        {
            this.layoutSlider();
        }

        if (this.sliderDragging)
        {
            this.updateDragging(context.mouseX);
        }

        this.updateHandleArea();

        boolean knobHot = this.sliderDragging || this.handleArea.isInside(context);

        RefreshedSlider.renderTrack(context, this.area, this.railArea, true, this.getHandleCenter(), knobHot);

        /* Number box (right) — manual entry. Focused: native editing (bg via TextboxMixin + caret +
         * focus underline). Otherwise: our themed field with the value centred. */
        if (this.textbox.isFocused())
        {
            this.textbox.render(context);
        }
        else
        {
            String label = this.forcedLabel == null ? UITrackpad.format(this.value) : this.forcedLabel.get();
            int color = this.textbox.getColor();

            if (RefreshedSlider.hasNumberBox(this.area))
            {
                RefreshedSlider.renderNumberBox(context, this.textbox.area, label, color);
            }
            else
            {
                RefreshedSlider.renderInlineValue(context, this.area, label, color);
            }
        }

        this.renderLockedArea(context);

        /* Reproduce UIElement.render's tooltip hook (we don't reach super here). */
        if (this.tooltip != null && this.area.isInside(context))
        {
            context.tooltip.set(context, this);
        }
    }
}
