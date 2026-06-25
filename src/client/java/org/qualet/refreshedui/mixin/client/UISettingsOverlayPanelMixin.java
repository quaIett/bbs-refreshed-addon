package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.settings.Settings;
import mchorse.bbs_mod.settings.ui.UISettingsOverlayPanel;
import mchorse.bbs_mod.settings.ui.UIValueMap;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.qualet.refreshedui.RefreshedUiAddon;
import org.qualet.refreshedui.client.ui.UIContrastColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Settings side panel inset (3.6): inset the side panel fill and divider by 1px so they sit inside
 * the new rounded-frame border drawn by the inherited {@link UISettingsOverlayPanel} super-call.
 * box ordinal 0 = side panel fill, ordinal 1 = divider.
 *
 * <p>3.9 — active module button draws a black icon over the primary highlight.</p>
 * <p>3.14 — append the addon's nested "refreshed" group (header + values) at the bottom of the
 * personalization view (BBS does not auto-render nested groups).</p>
 */
@Mixin(UISettingsOverlayPanel.class)
public abstract class UISettingsOverlayPanelMixin
{
    @Shadow
    private UIIcon currentModule;

    @Shadow
    @Final
    private Map<String, UIIcon> moduleButtons;

    @Shadow
    private Settings settings;

    @Shadow
    private ValueGroup category;

    @Shadow
    private String filter;

    @Shadow
    public UIScrollView options;

    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V", ordinal = 0)
    )
    private void refreshedui$insetSide(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        batcher.box(x1 + 1, y1, x2, y2 - 1, color);
    }

    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V", ordinal = 1)
    )
    private void refreshedui$insetDivider(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        batcher.box(x1, y1, x2, y2 - 1, color);
    }

    /** Active module: adaptive contrast icon (white/black by primary brightness) over the primary highlight. */
    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void refreshedui$blackenActiveModule(UIContext context, CallbackInfo ci)
    {
        int activeColor = UIContrastColor.onPrimary();

        for (UIIcon icon : this.moduleButtons.values())
        {
            icon.active(icon == this.currentModule).activeColor(activeColor);
        }
    }

    /**
     * 3.14 — render the nested "refreshed" group at the bottom of the personalization view: a section
     * header (divider + label) followed by the group's value rows. Only in the non-filtered view of
     * the bbs module's personalization category.
     */
    @Inject(method = "refresh", at = @At("TAIL"))
    private void refreshedui$appendRefreshedGroup(CallbackInfo ci)
    {
        ValueGroup group = RefreshedUiAddon.refreshedGroup;

        if (group == null || this.settings == null || this.category == null || !this.filter.isEmpty())
        {
            return;
        }

        if (!"bbs".equals(this.settings.getId()) || !"personalization".equals(this.category.getId()))
        {
            return;
        }

        this.options.add(new UISettingsOverlayPanel.UISectionHeader((UISettingsOverlayPanel) (Object) this, group));

        for (var value : group.getAll())
        {
            for (UIElement element : UIValueMap.create(value, (UIElement) (Object) this))
            {
                this.options.add(element);
            }
        }

        this.options.resize();
    }
}
