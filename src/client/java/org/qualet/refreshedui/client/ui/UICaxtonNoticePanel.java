package org.qualet.refreshedui.client.ui;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.framework.elements.utils.UIText;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;

/**
 * Material 3 dialog recommending Caxton (see {@link CaxtonNotice}): hero icon, headline, supporting text,
 * a link to the Caxton page and one filled action — "don't show again" — that stays locked with a countdown
 * for the first seconds so the message gets read. No title bar, grip or close strip: the dialog is its own
 * rounded container; Escape or a click outside still close it (for this session only).
 */
public class UICaxtonNoticePanel extends UIOverlayPanel
{
    public static final String LINK = "https://modrinth.com/mod/caxton";

    private static final String KEY = "refreshedui.caxton_notice.";

    private static final int WIDTH = 300;
    private static final int PAD = 18;
    private static final int RADIUS = 12;
    private static final int ICON = 24;
    private static final int HEADLINE_SCALE = 2;
    private static final int LOCK_MS = 10000;

    private static final int ON_SURFACE = 0xffe8e8e8;

    private final UIText body;
    private final UILink link;
    private final UIPillButton dismiss;

    public UICaxtonNoticePanel()
    {
        super(L10n.lang(KEY + "title"));

        this.title.setVisible(false);
        this.icons.setVisible(false);
        this.content.resetFlex().relative(this).xy(0, 0).w(1F).h(1F);

        int headlineY = PAD + ICON + 10;
        int bodyY = headlineY + 7 * HEADLINE_SCALE + 12;

        this.body = new UIText().text(L10n.lang(KEY + "body")).textAnchorX(0.5F).lineHeight(11);
        this.body.color(MaterialField.ON_SURFACE_VARIANT, false);
        this.body.relative(this.content).x(PAD).y(bodyY).w(1F, -PAD * 2);

        this.link = new UILink(L10n.lang(KEY + "link"), () -> UIUtils.openWebLink(LINK));
        this.link.relative(this.body).x(0.5F).y(1F, 8).h(14).anchorX(0.5F);

        this.dismiss = new UIPillButton(L10n.lang(KEY + "dismiss"), () ->
        {
            CaxtonNotice.dismissForever();
            this.close();
        });
        this.dismiss.relative(this.link).x(0.5F).y(1F, 16).h(20).anchorX(0.5F);

        this.content.add(new UIRenderable(this::renderHeader), this.body, this.link, this.dismiss);
    }

    @Override
    public boolean isResizable()
    {
        return false;
    }

    @Override
    public int getPreferredWidth()
    {
        return WIDTH;
    }

    @Override
    public int getContentHeight()
    {
        if (this.body.area.h <= 0)
        {
            return -1;
        }

        return this.dismiss.area.ey() + PAD - this.area.y;
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        IRoundedBatcher rounded = (IRoundedBatcher) context.batcher;
        /* Red, not primary: it is a warning, whatever colour the user themed the interface in */
        int outline = Colors.RED | Colors.A100;

        rounded.roundedFrame(this.area.x, this.area.y, this.area.w, this.area.h, RADIUS, 1F, outline, BBSSettings.raisedSurface());
    }

    private void renderHeader(UIContext context)
    {
        Batcher2D batcher = context.batcher;
        FontRenderer font = batcher.getFont();
        int primary = BBSSettings.primaryColor.get() | Colors.A100;

        batcher.scaledIcon(Icons.EXCLAMATION, primary, this.area.mx() - ICON / 2F, this.area.y + PAD, ICON);

        String headline = L10n.lang(KEY + "title").get();
        int width = font.getWidth(headline) * HEADLINE_SCALE;
        /* 1.21.11: the GUI stack is a 2D Matrix3x2fStack */
        Matrix3x2fStack stack = batcher.getContext().getMatrices();

        stack.pushMatrix();
        stack.translate(this.area.mx() - width / 2F, this.area.y + PAD + ICON + 10);
        stack.scale(HEADLINE_SCALE, HEADLINE_SCALE);
        batcher.text(headline, 0, 0, ON_SURFACE, false);
        stack.popMatrix();
    }

    /** MD3 inline link: primary text with an "external" glyph, underlined on hover. */
    private static class UILink extends UIElement
    {
        private static final int GLYPH = 12;

        private final IKey label;
        private final Runnable action;

        public UILink(IKey label, Runnable action)
        {
            this.label = label;
            this.action = action;
        }

        @Override
        public void resize()
        {
            this.w(Batcher2D.getDefaultTextRenderer().getWidth(this.label.get()) + 3 + GLYPH);

            super.resize();
        }

        @Override
        public boolean subMouseClicked(UIContext context)
        {
            if (context.mouseButton == 0 && this.area.isInside(context))
            {
                UIUtils.playClick();
                this.action.run();

                return true;
            }

            return super.subMouseClicked(context);
        }

        @Override
        public void render(UIContext context)
        {
            boolean hover = this.area.isInside(context);
            int color = BBSSettings.primaryColor.get() | Colors.A100;
            FontRenderer font = context.batcher.getFont();
            String text = this.label.get();
            int textW = font.getWidth(text);
            int y = this.area.my() - font.getHeight() / 2;

            if (hover)
            {
                context.requestCursor(GLFW.GLFW_HAND_CURSOR);
                color = Colors.mulRGB(color, 1.15F) | Colors.A100;
                context.batcher.box(this.area.x, y + font.getHeight() + 2, this.area.x + textW, y + font.getHeight() + 3, color);
            }

            context.batcher.text(text, this.area.x, y, color, false);
            context.batcher.scaledIcon(Icons.EXTERNAL, color, this.area.x + textW + 3, this.area.my() - GLYPH / 2F, GLYPH);

            super.render(context);
        }
    }

    /**
     * MD3 filled pill button. Locked for {@link #LOCK_MS} after it is first drawn: disabled colours
     * (on-surface 12% container, 38% label) and a seconds countdown after the label, clicks swallowed.
     */
    private static class UIPillButton extends UIElement
    {
        private static final int H_PAD = 16;

        private final IKey label;
        private final Runnable action;

        private long unlockAt;

        public UIPillButton(IKey label, Runnable action)
        {
            this.label = label;
            this.action = action;
        }

        private int secondsLeft()
        {
            if (this.unlockAt == 0L)
            {
                return LOCK_MS / 1000;
            }

            long left = this.unlockAt - System.currentTimeMillis();

            return left <= 0L ? 0 : (int) Math.ceil(left / 1000D);
        }

        @Override
        public void resize()
        {
            /* Sized for the widest countdown label, so the pill doesn't shrink when it unlocks */
            String widest = this.label.get() + " (" + LOCK_MS / 1000 + ")";

            this.w(Batcher2D.getDefaultTextRenderer().getWidth(widest) + H_PAD * 2);

            super.resize();
        }

        @Override
        public boolean subMouseClicked(UIContext context)
        {
            if (context.mouseButton == 0 && this.area.isInside(context))
            {
                if (this.unlockAt != 0L && this.secondsLeft() == 0)
                {
                    UIUtils.playClick();
                    this.action.run();
                }

                return true;
            }

            return super.subMouseClicked(context);
        }

        @Override
        public void render(UIContext context)
        {
            if (this.unlockAt == 0L)
            {
                this.unlockAt = System.currentTimeMillis() + LOCK_MS;
            }

            IRoundedBatcher rounded = (IRoundedBatcher) context.batcher;
            FontRenderer font = context.batcher.getFont();
            Area a = this.area;
            float radius = a.h / 2F;
            int seconds = this.secondsLeft();
            boolean hover = a.isInside(context);
            String text = this.label.get();
            int textColor;

            if (seconds > 0)
            {
                text += " (" + seconds + ")";
                rounded.roundedBox(a.x, a.y, a.w, a.h, radius, Colors.setA(ON_SURFACE, 0.12F));
                textColor = Colors.setA(ON_SURFACE, 0.38F);
            }
            else
            {
                int fill = BBSSettings.primaryColor.get() | Colors.A100;

                textColor = UIContrastColor.onPrimary();
                rounded.roundedBox(a.x, a.y, a.w, a.h, radius, fill);

                if (hover)
                {
                    /* MD3 state layer: on-primary at 8% */
                    rounded.roundedBox(a.x, a.y, a.w, a.h, radius, Colors.setA(textColor, 0.08F));
                    context.requestCursor(GLFW.GLFW_HAND_CURSOR);
                }
            }

            context.batcher.text(text, a.mx(font.getWidth(text)), a.my(font.getHeight()), textColor, false);

            super.render(context);
        }
    }
}
