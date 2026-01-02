
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

import com.electronwill.nightconfig.core.UnmodifiableConfig;

import java.util.Objects;

@EventBusSubscriber(modid = XChat.MODID, value = Dist.DEDICATED_SERVER)
public class XChatEvents {

    @SubscribeEvent
    @SuppressWarnings("resource")
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        String raw = event.getRawText();
        String playerName = sender.getGameProfile().getName();

        String prefix = LuckPermsHelper.getPrefix(sender);
        String suffix = LuckPermsHelper.getSuffix(sender);

        boolean rangeEnabled = ConfigHandler.RANGE_ENABLED.get();
        int localRange = ConfigHandler.LOCAL_RANGE.get();
        boolean localModeEnabled = rangeEnabled && localRange > 0;

        String sym = Objects.requireNonNullElse(ConfigHandler.GLOBAL_SYMBOL.get(), "!");
        boolean global = localModeEnabled && !sym.isEmpty() && raw.startsWith(sym);
        String text = global ? raw.substring(sym.length()).stripLeading() : raw;

        String mode = localModeEnabled ? (global ? "global" : "local") : "no-range";

        UnmodifiableConfig formats = ConfigHandler.CHAT_FORMATS.get();
        String group = Objects.requireNonNullElse(LuckPermsHelper.getPrimaryGroup(sender), "default");

        UnmodifiableConfig groupFormats = null;
        Object gf = formats.get(group);
        if (gf instanceof UnmodifiableConfig u) groupFormats = u;
        if (groupFormats == null) {
            Object defObj = formats.get("default");
            if (defObj instanceof UnmodifiableConfig u) groupFormats = u;
        }

        String template = null;
        if (groupFormats != null) {
            Object t = groupFormats.get(mode);
            if (t instanceof String s) template = s;
        }
        if (template == null) {
            Object defObj = formats.get("default");
            if (defObj instanceof UnmodifiableConfig def) {
                Object t = def.get(mode);
                if (t instanceof String s) template = s;
            }
            if (template == null) template = "%prefix%%player%%suffix%&7: &a%message%";
        }

        String rendered = template.replace("%player%", playerName).replace("%message%", text);
        rendered = rendered.replace("%prefix%", Objects.requireNonNullElse(prefix, ""));
        rendered = rendered.replace("%suffix%", Objects.requireNonNullElse(suffix, ""));

        Component msg = LegacyFormatter.parse(rendered);
        event.setCanceled(true);

        if (!localModeEnabled || global) {
            sender.server.getPlayerList().broadcastSystemMessage(msg, false);
        } else {
            double r2 = (double) localRange * (double) localRange;
            for (ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
                if (p.level().dimension().equals(sender.level().dimension()) && p.distanceToSqr(sender) <= r2) {
                    p.sendSystemMessage(msg);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        MinecraftServer server = sp.server;
        PlayerList pl = server.getPlayerList();

        boolean firstTime = JoinedOnceData.get(server).markAndCheckFirstJoin(sp.getUUID());
        String template = firstTime ? ConfigHandler.FIRST_JOIN_FORMAT.get() : ConfigHandler.JOIN_FORMAT.get();

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