package org.qualet.refreshedui.client.ui;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * A {@link UITrackpad} that renders and behaves as a horizontal slider whenever it has a finite
 * bounded range on both ends ({@code min} and {@code max}), otherwise it delegates to the normal
 * trackpad behaviour.
 *
 * <p>This exists because BBS form-property panels declare their fields as {@code UITrackpad} and a
 * mixin cannot change a field's type. By subclassing {@code UITrackpad} we stay assignable to those
 * fields, and a {@code @Redirect} on the {@code new UITrackpad(...)} call swaps in this adapter.</p>
 *
 * <p><b>Design (slider redesign):</b> a thin rounded <i>rail</i> with a circular <i>knob</i> on the
 * left, and a separate themed <i>number box</i> on the right for manual entry. The number box reuses
 * the inherited {@link UITrackpad#textbox} sub-element: in slider mode {@link #layoutSlider()} shrinks
 * {@code textbox.area} to just that right box, so clicking it focuses native text editing (caret,
 * parsing, live value update) — manual entry comes essentially for free. The rail handles slider
 * drag; the box handles typing.</p>
 *
 * <p>When the range is not finite-both ({@code hasSliderRange()} is false) every overridden method
 * falls through to {@code super}, so unbounded / one-sided trackpads keep their original textbox
 * behaviour untouched (and {@code textbox.area} stays the full element).</p>
 */
public class UISliderTrackpadAdapter extends UITrackpad
{
    /** Rail thickness (the thin track line); the clickable rail strip is full element height. */
    private static final int RAIL_HEIGHT = 4;
    /** Gap between the rail strip and the number box. */
    private static final int BOX_GAP = 6;
    /** Number-box width clamps (the right manual-entry field). */
    private static final int BOX_MIN_W = 24;
    private static final int BOX_MAX_W = 46;
    /** Knob fill at rest; brightens to white and grows by 1px on hover/drag. */
    private static final int KNOB_REST = 0xffe6e6ea;
    private static final int KNOB_SEGMENTS = 24;

    /** Left strip that hosts the rail + knob (the element minus the number box). */
    private final Area railArea = new Area();
    private final Area handleArea = new Area();

    private boolean sliderDragging;
    private double startValue;
    private int dragOffsetX;

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

    /* Slider geometry — all measured against railArea (the left strip), not the full element. */

    /** Carve the element into [ rail strip | gap | number box ]; the box becomes {@code textbox.area}. */
    private void layoutSlider()
    {
        int boxW = MathUtils.clamp(this.area.w / 4, BOX_MIN_W, BOX_MAX_W);
        boxW = Math.min(boxW, Math.max(BOX_MIN_W, this.area.w - BOX_GAP - 16));

        int railW = Math.max(1, this.area.w - boxW - BOX_GAP);

        this.railArea.set(this.area.x, this.area.y, railW, this.area.h);
        this.textbox.area.set(this.area.ex() - boxW, this.area.y, boxW, this.area.h);
    }

    private int getKnobRadius()
    {
        return MathUtils.clamp(this.area.h / 3, 4, 5);
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
        int pad = this.getKnobRadius();
        int minX = this.railArea.x + pad;
        int maxX = this.railArea.ex() - pad;
        int range = Math.max(maxX - minX, 0);

        return minX + Math.round(range * this.getProgress());
    }

    private void updateHandleArea()
    {
        if (!this.hasSliderRange())
        {
            this.handleArea.set(this.area.x, this.area.y, 0, this.area.h);

            return;
        }

        int r = this.getKnobRadius();

        this.handleArea.set(this.getHandleCenter() - r, this.area.y, r * 2, this.area.h);
    }

    private double getValueFromMouse(int mouseX)
    {
        int centerX = mouseX - this.dragOffsetX;
        int pad = this.getKnobRadius();
        int left = this.railArea.x + pad;
        int width = Math.max(this.railArea.w - pad * 2, 1);
        double factor = MathUtils.clamp((centerX - left) / (double) width, 0D, 1D);

        return this.min + factor * (this.max - this.min);
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

    private void updateDragging(int mouseX)
    {
        if (this.hasSliderRange())
        {
            this.applySliderValue(this.getValueFromMouse(mouseX));
        }
    }

    private void stopDragging()
    {
        this.sliderDragging = false;
        this.dragOffsetX = 0;
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
        this.dragOffsetX = this.handleArea.isInside(context) ? context.mouseX - this.handleArea.mx() : 0;

        this.updateDragging(context.mouseX);
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
        if (this.textbox.area.isInside(context))
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

        IRoundedBatcher batcher = (IRoundedBatcher) context.batcher;
        int primary = Colors.opaque(BBSSettings.primaryColor.get());

        /* Rail — a thin rounded track, vertically centred in the strip. */
        int railH = Math.min(RAIL_HEIGHT, this.area.h);
        int railY = this.area.my() - railH / 2;
        float railRadius = railH / 2F;

        batcher.roundedBox(this.railArea.x, railY, this.railArea.w, railH, railRadius, BBSSettings.dividerColor());

        int knobCx = this.getHandleCenter();
        int fillWidth = MathUtils.clamp(knobCx, this.railArea.x, this.railArea.ex()) - this.railArea.x;

        if (fillWidth > 0)
        {
            batcher.roundedBoxSides(this.railArea.x, railY, fillWidth, railH, railRadius, primary, true, false);
        }

        /* Knob — circular; near-white at rest, full white and 1px larger on hover/drag. */
        boolean knobHot = this.sliderDragging || this.handleArea.isInside(context);
        int knobR = this.getKnobRadius();

        batcher.filledCircle(knobCx, this.area.my(), knobHot ? knobR + 1F : knobR, knobHot ? Colors.WHITE : KNOB_REST, KNOB_SEGMENTS);

        /* Number box (right) — manual entry. Focused: native editing (bg via TextboxMixin + caret +
         * focus underline). Otherwise: our themed field with the value centred. */
        if (this.textbox.isFocused())
        {
            this.textbox.render(context);
        }
        else
        {
            RoundedAreas.renderField(this.textbox.area, context.batcher, BBSSettings.inputSurface(), UICornerRadii.buttonsAndTrackpads());

            FontRenderer font = context.batcher.getFont();
            String label = this.forcedLabel == null ? UITrackpad.format(this.value) : this.forcedLabel.get();
            int lx = this.textbox.area.mx(font.getWidth(label));
            int ly = this.textbox.area.my() - font.getHeight() / 2;

            context.batcher.text(label, lx, ly, Colors.WHITE);
        }

        this.renderLockedArea(context);

        /* Reproduce UIElement.render's tooltip hook (we don't reach super here). */
        if (this.tooltip != null && this.area.isInside(context))
        {
            context.tooltip.set(context, this);
        }
    }
}
