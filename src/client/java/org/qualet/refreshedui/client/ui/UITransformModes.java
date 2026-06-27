package org.qualet.refreshedui.client.ui;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcons;
import mchorse.bbs_mod.ui.framework.tooltips.LabelTooltip;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/**
 * Refreshed transform-mode selector — a {@link UIIcons} variant for the redesigned transform panel
 * (see {@code UITransformMixin}). It adds two behaviours the base widget's API can't express:
 * <ul>
 *   <li>a cell can be drawn horizontally <em>mirrored</em> — Rotate2 reuses the Rotate icon flipped,
 *       so the two rotations read as distinct without a new atlas glyph;</li>
 *   <li>a <em>right</em> click on a cell fires a separate callback (scale → uniform toggle,
 *       translate → local/global toggle), while left click still selects the mode.</li>
 * </ul>
 *
 * <p>It paints the refreshed look (rounded track + rounded primary active cell + contrast icon)
 * <em>inline</em> rather than relying on {@code UIIconsMixin}: that mixin targets the base
 * {@code renderSkin}, which this class overrides, so it would never run here. The drawing mirrors
 * {@code UIIconsMixin} exactly so the selector matches every other restyled {@code UIIcons}.</p>
 */
public class UITransformModes extends UIIcons
{
    private final List<Icon> icons = new ArrayList<>();
    private final List<IKey> tooltips = new ArrayList<>();
    private final List<Boolean> mirror = new ArrayList<>();

    /** Right-click handler: receives the cell index, returns whether it consumed the click. */
    private IntPredicate rightClick;

    public UITransformModes(Consumer<UIIcons> callback)
    {
        super(callback);
    }

    public UITransformModes addIcon(Icon icon, IKey tooltip, boolean mirrored)
    {
        this.icons.add(icon);
        this.tooltips.add(tooltip);
        this.mirror.add(mirrored);

        return this;
    }

    public UITransformModes onRightClick(IntPredicate rightClick)
    {
        this.rightClick = rightClick;

        return this;
    }

    @Override
    public int getCount()
    {
        return this.icons.size();
    }

    @Override
    public void setValue(int value)
    {
        if (!this.icons.isEmpty())
        {
            this.value = Math.max(0, Math.min(value, this.icons.size() - 1));
        }
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.isEnabled() && this.area.isInside(context) && !this.icons.isEmpty())
        {
            int index = this.indexAt(context.mouseX);

            if (context.mouseButton == 0)
            {
                if (index != this.value)
                {
                    this.value = index;
                    UIUtils.playClick();

                    if (this.callback != null)
                    {
                        this.callback.accept(this);
                    }
                }

                return true;
            }

            if (context.mouseButton == 1 && this.rightClick != null && this.rightClick.test(index))
            {
                UIUtils.playClick();

                return true;
            }
        }

        return super.subMouseClicked(context);
    }

    private int indexAt(int mouseX)
    {
        int count = this.icons.size();
        int index = (int) ((mouseX - this.area.x) / (this.area.w / (float) count));

        return Math.max(0, Math.min(index, count - 1));
    }

    @Override
    protected void renderSkin(UIContext context)
    {
        this.tooltip = null;

        int count = this.icons.size();

        if (count == 0)
        {
            return;
        }

        float radius = UICornerRadii.buttonsAndTrackpads();

        RoundedAreas.renderRounded(this.area, context.batcher, BBSSettings.deepSurface(), radius);

        float cellW = this.area.w / (float) count;
        int hovered = this.hover ? this.indexAt(context.mouseX) : -1;

        for (int i = 0; i < count; i++)
        {
            int x1 = this.area.x + (int) (i * cellW);
            int x2 = i == count - 1 ? this.area.ex() : this.area.x + (int) ((i + 1) * cellW);
            boolean active = i == this.value;
            boolean cellHover = i == hovered;

            if (active)
            {
                Area.SHARED.set(x1, this.area.y, x2 - x1, this.area.h);
                RoundedAreas.renderRounded(Area.SHARED, context.batcher, BBSSettings.primaryColor(Colors.A100), radius);
            }
            else if (cellHover)
            {
                context.batcher.box(x1, this.area.y, x2, this.area.ey(), BBSSettings.chromeSurface());
            }

            int color = active ? UIContrastColor.onPrimary() : (cellHover ? Colors.LIGHTEST_GRAY : Colors.setA(Colors.WHITE, 0.6F));
            Icon icon = this.icons.get(i);
            float cx = (x1 + x2) / 2F;
            float cy = this.area.my();

            if (this.mirror.get(i))
            {
                float w = icon.w;
                float h = icon.h;

                /* Same region as the icon, but with u1/u2 swapped → a horizontal flip (no mirrored
                 * atlas glyph needed). Batcher2D.icon() hard-codes u1=icon.x, u2=icon.x+icon.w, so we
                 * go straight to texturedBox to control the UVs. */
                context.batcher.texturedBox(
                    BBSModClient.getTextures().getTexture(icon.texture), color,
                    cx - w / 2F, cy - h / 2F, w, h,
                    icon.x + icon.w, icon.y, icon.x, icon.y + icon.h,
                    icon.textureW, icon.textureH
                );
            }
            else
            {
                context.batcher.icon(icon, color, cx, cy, 0.5F, 0.5F);
            }

            if (cellHover && this.tooltips.get(i) != null)
            {
                this.tooltip = new LabelTooltip(this.tooltips.get(i), Direction.TOP);
            }
        }
    }
}
