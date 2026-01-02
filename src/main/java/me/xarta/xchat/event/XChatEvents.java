package me.xarta.xchat.event;

import me.xarta.xchat.XChat;
import me.xarta.xchat.config.ConfigHandler;
import me.xarta.xchat.data.JoinedOnceData;
import me.xarta.xchat.util.LegacyFormatter;
import me.xarta.xchat.util.LuckPermsHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = XChat.MODID, value = Dist.DEDICATED_SERVER)
public class XChatEvents {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();

        String raw = event.getRawText();
        String playerName = sender.getGameProfile().getName();

        String prefix = LuckPermsHelper.getPrefix(sender);
        String suffix = LuckPermsHelper.getSuffix(sender);

        String template = ConfigHandler.CHAT_FORMAT.get();
        String rendered = template
                .replace("%player%", playerName)
                .replace("%message%", raw);

        rendered = rendered.replace("%prefix%", java.util.Objects.requireNonNullElse(prefix, ""));
        rendered = rendered.replace("%suffix%", java.util.Objects.requireNonNullElse(suffix, ""));

        Component msg = LegacyFormatter.parse(rendered);

        event.setCanceled(true);
        PlayerList pl = sender.server.getPlayerList();
        pl.broadcastSystemMessage(msg, false);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        MinecraftServer server = sp.server;
        PlayerList pl = server.getPlayerList();

        boolean firstTime = JoinedOnceData.get(server).markAndCheckFirstJoin(sp.getUUID());
        String template = firstTime
                ? ConfigHandler.FIRST_JOIN_FORMAT.get()
                : ConfigHandler.JOIN_FORMAT.get();

        String player = sp.getGameProfile().getName();
        String prefix = LuckPermsHelper.getPrefix(sp);
        String suffix = LuckPermsHelper.getSuffix(sp);

        String rendered = template.replace("%player%", player);
        rendered = (prefix != null) ? rendered.replace("%prefix%", prefix) : rendered.replace("%prefix%", "");
        rendered = (suffix != null) ? rendered.replace("%suffix%", suffix) : rendered.replace("%suffix%", "");

        pl.broadcastSystemMessage(LegacyFormatter.parse(rendered), false);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        String player = sp.getGameProfile().getName();
        String prefix = LuckPermsHelper.getPrefix(sp);
        String suffix = LuckPermsHelper.getSuffix(sp);

        String rendered = ConfigHandler.LEAVE_FORMAT.get().replace("%player%", player);
        rendered = (prefix != null) ? rendered.replace("%prefix%", prefix) : rendered.replace("%prefix%", "");
        rendered = (suffix != null) ? rendered.replace("%suffix%", suffix) : rendered.replace("%suffix%", "");

        sp.server.getPlayerList().broadcastSystemMessage(LegacyFormatter.parse(rendered), false);
    }
}