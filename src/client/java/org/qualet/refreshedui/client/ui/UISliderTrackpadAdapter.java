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
 * fields, and a {@code @Redirect} on the {@code new UITrackpad(...)} call swaps in this adapter. The
 * slider visuals/input below are adapted from upstream {@code UISliderTrackpad} (which extends
 * {@code UIElement}, hence cannot be reused by inheritance here) — keep in sync if BBS changes it.</p>
 *
 * <p>The three layers (track / active progress / handle) are drawn fully opaque so they never show
 * through one another; hover/drag feedback is a colour change, not an alpha change. Progress and
 * handle are rounded to match the addon theme.</p>
 *
 * <p>When the range is not finite-both ({@code hasSliderRange()} is false) every overridden method
 * falls through to {@code super}, so unbounded / one-sided trackpads keep their original textbox
 * behaviour untouched.</p>
 */
public class UISliderTrackpadAdapter extends UITrackpad
{
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

    /* Slider geometry (adapted from UISliderTrackpad) */

    private int getHandleWidth()
    {
        return Math.min(Math.max(this.area.h / 3, 6), 10);
    }

    private int getHandlePadding()
    {
        return this.getHandleWidth() / 2;
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
        int handlePadding = this.getHandlePadding();
        int handleMinX = this.area.x + handlePadding;
        int handleMaxX = this.area.ex() - handlePadding;
        int handleRange = Math.max(handleMaxX - handleMinX, 0);

        return handleMinX + Math.round(handleRange * this.getProgress());
    }

    private void updateHandleArea()
    {
        if (!this.hasSliderRange())
        {
            this.handleArea.set(this.area.x, this.area.y, 0, this.area.h);

            return;
        }

        int handleWidth = this.getHandleWidth();
        int handleCenter = this.getHandleCenter();

        this.handleArea.set(handleCenter - handleWidth / 2, this.area.y, handleWidth, this.area.h);
    }

    private double getValueFromMouse(int mouseX)
    {
        int centerX = mouseX - this.dragOffsetX;
        int handlePadding = this.getHandlePadding();
        int left = this.area.x + handlePadding;
        int width = Math.max(this.area.w - handlePadding * 2, 1);
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

        this.updateHandleArea();

        if (this.sliderDragging)
        {
            this.updateDragging(context.mouseX);
            this.updateHandleArea();
        }

        IRoundedBatcher batcher = (IRoundedBatcher) context.batcher;
        float radius = UICornerRadii.buttonsAndTrackpads();
        int primary = Colors.opaque(BBSSettings.primaryColor.get());
        int fillX = MathUtils.clamp(this.getHandleCenter(), this.area.x, this.area.ex());
        int fillWidth = fillX - this.area.x;

        /* Track (inactive) — rounded input surface, matching the addon theme (see UITrackpadMixin). */
        RoundedAreas.renderRounded(this.area, context.batcher, BBSSettings.inputSurface(), radius);

        /* Active progress — opaque so the track does not show through; rounded on the left to match
         * the track, squared on the right where the handle sits over it. */
        if (fillWidth > 0)
        {
            batcher.roundedBoxSides(this.area.x, this.area.y, fillWidth, this.area.h, radius, primary, true, false);
        }

        /* Handle — opaque rounded pill; the accent lightened ~30% at rest, full white on hover/drag
         * (colour change, not alpha, so nothing shows through). */
        int handleColor = this.sliderDragging || this.handleArea.isInside(context)
            ? Colors.WHITE
            : Colors.mulRGB(primary, 1.3F);

        batcher.roundedBox(this.handleArea.x, this.handleArea.y, this.handleArea.w, this.handleArea.h, this.handleArea.w / 2F, handleColor);

        FontRenderer font = context.batcher.getFont();
        String label = this.forcedLabel == null ? UITrackpad.format(this.value) : this.forcedLabel.get();
        int lx = this.area.ex() - 6 - font.getWidth(label);
        int ly = this.area.my() - font.getHeight() / 2;

        context.batcher.text(label, lx, ly, Colors.WHITE);

        this.renderLockedArea(context);

        /* Reproduce UIElement.render's tooltip hook (we don't reach super here). */
        if (this.tooltip != null && this.area.isInside(context))
        {
            context.tooltip.set(context, this);
        }
    }
}
