
package me.xarta.xchat.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.xarta.xchat.XChat;
import me.xarta.xchat.config.ConfigHandler;
import me.xarta.xchat.data.LastPmData;
import me.xarta.xchat.util.LegacyFormatter;
import me.xarta.xchat.util.LuckPermsHelper;
import me.xarta.xchat.util.MentionUtil;
import me.xarta.xchat.util.TemplateUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = XChat.MODID, value = Dist.DEDICATED_SERVER)
public class XChatCommands {

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)[&§][0-9A-FK-OR]");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        for (String alias : ConfigHandler.getAllPmAliases()) {
            registerPm(d, alias);
        }
        for (String alias : ConfigHandler.getAllReplyAliases()) {
            registerReply(d, alias);
        }
    }

    private static void registerPm(CommandDispatcher<CommandSourceStack> d, String alias) {
        d.register(Commands.literal(alias)
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer sender = requirePlayer(ctx.getSource());
                                    if (sender == null) return 0;
                                    if (denyIfNoPermission(sender, ConfigHandler.PM_PERMISSION_REQUIRED.get(), "xchat.pm", ConfigHandler.NO_PM_PERMISSION.get())) return 0;

                                    Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "target");
                                    if (profiles.isEmpty()) {
                                        sendSystem(sender, ConfigHandler.TARGET_IS_OFFLINE_MESSAGE.get());
                                        return 0;
                                    }

                                    GameProfile gp = profiles.iterator().next();
                                    ServerPlayer target = sender.server.getPlayerList().getPlayerByName(gp.getName());
                                    if (target == null) {
                                        sendSystem(sender, ConfigHandler.TARGET_IS_OFFLINE_MESSAGE.get());
                                        return 0;
                                    }

                                    if (target.getUUID().equals(sender.getUUID())) {
                                        sendSystem(sender, ConfigHandler.CANT_PM_YOURSELF_MESSAGE.get());
                                        return 0;
                                    }

                                    String rawMsg = StringArgumentType.getString(ctx, "message");
                                    return processPm(sender, target, rawMsg);
                                }))));
    }

    private static void registerReply(CommandDispatcher<CommandSourceStack> d, String alias) {
        d.register(Commands.literal(alias)
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer sender = requirePlayer(ctx.getSource());
                            if (sender == null) return 0;
                            if (denyIfNoPermission(sender, ConfigHandler.REPLY_PERMISSION_REQUIRED.get(), "xchat.reply", ConfigHandler.NO_REPLY_PERMISSION.get())) return 0;

                            var server = sender.server;
                            long now = System.currentTimeMillis();
                            long activeMillis = ConfigHandler.PM_ACTIVE_TIME.get() * 1000L;
                            var lastSender = LastPmData.get(server).getLastSenderIfActive(sender.getUUID(), now, activeMillis);
                            if (lastSender == null) {
                                sendSystem(sender, ConfigHandler.NOBODY_TO_REPLY_MESSAGE.get());
                                return 0;
                            }

                            ServerPlayer target = server.getPlayerList().getPlayer(lastSender);
                            if (target == null) {
                                sendSystem(sender, ConfigHandler.TARGET_IS_OFFLINE_MESSAGE.get());
                                return 0;
                            }

                            String rawMsg = StringArgumentType.getString(ctx, "message");
                            return processPm(sender, target, rawMsg);
                        })));
    }

    private static ServerPlayer requirePlayer(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer sp)) {
            src.sendFailure(Component.literal("Only players can use this command"));
            return null;
        }
        return sp;
    }

    private static boolean denyIfNoPermission(ServerPlayer player, boolean required, String node, String denyMsg) {
        if (required && !LuckPermsHelper.hasPermission(player, node)) {
            sendSystem(player, denyMsg);
            return true;
        }
        return false;
    }

    private static int processPm(ServerPlayer sender, ServerPlayer target, String rawMsg) {
        boolean needColorPerm = ConfigHandler.COLOR_PERMISSION_REQUIRED.get();
        boolean allowColors = !needColorPerm || LuckPermsHelper.hasPermission(sender, "xchat.color");
        String text = allowColors ? rawMsg : stripLegacyCodes(rawMsg);

        String msgMarker = "{#MSG#}";

        String tmplS = ConfigHandler.PM_FORMAT_SENDER.get();
        String senderTemplate = tmplS
                .replace("%sender-prefix%", safe(LuckPermsHelper.getPrefix(sender)))
                .replace("%sender%", sender.getGameProfile().getName())
                .replace("%sender-suffix%", safe(LuckPermsHelper.getSuffix(sender)))
                .replace("%receiver-prefix%", safe(LuckPermsHelper.getPrefix(target)))
                .replace("%receiver%", target.getGameProfile().getName())
                .replace("%receiver-suffix%", safe(LuckPermsHelper.getSuffix(target)))
                .replace("%message%", msgMarker);

        String tmplR = ConfigHandler.PM_FORMAT_RECEIVER.get();
        String receiverTemplate = tmplR
                .replace("%sender-prefix%", safe(LuckPermsHelper.getPrefix(sender)))
                .replace("%sender%", sender.getGameProfile().getName())
                .replace("%sender-suffix%", safe(LuckPermsHelper.getSuffix(sender)))
                .replace("%receiver-prefix%", safe(LuckPermsHelper.getPrefix(target)))
                .replace("%receiver%", target.getGameProfile().getName())
                .replace("%receiver-suffix%", safe(LuckPermsHelper.getSuffix(target)))
                .replace("%message%", msgMarker);

        Component msgComp = MentionUtil.buildMentionsComponent(sender, text, false);

        Map<String, Component> inserts = new HashMap<>();
        inserts.put(msgMarker, msgComp);

        Component sComp = TemplateUtil.render(senderTemplate, inserts);
        Component rComp = TemplateUtil.render(receiverTemplate, inserts);

        sender.sendSystemMessage(sComp);
        target.sendSystemMessage(rComp);

        LastPmData.get(sender.server).markReceived(target.getUUID(), sender.getUUID(), System.currentTimeMillis());
        return 1;
    }

    private static void sendSystem(ServerPlayer player, String msgTemplate) {
        player.sendSystemMessage(LegacyFormatter.parse(msgTemplate));
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    private static String stripLegacyCodes(String s) {
        if (s == null || s.isEmpty()) return s;
        return LEGACY_COLOR_PATTERN.matcher(s).replaceAll("");
    }
}