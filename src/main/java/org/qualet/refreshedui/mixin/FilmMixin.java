package org.qualet.refreshedui.mixin;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import org.qualet.refreshedui.replays.IReplayFolderOrder;
import org.qualet.refreshedui.replays.ValueStringListAddon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a persisted, ordered {@code replay_category_order} value to {@link Film} so the nested-folder replay
 * list can remember the user's folder order (upstream 2.3.1 has only the unordered {@code replay_categories}
 * set). Registered as a child at constructor tail via {@code ValueGroup.add} so it serializes with the film.
 * Exposed through {@link IReplayFolderOrder}.
 */
@Mixin(Film.class)
public abstract class FilmMixin implements IReplayFolderOrder
{
    @Unique
    private ValueStringListAddon refreshed$replayCategoryOrder;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$addReplayCategoryOrder(CallbackInfo ci)
    {
        this.refreshed$replayCategoryOrder = new ValueStringListAddon("replay_category_order");

        ((ValueGroup) (Object) this).add(this.refreshed$replayCategoryOrder);
    }

    @Override
    public ValueStringListAddon refreshed$getReplayCategoryOrder()
    {
        return this.refreshed$replayCategoryOrder;
    }
}
