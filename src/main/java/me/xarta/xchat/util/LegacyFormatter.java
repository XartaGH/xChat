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
                    if (fmt == ChatFormatting.RESET) {
                        style = Style.EMPTY;
                    } else if (fmt.isColor()) {
                        style = style.withColor(fmt);
                    } else {
                        switch (fmt) {
                            case BOLD -> style = style.withBold(true);
                            case ITALIC -> style = style.withItalic(true);
                            case UNDERLINE -> style = style.withUnderlined(true);
                            case STRIKETHROUGH -> style = style.withStrikethrough(true);
                            case OBFUSCATED -> style = style.withObfuscated(true);
                            default -> {}
                        }
                    }
                    i++;
                    continue;
                }
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            root.append(Component.literal(current.toString()).setStyle(style));
        }
        return root;
    }
}