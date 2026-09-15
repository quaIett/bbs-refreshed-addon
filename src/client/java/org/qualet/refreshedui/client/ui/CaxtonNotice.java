package org.qualet.refreshedui.client.ui;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import org.qualet.refreshedui.RefreshedUiAddon;
import org.qualet.refreshedui.client.font.Fonts;

/**
 * "Install Caxton" notice. The refreshed design is drawn around the addon's smooth Caxton font, so without
 * Caxton the dashboard shows a dialog recommending it — once per game session, on the first dashboard open,
 * until the user picks "don't show again" (persisted in the hidden {@code caxton_notice_dismissed} value).
 */
public final class CaxtonNotice
{
    private static boolean shownThisSession;

    private CaxtonNotice()
    {}

    public static void maybeShow(UIContext context)
    {
        if (shownThisSession || context == null || Fonts.customFontEnabled())
        {
            return;
        }

        if (RefreshedUiAddon.caxtonNoticeDismissed == null || RefreshedUiAddon.caxtonNoticeDismissed.get())
        {
            return;
        }

        shownThisSession = true;

        UIOverlay.addOverlay(context, new UICaxtonNoticePanel());
    }

    public static void dismissForever()
    {
        if (RefreshedUiAddon.caxtonNoticeDismissed != null)
        {
            RefreshedUiAddon.caxtonNoticeDismissed.set(true);
        }
    }
}
