package me.xarta.xchat.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class LegacyFormatter {
    private LegacyFormatter() {}

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        MutableComponent root = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(i + 1));
                if (!current.isEmpty()) {
                    root.append(Component.literal(current.toString()).setStyle(style));
                    current.setLength(0);
                }
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                if (fmt != null) {
                    style = applyFormatCode(style, fmt);
                    i++;
                } else {
                    current.append(c);
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            root.append(Component.literal(current.toString()).setStyle(style));
        }
        return root;
    }

    public static Style styleFromCodes(String codes) {
        if (codes == null || codes.isEmpty()) return Style.EMPTY;
        return applyCodesOver(codes, Style.EMPTY);
    }

    public static Style endStyleFromLegacy(String input, Style base) {
        if (base == null) base = Style.EMPTY;
        if (input == null || input.isEmpty()) return base;
        return applyCodesOver(input, base);
    }

    private static Style applyCodesOver(String text, Style base) {
        Style style = base;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                if (fmt != null) {
                    style = applyFormatCode(style, fmt);
                    i++;
                }
            }
        }
        return style;
    }

    private static Style applyFormatCode(Style style, ChatFormatting fmt) {
        if (fmt == ChatFormatting.RESET) {
            return Style.EMPTY;
        } else if (fmt.isColor()) {
            return style.withColor(fmt);
        } else {
            return switch (fmt) {
                case BOLD -> style.withBold(true);
                case ITALIC -> style.withItalic(true);
                case UNDERLINE -> style.withUnderlined(true);
                case STRIKETHROUGH -> style.withStrikethrough(true);
                case OBFUSCATED -> style.withObfuscated(true);
                default -> style;
            };
        }
    }
}