package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.onboarding.Onboarding;
import mchorse.bbs_mod.ui.onboarding.TourChapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Force-skips BBS 2.6 onboarding. On the first dashboard open (welcome not seen yet) the refreshed palette is
 * applied silently — primary {@code #e089ff}, secondary {@code #171717}, the editor layout stays the default —
 * and the welcome is marked seen, so the welcome wizard never appears. Tours (the popup hint chapters) are
 * cancelled at their single entry point {@code start}, which also covers "reset tours" from the settings.
 * Settings persist on their own ({@code Settings.postNotify} → {@code saveLater}).
 */
@Mixin(Onboarding.class)
public abstract class OnboardingMixin
{
    @Unique
    private static final int refreshedui$PRIMARY = 0xe089ff;

    @Unique
    private static final int refreshedui$SECONDARY = 0x171717;

    @Inject(method = "dashboardOpened", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$skipWelcome(UIDashboard dashboard, CallbackInfo ci)
    {
        if (!BBSSettings.onboardingWelcomeSeen.get())
        {
            BBSSettings.primaryColor.set(refreshedui$PRIMARY);
            BBSSettings.secondaryColor.set(refreshedui$SECONDARY);
            BBSSettings.onboardingWelcomeSeen.set(true);
        }

        ci.cancel();
    }

    @Inject(method = "showWelcome", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$noWelcome(UIContext context, CallbackInfo ci)
    {
        ci.cancel();
    }

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private static void refreshedui$noTours(UIContext context, TourChapter chapter, CallbackInfo ci)
    {
        ci.cancel();
    }
}
