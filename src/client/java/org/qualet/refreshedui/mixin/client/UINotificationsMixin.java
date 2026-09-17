package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.notifications.Notification;
import mchorse.bbs_mod.ui.framework.notifications.UINotifications;
import mchorse.bbs_mod.utils.interps.Lerps;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Redesigns the toast stack as a rounded MD3 card — {@link BBSSettings#raisedSurface()} fill, the
 * notification's own accent colour as a 1px border, matching {@code UICaxtonNoticePanel}/{@code
 * UIOverlayPanelMixin} — and swaps its slide axis from horizontal to vertical: BBS slides each toast in
 * from off the right edge while pinning it near the top; here it drops down from off the top edge into
 * its stacked slot instead. {@link Notification#getFactor} (its own appear/hold/expire envelope) is
 * reused unchanged — only the axis its 0..1 output drives is swapped from x to y.
 */
@Mixin(UINotifications.class)
public abstract class UINotificationsMixin
{
    private static final int WIDTH = 300;
    private static final int TOP_MARGIN = 10;
    private static final int GAP = 10;
    private static final int PADDING = 8;
    private static final int LINE_MARGIN = 5;
    /** Extra clearance above the screen edge so a card is fully hidden, not just touching y=0. */
    private static final int OFFSCREEN_GAP = 20;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void refreshedui$render(UIContext context, CallbackInfo ci)
    {
        List<Notification> notifications = ((UINotifications) (Object) this).notifications;
        FontRenderer font = context.batcher.getFont();
        IRoundedBatcher rounded = (IRoundedBatcher) context.batcher;
        int radius = UICornerRadii.interfaceChrome();
        int lineHeight = font.getHeight() + LINE_MARGIN;
        int x = context.menu.width / 2 - WIDTH / 2;
        int restY = TOP_MARGIN;

        for (int i = notifications.size() - 1; i >= 0; i--)
        {
            Notification notification = notifications.get(i);
            List<String> splits = font.wrap(notification.message.get(), WIDTH - PADDING * 2);
            int h = PADDING * 2 + splits.size() * lineHeight - LINE_MARGIN;
            float factor = notification.getFactor(context.getTransition());
            int y = (int) Lerps.lerp(-h - OFFSCREEN_GAP, restY, factor);

            rounded.roundedFrame(x, y, WIDTH, h, radius, 1F, notification.background, BBSSettings.raisedSurface());

            int ly = y + PADDING;

            for (String line : splits)
            {
                context.batcher.textShadow(line, x + PADDING, ly, notification.color);

                ly += lineHeight;
            }

            restY += h + GAP;
        }

        ci.cancel();
    }
}
