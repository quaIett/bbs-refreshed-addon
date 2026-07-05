package org.qualet.refreshedui.mixin.client;

import java.util.List;
import java.util.function.Consumer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.ReplayListEntry;
import mchorse.bbs_mod.ui.film.replays.UIReplayList;
import mchorse.bbs_mod.ui.film.replays.overlays.UIReplaysOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import org.qualet.refreshedui.client.replays.UIReplayListRefreshed;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Three edits to the replays overlay picker:
 * <ul>
 *   <li>rounds the inner content surface (3.2b — the outer panel is rounded by inherited
 *       {@code UIOverlayPanel.renderBackground}; here we round the inner content area drawn after
 *       {@code super}, hence {@code ordinal = 0}), and</li>
 *   <li>substitutes the nested-folder {@link UIReplayListRefreshed} for the base {@code UIReplayList} at the
 *       overlay's {@code new UIReplayList(...)} — the second of two call sites. Here {@code formConsumer} is
 *       {@code null}; the subclass tolerates that exactly like upstream.</li>
 *   <li>the "right click here" empty-state hint is gated on {@code this.replays.getList().size() < 3}
 *       (upstream {@code UIReplaysOverlayPanel.renderBackground}). Folder header rows inflate that count for
 *       our nested list, so for our instances we substitute the count of actual replay entries.</li>
 * </ul>
 */
@Mixin(UIReplaysOverlayPanel.class)
public abstract class UIReplaysOverlayPanelMixin
{
    @Redirect(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/utils/Area;render(Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;I)V", ordinal = 0)
    )
    private void refreshedui$roundContent(Area area, Batcher2D batcher, int color)
    {
        RoundedAreas.renderRounded(area, batcher, color, UICornerRadii.interfaceChrome());
    }

    @Redirect(
        method = "<init>",
        at = @At(value = "NEW", target = "mchorse/bbs_mod/ui/film/replays/UIReplayList")
    )
    private UIReplayList refreshedui$nestedFolders(Consumer<List<Replay>> callback, Consumer<Form> formConsumer, UIFilmPanel panel)
    {
        return new UIReplayListRefreshed(callback, formConsumer, panel);
    }

    @ModifyExpressionValue(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Ljava/util/List;size()I")
    )
    private int refreshedui$countReplaysOnly(int original)
    {
        UIReplayList replays = ((UIReplaysOverlayPanel) (Object) this).replays;

        if (!(replays instanceof UIReplayListRefreshed))
        {
            return original;
        }

        int count = 0;

        for (ReplayListEntry entry : replays.getList())
        {
            if (entry.isReplay())
            {
                count++;
            }
        }

        return count;
    }
}
