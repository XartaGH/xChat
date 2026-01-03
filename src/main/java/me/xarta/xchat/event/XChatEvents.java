package me.xarta.xchat.event;

import me.xarta.xchat.XChat;
import me.xarta.xchat.config.ConfigHandler;
import me.xarta.xchat.data.JoinedOnceData;
import me.xarta.xchat.util.LegacyFormatter;
import me.xarta.xchat.util.LuckPermsHelper;
import me.xarta.xchat.util.MentionUtil;
import me.xarta.xchat.util.ReplyingUtil;
import me.xarta.xchat.util.TemplateUtil;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@EventBusSubscriber(modid = XChat.MODID, value = Dist.DEDICATED_SERVER)
public class XChatEvents {

    @SubscribeEvent
    @SuppressWarnings("resource")
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        String raw = event.getRawText();

        boolean needChatPerm = ConfigHandler.CHAT_PERMISSION_REQUIRED.get();
        if (needChatPerm && !LuckPermsHelper.hasPermission(sender, "xchat.chat")) {
            event.setCanceled(true);
            Component deny = LegacyFormatter.parse(ConfigHandler.NO_CHAT_PERMISSION_MESSAGE.get());
            sender.sendSystemMessage(deny);
            return;
        }

        boolean needColorPerm = ConfigHandler.COLOR_PERMISSION_REQUIRED.get();
        boolean allowColors = !needColorPerm || LuckPermsHelper.hasPermission(sender, "xchat.color");
        String processedRaw = allowColors ? raw : stripLegacyCodes(raw);

        String prefix = LuckPermsHelper.getPrefix(sender);
        String suffix = LuckPermsHelper.getSuffix(sender);

        boolean rangeEnabled = ConfigHandler.RANGE_ENABLED.get();
        int localRange = ConfigHandler.LOCAL_RANGE.get();
        boolean localModeEnabled = rangeEnabled && localRange > 0;

        String sym = Objects.requireNonNullElse(ConfigHandler.GLOBAL_SYMBOL.get(), "!");
        boolean global = localModeEnabled && !sym.isEmpty() && processedRaw.startsWith(sym);
        String text = global ? processedRaw.substring(sym.length()).stripLeading() : processedRaw;

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

        String nameMarker = "{#PLAYER#}";
        String msgMarker = "{#MSG#}";

        String templWithMarkers = template
                .replace("%player%", nameMarker)
                .replace("%message%", msgMarker)
                .replace("%prefix%", Objects.requireNonNullElse(prefix, ""))
                .replace("%suffix%", Objects.requireNonNullElse(suffix, ""));

        Component nameComp = ReplyingUtil.makeClickableName(sender);
        Component msgComp = MentionUtil.buildMentionsComponent(sender, text, true);

        Map<String, Component> inserts = new HashMap<>();
        inserts.put(nameMarker, nameComp);
        inserts.put(msgMarker, msgComp);

        Component finalMsg = TemplateUtil.render(templWithMarkers, inserts);
        event.setCanceled(true);

        if (!localModeEnabled || global) {
            sender.server.getPlayerList().broadcastSystemMessage(finalMsg, false);
        } else {
            double r2 = (double) localRange * (double) localRange;
            for (ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
                if (p.level().dimension().equals(sender.level().dimension()) && p.distanceToSqr(sender) <= r2) {
                    p.sendSystemMessage(finalMsg);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        MinecraftServer server = sp.server;
        boolean firstTime = JoinedOnceData.get(server).markAndCheckFirstJoin(sp.getUUID());
        String template = firstTime ? ConfigHandler.FIRST_JOIN_FORMAT.get() : ConfigHandler.JOIN_FORMAT.get();
        if (!template.isBlank()) {
            broadcastFormatted(sp.server.getPlayerList(), template, sp);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        String template = ConfigHandler.LEAVE_FORMAT.get();
        if (!template.isBlank()) {
            broadcastFormatted(sp.server.getPlayerList(), template, sp);
        }
    }

    private static void broadcastFormatted(PlayerList pl, String template, ServerPlayer sp) {
        String player = sp.getGameProfile().getName();
        String prefix = LuckPermsHelper.getPrefix(sp);
        String suffix = LuckPermsHelper.getSuffix(sp);
        String rendered = template.replace("%player%", player);
        rendered = (prefix != null) ? rendered.replace("%prefix%", prefix) : rendered.replace("%prefix%", "");
        rendered = (suffix != null) ? rendered.replace("%suffix%", suffix) : rendered.replace("%suffix%", "");
        Component msg = LegacyFormatter.parse(rendered);
        if (!msg.getString().isEmpty()) {
            pl.broadcastSystemMessage(msg, false);
        }
    }

    private static String stripLegacyCodes(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.replaceAll("(?i)[&§][0-9A-FK-OR]", "");
    }
}