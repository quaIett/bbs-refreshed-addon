package org.qualet.refreshedui.replays;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Colored-text helpers ported from the fork's {@code StringUtils}, kept as an addon utility (BBS's own
 * {@code StringUtils} on the clean dep has neither method). Used ONLY for folder rows: the raw leaf name
 * may carry {@code [c} markup, which {@link #processColoredText(String)} turns into legacy {@code §} codes
 * and {@link #legacyToText(String)} converts into a styled {@link Text} for rendering.
 */
public final class RefreshedTextUtils
{
    /**
     * Convert BBS user-facing {@code [} color markup into legacy {@code §} codes ({@code \\[} escapes a
     * literal bracket). Returns the input unchanged when it contains no {@code [}.
     */
    public static String processColoredText(String text)
    {
        if (text == null || !text.contains("["))
        {
            return text == null ? "" : text;
        }

        StringBuilder builder = new StringBuilder();
        int i = 0;

        for (int c = text.length(); i < c; i++)
        {
            char character = text.charAt(i);

            if (character == '\\' && i < c - 1 && text.charAt(i + 1) == '[')
            {
                builder.append('[');
                i += 1;
            }
            else
            {
                builder.append(character == '[' ? "§" : character);
            }
        }

        return builder.toString();
    }

    /**
     * Convert a legacy formatted string (Minecraft {@code §} codes) into styled {@link Text}. Needed where
     * rendering a raw String would not interpret {@code §} formatting. Run {@link #processColoredText(String)}
     * first to turn {@code [} into {@code §}.
     */
    public static Text legacyToText(String legacy)
    {
        if (legacy == null || legacy.isEmpty())
        {
            return Text.empty();
        }

        MutableText out = Text.empty();
        StringBuilder segment = new StringBuilder();
        Style style = Style.EMPTY;
        boolean any = false;

        for (int i = 0, len = legacy.length(); i < len; i++)
        {
            char ch = legacy.charAt(i);

            if (ch == '§' && i + 1 < len)
            {
                if (segment.length() > 0)
                {
                    out = out.append(Text.literal(segment.toString()).setStyle(style));
                    segment.setLength(0);
                    any = true;
                }

                char code = Character.toLowerCase(legacy.charAt(i + 1));
                i += 1;

                Formatting fmt = Formatting.byCode(code);

                if (fmt == null)
                {
                    continue;
                }

                if (fmt == Formatting.RESET)
                {
                    style = Style.EMPTY;
                    continue;
                }

                if (fmt.isColor())
                {
                    /* Vanilla behavior: color code resets previous modifiers. */
                    style = Style.EMPTY.withFormatting(fmt);
                }
                else
                {
                    style = style.withFormatting(fmt);
                }

                continue;
            }

            segment.append(ch);
        }

        if (segment.length() > 0)
        {
            out = out.append(Text.literal(segment.toString()).setStyle(style));
            any = true;
        }

        return any ? out : Text.literal(legacy);
    }

    private RefreshedTextUtils()
    {}
}
