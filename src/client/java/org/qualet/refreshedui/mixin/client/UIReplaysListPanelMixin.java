package org.qualet.refreshedui.mixin.client;

import java.util.List;
import java.util.function.Consumer;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplayList;
import mchorse.bbs_mod.ui.film.replays.UIReplaysListPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.qualet.refreshedui.client.replays.UIReplayListRefreshed;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Two edits to the main film-editor replays panel:
 * <ul>
 *   <li>rounds the toolbar bar background (3.2b), and</li>
 *   <li>substitutes the nested-folder {@link UIReplayListRefreshed} for the base {@code UIReplayList} at
 *       its single {@code new UIReplayList(...)} construction — the primary of two call sites (the other is
 *       the replays overlay picker, see {@code UIReplaysOverlayPanelMixin}).</li>
 * </ul>
 */
@Mixin(UIReplaysListPanel.class)
public abstract class UIReplaysListPanelMixin
{
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V")
    )
    private void refreshedui$roundBar(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        RoundedAreas.roundedBox(batcher, x1, y1, x2 - x1, y2 - y1, UICornerRadii.interfaceChrome(), color);
    }

    @Redirect(
        method = "<init>",
        at = @At(value = "NEW", target = "mchorse/bbs_mod/ui/film/replays/UIReplayList")
    )
    private UIReplayList refreshedui$nestedFolders(Consumer<List<Replay>> callback, Consumer<Form> formConsumer, UIFilmPanel panel)
    {
        return new UIReplayListRefreshed(callback, formConsumer, panel);
    }
}
