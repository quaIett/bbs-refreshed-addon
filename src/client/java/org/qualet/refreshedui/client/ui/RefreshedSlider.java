package org.qualet.refreshedui.client.ui;

import org.qualet.refreshedui.client.batcher.IRoundedBatcher;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * The one place our slider look lives: a thin rounded <i>rail</i> with a circular <i>knob</i>, and a
 * separate themed <i>number box</i> on the right for manual entry.
 *
 * <p>Two call sites share it so the two kinds of slider in the UI can never drift apart:
 * {@link UISliderTrackpadAdapter} (our {@code UITrackpad} subclass, swapped into form panels that
 * still declare their fields as trackpads) and {@code UISliderTrackpadMixin} (BBS 2.4's own
 * {@code UISliderTrackpad}, which by now backs the settings screen, keyframe factories, pose/model
 * editors and most form panels).</p>
 *
 * <p>Everything here is a pure function of the element's {@link Area}, so no layout state has to be
 * kept in sync — geometry can be recomputed wherever it is needed.</p>
 */
public final class RefreshedSlider
{
    /** Rail thickness (the thin track line); the clickable rail strip is full element height. */
    public static final int RAIL_HEIGHT = 4;
    /** Gap between the rail strip and the number box. */
    public static final int BOX_GAP = 6;
    /** Number-box width clamps (the right manual-entry field). */
    public static final int BOX_MIN_W = 24;
    public static final int BOX_MAX_W = 46;
    /** Below this much rail the number box is dropped — a 10px rail is worse than an inline value. */
    public static final int RAIL_MIN_W = 28;

    /** Knob fill at rest; brightens to white and grows by 1px on hover/drag. */
    private static final int KNOB_REST = 0xffe6e6ea;
    private static final int KNOB_SEGMENTS = 24;

    private RefreshedSlider()
    {}

    /* Geometry — all pure functions of the element area */

    public static int knobRadius(Area area)
    {
        return MathUtils.clamp(area.h / 3, 4, 5);
    }

    /**
     * Width of the right number box, or {@code 0} when the element is too narrow to carve one out
     * (see {@link #RAIL_MIN_W}) — narrow hosts like context menus fall back to an inline value.
     */
    public static int boxWidth(Area area)
    {
        int boxW = MathUtils.clamp(area.w / 4, BOX_MIN_W, BOX_MAX_W);

        boxW = Math.min(boxW, Math.max(BOX_MIN_W, area.w - BOX_GAP - 16));

        return area.w - boxW - BOX_GAP < RAIL_MIN_W ? 0 : boxW;
    }

    /** Whether this element is wide enough for the number box to exist as a separate click target. */
    public static boolean hasNumberBox(Area area)
    {
        return boxWidth(area) > 0;
    }

    /** The left strip that hosts the rail and the knob — the element minus the number box. */
    public static void rail(Area area, Area out)
    {
        int boxW = boxWidth(area);

        if (boxW <= 0)
        {
            out.copy(area);

            return;
        }

        out.set(area.x, area.y, Math.max(1, area.w - boxW - BOX_GAP), area.h);
    }

    /**
     * The right manual-entry field. Meant to become the host's {@code textbox.area}, so that clicking
     * it focuses native text editing.
     *
     * <p>Without room for a box (see {@link #boxWidth}) it mirrors the element — the text field still
     * covers it when focused, it just stops being a separate click target ({@link #hasNumberBox} is
     * the guard for that).</p>
     */
    public static void box(Area area, Area out)
    {
        int boxW = boxWidth(area);

        if (boxW <= 0)
        {
            out.copy(area);

            return;
        }

        out.set(area.ex() - boxW, area.y, boxW, area.h);
    }

    /** Carve the element into [ rail strip | gap | number box ] in one go. */
    public static void layout(Area area, Area rail, Area box)
    {
        rail(area, rail);
        box(area, box);
    }

    /** Travel available to the knob centre — the rail minus the half-knob padding on both ends. */
    public static int trackWidth(Area area, Area rail)
    {
        return Math.max(rail.w - knobRadius(area) * 2, 1);
    }

    public static int handleCenter(Area area, Area rail, float progress)
    {
        return rail.x + knobRadius(area) + Math.round(trackWidth(area, rail) * progress);
    }

    /** Absolute value under the cursor — the jump-to-click when grabbing the bare rail. */
    public static double valueFromMouse(Area area, Area rail, int mouseX, double min, double max)
    {
        int left = rail.x + knobRadius(area);
        double factor = MathUtils.clamp((mouseX - left) / (double) trackWidth(area, rail), 0D, 1D);

        return min + factor * (max - min);
    }

    /* Rendering */

    /**
     * Rail, progress fill and knob. {@code handleCenter} is passed in rather than recomputed so the
     * caller stays the authority on where the handle stands (hosts keep their own handle area).
     */
    public static void renderTrack(UIContext context, Area area, Area rail, boolean hasRange, int handleCenter, boolean knobHot)
    {
        IRoundedBatcher batcher = (IRoundedBatcher) context.batcher;

        int railH = Math.min(RAIL_HEIGHT, area.h);
        int railY = area.my() - railH / 2;
        float railRadius = railH / 2F;

        batcher.roundedBox(rail.x, railY, rail.w, railH, railRadius, BBSSettings.dividerColor());

        if (!hasRange)
        {
            return;
        }

        int primary = Colors.opaque(BBSSettings.primaryColor.get());
        int fillWidth = MathUtils.clamp(handleCenter, rail.x, rail.ex()) - rail.x;

        if (fillWidth > 0)
        {
            batcher.roundedBoxSides(rail.x, railY, fillWidth, railH, railRadius, primary, true, false);
        }

        float knobR = knobRadius(area) - 1.5F;

        batcher.filledCircle(handleCenter, area.my(), knobHot ? knobR + 1F : knobR, knobHot ? Colors.WHITE : KNOB_REST, KNOB_SEGMENTS);
    }

    /** The right manual-entry field, drawn while it is not focused (focused = native textbox render). */
    public static void renderNumberBox(UIContext context, Area box, String label, int color)
    {
        RoundedAreas.renderField(box, context.batcher, BBSSettings.inputSurface(), UICornerRadii.buttonsAndTrackpads());

        FontRenderer font = context.batcher.getFont();

        context.batcher.text(label, box.mx(font.getWidth(label)), box.my() - font.getHeight() / 2, color);
    }

    /**
     * No-track fallback — BBS allows a slider with infinite limits, where the drag is relative and
     * there is nothing to lay the value out along. Drawn as our plain input field instead, with the
     * value right-aligned the way the stock slider spells it.
     */
    public static void renderPlainField(UIContext context, Area area, String label, int color)
    {
        RoundedAreas.renderField(area, context.batcher, BBSSettings.inputSurface(), UICornerRadii.buttonsAndTrackpads());

        FontRenderer font = context.batcher.getFont();

        context.batcher.text(label, area.ex() - 6 - font.getWidth(label), area.my() - font.getHeight() / 2, color);
    }

    /** Narrow fallback — the value sits on the rail's right end over a small plate so it stays legible. */
    public static void renderInlineValue(UIContext context, Area area, String label, int color)
    {
        FontRenderer font = context.batcher.getFont();
        int w = font.getWidth(label);
        int x = area.ex() - 4 - w;
        int y = area.my() - font.getHeight() / 2;

        ((IRoundedBatcher) context.batcher).roundedBox(x - 3, y - 2, w + 6, font.getHeight() + 4, 3F, Colors.setA(Colors.A100, 0.5F));

        context.batcher.text(label, x, y, color);
    }
}
