package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.UITransform;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.ITransformModes;
import org.qualet.refreshedui.client.ui.UITransformModes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redesigned transform editor ({@link UITransform}, the base behind {@code UIPropTransform} /
 * {@code UIDeltaPropTransform}). Stock layout is 4 always-visible rows (translate / scale / rotate /
 * rotate2), each an icon + three X/Y/Z trackpads. The refreshed layout collapses that to a single
 * mode at a time: a segmented {@link UITransformModes} selector on top picks translate / scale /
 * rotate / rotate2, and below it sit just the three X/Y/Z trackpads of the active mode, each with a
 * colored axis letter (the number itself goes neutral).
 *
 * <p>The two icons that carried a second function survive as right-click gestures on their selector
 * cell, mirroring how the original wired them onto the row icon:</p>
 * <ul>
 *   <li><b>Scale</b> — RMB toggles uniform scale. Routed through the real {@code toggleUniformScale()}
 *       (also called programmatically by {@code UIPropTransform}), and an {@code @Inject} TAIL on it
 *       rebuilds our layout, so user toggles and data-driven toggles stay in sync.</li>
 *   <li><b>Translate</b> — RMB toggles local/global space by firing {@code iconT}'s callback (set by
 *       {@code UIPropTransform} after this constructor runs; null elsewhere, so it's a no-op there).</li>
 * </ul>
 */
@Mixin(UITransform.class)
public abstract class UITransformMixin extends UIElement implements ITransformModes
{
    @Shadow public UITrackpad tx;
    @Shadow public UITrackpad ty;
    @Shadow public UITrackpad tz;
    @Shadow public UITrackpad sx;
    @Shadow public UITrackpad sy;
    @Shadow public UITrackpad sz;
    @Shadow public UITrackpad rx;
    @Shadow public UITrackpad ry;
    @Shadow public UITrackpad rz;
    @Shadow public UITrackpad r2x;
    @Shadow public UITrackpad r2y;
    @Shadow public UITrackpad r2z;

    @Shadow protected UIIcon iconT;
    @Shadow private boolean uniformScale;

    @Shadow protected abstract void toggleUniformScale();

    @Unique private UITransformModes refreshedui$selector;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$rebuildLayout(CallbackInfo ci)
    {
        UITransformModes selector = new UITransformModes((s) -> this.refreshedui$apply());

        selector.addIcon(Icons.ALL_DIRECTIONS, UIKeys.TRANSFORMS_TRANSLATE, false);
        selector.addIcon(Icons.SCALE, UIKeys.TRANSFORMS_SCALE, false);
        selector.addIcon(Icons.REFRESH, UIKeys.TRANSFORMS_ROTATE, false);
        selector.addIcon(Icons.REFRESH, UIKeys.TRANSFORMS_ROTATE2, true);
        selector.onRightClick((index) ->
        {
            if (index == 1)
            {
                this.toggleUniformScale();

                return true;
            }

            if (index == 0 && this.iconT != null && this.iconT.callback != null)
            {
                this.iconT.callback.accept(this.iconT);

                return true;
            }

            return false;
        });
        selector.h(UIConstants.CONTROL_HEIGHT);

        this.refreshedui$selector = selector;

        this.refreshedui$neutralizeNumbers();
        this.refreshedui$apply();
    }

    @Inject(method = "toggleUniformScale", at = @At("TAIL"))
    private void refreshedui$afterUniformToggle(CallbackInfo ci)
    {
        this.refreshedui$apply();
    }

    /** {@link ITransformModes} — gizmo hotkeys (G/S/R) make the selected tab follow. */
    @Override
    public void refreshedui$setMode(int mode)
    {
        if (this.refreshedui$selector == null
            || mode < 0
            || mode >= this.refreshedui$selector.getCount()
            || this.refreshedui$selector.getValue() == mode)
        {
            return;
        }

        this.refreshedui$selector.setValue(mode);
        this.refreshedui$apply();
    }

    @Unique
    private void refreshedui$apply()
    {
        this.removeAll();
        this.add(this.refreshedui$selector);

        int mode = this.refreshedui$selector.getValue();

        if (mode == 1 && this.uniformScale)
        {
            this.add(UI.row(2, 0, UIConstants.CONTROL_HEIGHT, this.refreshedui$axisLabel("", Colors.WHITE), this.sx));
        }
        else
        {
            UITrackpad[] trio = this.refreshedui$trio(mode);

            this.add(UI.row(2, 0, UIConstants.CONTROL_HEIGHT, this.refreshedui$axisLabel("X", Colors.RED), trio[0]));
            this.add(UI.row(2, 0, UIConstants.CONTROL_HEIGHT, this.refreshedui$axisLabel("Y", Colors.GREEN), trio[1]));
            this.add(UI.row(2, 0, UIConstants.CONTROL_HEIGHT, this.refreshedui$axisLabel("Z", Colors.BLUE), trio[2]));
        }

        UIElement parent = this.getParentContainer();

        if (parent != null)
        {
            parent.resize();
        }
        else
        {
            this.resize();
        }
    }

    @Unique
    private UITrackpad[] refreshedui$trio(int mode)
    {
        switch (mode)
        {
            case 1:
                return new UITrackpad[]{this.sx, this.sy, this.sz};
            case 2:
                return new UITrackpad[]{this.rx, this.ry, this.rz};
            case 3:
                return new UITrackpad[]{this.r2x, this.r2y, this.r2z};
            default:
                return new UITrackpad[]{this.tx, this.ty, this.tz};
        }
    }

    @Unique
    private UILabel refreshedui$axisLabel(String text, int color)
    {
        UILabel label = new UILabel(IKey.constant(text), color);

        label.labelAnchor(0.5F, 0.5F);
        label.wh(14, UIConstants.CONTROL_HEIGHT);

        return label;
    }

    @Unique
    private void refreshedui$neutralizeNumbers()
    {
        int neutral = Colors.WHITE;

        this.tx.textbox.setColor(neutral);
        this.ty.textbox.setColor(neutral);
        this.tz.textbox.setColor(neutral);
        this.sx.textbox.setColor(neutral);
        this.sy.textbox.setColor(neutral);
        this.sz.textbox.setColor(neutral);
        this.rx.textbox.setColor(neutral);
        this.ry.textbox.setColor(neutral);
        this.rz.textbox.setColor(neutral);
        this.r2x.textbox.setColor(neutral);
        this.r2y.textbox.setColor(neutral);
        this.r2z.textbox.setColor(neutral);
    }
}
