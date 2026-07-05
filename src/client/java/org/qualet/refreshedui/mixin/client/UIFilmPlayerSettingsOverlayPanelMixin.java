package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPlayerSettingsOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Regrid "Configure player": two-column pairs (Hunger|Health, Experience level|Experience progress),
 * full-width Mob recording radius, and Replace inventory / Apply to player side by side instead of
 * stacked full-width — matches the reference layout the user approved. Rebuilds {@code editor}'s
 * children at constructor TAIL, once the fields (hp, hunger, ...) already exist; the original scroll
 * view container is reused, just repopulated, so no scrolling is needed at the panel's fixed pixel
 * size (OverlaySizes). The two buttons carry their original stacked-layout marginTop (10/4) which would
 * misalign them side by side, so it's reset to 0 before they're placed in a row.
 */
@Mixin(UIFilmPlayerSettingsOverlayPanel.class)
public abstract class UIFilmPlayerSettingsOverlayPanelMixin
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$regrid(Film film, CallbackInfo ci)
    {
        UIFilmPlayerSettingsOverlayPanel self = (UIFilmPlayerSettingsOverlayPanel) (Object) this;

        self.replaceInventory.marginTop(0);
        self.applyToPlayer.marginTop(0);

        self.editor.removeAll();
        self.editor.add(
            UI.row(
                UI.column(UI.label(UIKeys.FILM_PLAYER_SETTINGS_HUNGER), self.hunger),
                UI.column(UI.label(UIKeys.FILM_PLAYER_SETTINGS_HP), self.hp)
            ),
            UI.row(
                UI.column(UI.label(UIKeys.FILM_PLAYER_SETTINGS_XP_LEVEL), self.xpLevel),
                UI.column(UI.label(UIKeys.FILM_PLAYER_SETTINGS_XP_PROGRESS), self.xpProgress)
            ),
            UI.column(UI.label(UIKeys.FILM_PLAYER_SETTINGS_MOB_RECORDING_RADIUS), self.mobRecordingRadius),
            UI.row(self.replaceInventory, self.applyToPlayer).marginTop(10)
        );
    }
}
