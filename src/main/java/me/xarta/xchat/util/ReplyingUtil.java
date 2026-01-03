package me.xarta.xchat.util;

import me.xarta.xchat.config.ConfigHandler;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

public final class ReplyingUtil {
    private ReplyingUtil() {}

    public static Component makeClickableName(ServerPlayer player) {
        boolean enabled = ConfigHandler.REPLYING_TO_ENABLED.get();
        if (!enabled) {
            return Component.literal(player.getGameProfile().getName());
        }
        MutableComponent name = Component.literal(player.getGameProfile().getName())
                .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/pm " + player.getGameProfile().getName() + " ")));
        if (ConfigHandler.REPLYING_TO_TOOLTIP_ENABLED.get()) {
            String t = ConfigHandler.REPLYING_TO_TOOLTIP_FORMAT.get().replace("%player%", player.getGameProfile().getName());
            name = name.withStyle(name.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, LegacyFormatter.parse(t))));
        }
        return name;
    }
}