package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.film.replays.UIReplaysListPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import mchorse.bbs_mod.ui.utils.UIConstants;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Film-editor replays panel: rounds the toolbar bar background (3.2b) and shrinks the search
 * field from the bar's full height to the standard control height (same as the replay
 * properties' label/name textboxes), centred vertically in the bar. The field also keeps the
 * same 2px gap to the bar's right edge as it has to the add button on its left.
 */
@Mixin(UIReplaysListPanel.class)
public abstract class UIReplaysListPanelMixin
{
    /* Mirrors UIReplaysListPanel's private BAR_ICON_SIZE / BAR_ICON_MARGIN. */
    private static final int REFRESHEDUI$ICON_SIZE = 20;
    private static final int REFRESHEDUI$MARGIN = 2;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$searchHeight(CallbackInfo ci)
    {
        ((UIReplaysListPanel) (Object) this).search
            .w(1F, -REFRESHEDUI$ICON_SIZE - REFRESHEDUI$MARGIN * 2)
            .y(0.5F, 0)
            .h(UIConstants.CONTROL_HEIGHT)
            .anchorY(0.5F);
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V")
    )
    private void refreshedui$roundBar(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        RoundedAreas.roundedBox(batcher, x1, y1, x2 - x1, y2 - y1, UICornerRadii.interfaceChrome(), color);
    }
}
