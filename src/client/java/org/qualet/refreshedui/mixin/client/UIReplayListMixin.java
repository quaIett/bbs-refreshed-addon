package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.ui.film.replays.UIReplayList;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import org.qualet.refreshedui.client.replays.INestedFolderList;
import org.qualet.refreshedui.client.ui.OverlaySizes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two edits to the base {@code UIReplayList} (both also apply to our {@code UIReplayListRefreshed} subclass,
 * since a mixin on a superclass applies to subclasses):
 * <ul>
 *   <li>the random-textures folder picker uses its fixed default overlay size (3.7), and</li>
 *   <li>the base (flat) private folder context actions {@code removeReplayCategory} / {@code openAddCategoryOverlay}
 *       are routed into the nested subclass implementation for our instances. This keeps the base menu's
 *       "Remove folder" / "Add folder" items (no duplication) while giving them nested semantics (lift
 *       contents to parent on remove; persist folder order + move current selection into the new folder on
 *       add). Non-nested (base) instances fall through to the stock behavior.</li>
 * </ul>
 */
@Mixin(UIReplayList.class)
public abstract class UIReplayListMixin
{
    @Redirect(
        method = "*",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlay;addOverlay(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlayPanel;IF)Lmchorse/bbs_mod/ui/framework/elements/overlay/UIOverlay;")
    )
    private UIOverlay refreshedui$defaultSize(UIContext context, UIOverlayPanel panel, int w, float h)
    {
        return OverlaySizes.sizeFor(panel) != null
            ? UIOverlay.addOverlay(context, panel)
            : UIOverlay.addOverlay(context, panel, w, h);
    }

    @Inject(method = "removeReplayCategory", at = @At("HEAD"), cancellable = true)
    private void refreshedui$nestedRemove(String normalizedName, CallbackInfo ci)
    {
        if (this instanceof INestedFolderList nested)
        {
            nested.refreshed$removeFolder(normalizedName);
            ci.cancel();
        }
    }

    @Inject(method = "openAddCategoryOverlay", at = @At("HEAD"), cancellable = true)
    private void refreshedui$nestedAddCategory(CallbackInfo ci)
    {
        if (this instanceof INestedFolderList nested)
        {
            nested.refreshed$openAddCategory();
            ci.cancel();
        }
    }
}
