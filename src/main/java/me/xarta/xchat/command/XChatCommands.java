
package me.xarta.xchat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.xarta.xchat.XChat;
import me.xarta.xchat.config.ConfigHandler;
import me.xarta.xchat.data.LastPmData;
import me.xarta.xchat.util.LegacyFormatter;
import me.xarta.xchat.util.LuckPermsHelper;
import me.xarta.xchat.util.MentionUtil;
import me.xarta.xchat.util.TemplateUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = XChat.MODID, value = Dist.DEDICATED_SERVER)
public class XChatCommands {

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)[&§][0-9A-FK-OR]");
    private static final UUID CONSOLE_UUID = new UUID(0L, 0L);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        for (String v : new String[] { "msg", "tell", "whisper", "w" }) {
            removeLiteral(d, v);
        }

        for (String alias : ConfigHandler.getAllPmAliases()) {
            registerPm(d, alias);
        }
        for (String alias : ConfigHandler.getAllReplyAliases()) {
            registerReply(d, alias);
        }
        for (String alias : ConfigHandler.getAllBroadcastAliases()) {
            registerBroadcast(d, alias);
        }
    }

    private static void removeLiteral(CommandDispatcher<CommandSourceStack> d, String name) {
        CommandNode<CommandSourceStack> root = d.getRoot();
        root.getChildren().removeIf(n -> n instanceof LiteralCommandNode && n.getName().equals(name));
    }

    private static void replaceLiteral(CommandDispatcher<CommandSourceStack> d, String name, LiteralArgumentBuilder<CommandSourceStack> builder) {
        removeLiteral(d, name);
        d.register(builder);
    }

    private static void registerPm(CommandDispatcher<CommandSourceStack> d, String alias) {
        String consoleId = ConfigHandler.safeGetString(ConfigHandler.CONSOLE_IDENTIFIER, "#CONSOLE");

        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(alias)
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            var names = ctx.getSource().getServer().getPlayerList().getPlayerNamesArray();
                            return net.minecraft.commands.SharedSuggestionProvider.suggest(names, b);
                        })
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    CommandSourceStack src = ctx.getSource();
                                    String targetName = StringArgumentType.getString(ctx, "target");
                                    String rawMsg = StringArgumentType.getString(ctx, "message");

                                    if (targetName.equalsIgnoreCase(consoleId)) {
                                        return processPmToConsole(src, rawMsg);
                                    }

                                    ServerPlayer target = src.getServer().getPlayerList().getPlayerByName(targetName);
                                    if (target == null) {
                                        Component off = LegacyFormatter.parse(ConfigHandler.TARGET_IS_OFFLINE_MESSAGE.get());
                                        if (src.getEntity() instanceof ServerPlayer sender) {
                                            sender.sendSystemMessage(off);
                                        } else {
                                            src.getServer().sendSystemMessage(off);
                                        }
                                        return 0;
                                    }

                                    if (src.getEntity() instanceof ServerPlayer sender && sender.getUUID().equals(target.getUUID())) {
                                        sender.sendSystemMessage(LegacyFormatter.parse(ConfigHandler.CANT_PM_YOURSELF_MESSAGE.get()));
                                        return 0;
                                    }

                                    return processPmToPlayer(src, target, rawMsg);
                                })))
                .then(Commands.literal(consoleId)
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    CommandSourceStack src = ctx.getSource();
                                    String rawMsg = StringArgumentType.getString(ctx, "message");
                                    return processPmToConsole(src, rawMsg);
                                })));

        replaceLiteral(d, alias, builder);
    }

    private static void registerReply(CommandDispatcher<CommandSourceStack> d, String alias) {
        d.register(Commands.literal(alias)
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            String rawMsg = StringArgumentType.getString(ctx, "message");
                            long now = System.currentTimeMillis();
                            long activeMillis = ConfigHandler.PM_ACTIVE_TIME.get() * 1000L;

                            if (src.getEntity() instanceof ServerPlayer sender) {
                                if (ConfigHandler.REPLY_PERMISSION_REQUIRED.get() && !LuckPermsHelper.hasPermission(sender, "xchat.reply")) {
                                    sender.sendSystemMessage(LegacyFormatter.parse(ConfigHandler.NO_REPLY_PERMISSION.get()));
                                    return 0;
                                }
                                var server = sender.server;
                                var lastSender = LastPmData.get(server).getLastSenderIfActive(sender.getUUID(), now, activeMillis);
                                if (lastSender == null) {
                                    sender.sendSystemMessage(LegacyFormatter.parse(ConfigHandler.NOBODY_TO_REPLY_MESSAGE.get()));
                                    return 0;
                                }
                                if (CONSOLE_UUID.equals(lastSender)) {
                                    return processPmToConsole(src, rawMsg);
                                }
                                ServerPlayer target = server.getPlayerList().getPlayer(lastSender);
                                if (target == null) {
                                    sender.sendSystemMessage(LegacyFormatter.parse(ConfigHandler.TARGET_IS_OFFLINE_MESSAGE.get()));
                                    return 0;
                                }
                                return processPmPlayerToPlayer(sender, target, rawMsg);
                            }

                            var server = src.getServer();
                            var lastSender = LastPmData.get(server).getLastSenderIfActive(CONSOLE_UUID, now, activeMillis);
                            if (lastSender == null || CONSOLE_UUID.equals(lastSender)) {
                                server.sendSystemMessage(LegacyFormatter.parse(ConfigHandler.NOBODY_TO_REPLY_MESSAGE.get()));
                                return 0;
                            }
                            ServerPlayer target = server.getPlayerList().getPlayer(lastSender);
                            if (target == null) {
                                server.sendSystemMessage(LegacyFormatter.parse(ConfigHandler.TARGET_IS_OFFLINE_MESSAGE.get()));
                                return 0;
                            }
                            return processPmToPlayer(src, target, rawMsg);
                        })));
    }

    private static void registerBroadcast(CommandDispatcher<CommandSourceStack> d, String alias) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(alias)
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            String raw = StringArgumentType.getString(ctx, "message");
                            return processBroadcast(src, raw);
                        }));
        replaceLiteral(d, alias, builder);
    }

    private static int processPmToConsole(CommandSourceStack src, String rawMsg) {
        final boolean fromPlayer = src.getEntity() instanceof ServerPlayer;
        final ServerPlayer sender = fromPlayer ? (ServerPlayer) src.getEntity() : null;

        if (fromPlayer) {
            if (ConfigHandler.PM_PERMISSION_REQUIRED.get() && !LuckPermsHelper.hasPermission(sender, "xchat.pm")) {
                sender.sendSystemMessage(LegacyFormatter.parse(ConfigHandler.NO_PM_PERMISSION.get()));
                return 0;
            }
        }

        final boolean allowColors = !fromPlayer
                || !ConfigHandler.COLOR_PERMISSION_REQUIRED.get()
                || LuckPermsHelper.hasPermission(sender, "xchat.color");
        final String text = allowColors ? rawMsg : stripLegacyCodes(rawMsg);

        final String consoleName = ConfigHandler.safeGetString(ConfigHandler.CONSOLE_FORMAT, "Console");

        String senderTemplate = ConfigHandler.PM_FORMAT_SENDER.get()
                .replace("%receiver-prefix%", "")
                .replace("%receiver%", consoleName)
                .replace("%receiver-suffix%", "")
                .replace("%message%", "{#MSG#}");

        String receiverTemplate = ConfigHandler.PM_FORMAT_RECEIVER.get()
                .replace("%receiver-prefix%", "")
                .replace("%receiver%", consoleName)
                .replace("%receiver-suffix%", "")
                .replace("%message%", "{#MSG#}");

        if (fromPlayer) {
            senderTemplate = senderTemplate
                    .replace("%sender-prefix%", safe(LuckPermsHelper.getPrefix(sender)))
                    .replace("%sender%", sender.getGameProfile().getName())
                    .replace("%sender-suffix%", safe(LuckPermsHelper.getSuffix(sender)));

            receiverTemplate = receiverTemplate
                    .replace("%sender-prefix%", safe(LuckPermsHelper.getPrefix(sender)))
                    .replace("%sender%", sender.getGameProfile().getName())
                    .replace("%sender-suffix%", safe(LuckPermsHelper.getSuffix(sender)));
        } else {
            senderTemplate = senderTemplate
                    .replace("%sender-prefix%", "")
                    .replace("%sender%", consoleName)
                    .replace("%sender-suffix%", "");

            receiverTemplate = receiverTemplate
                    .replace("%sender-prefix%", "")
                    .replace("%sender%", consoleName)
                    .replace("%sender-suffix%", "");
        }

        final Component msgComp = fromPlayer
                ? MentionUtil.buildMentionsComponent(sender, text, false)
                : LegacyFormatter.parse(text);

        Map<String, Component> inserts = new HashMap<>();
        inserts.put("{#MSG#}", msgComp);

        Component sComp = TemplateUtil.render(senderTemplate, inserts);
        Component rComp = TemplateUtil.render(receiverTemplate, inserts);

        if (fromPlayer) {
            sender.sendSystemMessage(sComp);
            src.getServer().sendSystemMessage(rComp);
            LastPmData.get(src.getServer()).markReceived(CONSOLE_UUID, sender.getUUID(), System.currentTimeMillis());
        } else {
            src.getServer().sendSystemMessage(sComp);
            LastPmData.get(src.getServer()).markReceived(CONSOLE_UUID, CONSOLE_UUID, System.currentTimeMillis());
        }
        return 1;
    }

    private static int processPmToPlayer(CommandSourceStack src, ServerPlayer target, String rawMsg) {
        if (src.getEntity() instanceof ServerPlayer sender) {
            return processPmPlayerToPlayer(sender, target, rawMsg);
        } else {
            final String consoleName = ConfigHandler.safeGetString(ConfigHandler.CONSOLE_FORMAT, "Console");

            String senderTemplate = ConfigHandler.PM_FORMAT_SENDER.get()
                    .replace("%sender-prefix%", "")
                    .replace("%sender%", consoleName)
                    .replace("%sender-suffix%", "")
                    .replace("%receiver-prefix%", safe(LuckPermsHelper.getPrefix(target)))
                    .replace("%receiver%", target.getGameProfile().getName())
                    .replace("%receiver-suffix%", safe(LuckPermsHelper.getSuffix(target)))
                    .replace("%message%", "{#MSG#}");

            String receiverTemplate = ConfigHandler.PM_FORMAT_RECEIVER.get()
                    .replace("%sender-prefix%", "")
                    .replace("%sender%", consoleName)
                    .replace("%sender-suffix%", "")
                    .replace("%receiver-prefix%", safe(LuckPermsHelper.getPrefix(target)))
                    .replace("%receiver%", target.getGameProfile().getName())
                    .replace("%receiver-suffix%", safe(LuckPermsHelper.getSuffix(target)))
                    .replace("%message%", "{#MSG#}");

            Component msgComp = LegacyFormatter.parse(rawMsg);
            Map<String, Component> inserts = new HashMap<>();
            inserts.put("{#MSG#}", msgComp);

            Component sComp = TemplateUtil.render(senderTemplate, inserts);
            Component rComp = TemplateUtil.render(receiverTemplate, inserts);

            src.getServer().sendSystemMessage(sComp);
            target.sendSystemMessage(rComp);

            LastPmData.get(src.getServer()).markReceived(target.getUUID(), CONSOLE_UUID, System.currentTimeMillis());
            return 1;
        }
    }

    private static int processPmPlayerToPlayer(ServerPlayer sender, ServerPlayer target, String rawMsg) {
        if (sender.getUUID().equals(target.getUUID())) {
            sender.sendSystemMessage(LegacyFormatter.parse(ConfigHandler.CANT_PM_YOURSELF_MESSAGE.get()));
            return 0;
        }

        boolean allowColors = !ConfigHandler.COLOR_PERMISSION_REQUIRED.get() || LuckPermsHelper.hasPermission(sender, "xchat.color");
        String text = allowColors ? rawMsg : stripLegacyCodes(rawMsg);

        String msgMarker = "{#MSG#}";

        String senderTemplate = ConfigHandler.PM_FORMAT_SENDER.get()
                .replace("%sender-prefix%", safe(LuckPermsHelper.getPrefix(sender)))
                .replace("%sender%", sender.getGameProfile().getName())
                .replace("%sender-suffix%", safe(LuckPermsHelper.getSuffix(sender)))
                .replace("%receiver-prefix%", safe(LuckPermsHelper.getPrefix(target)))
                .replace("%receiver%", target.getGameProfile().getName())
                .replace("%receiver-suffix%", safe(LuckPermsHelper.getSuffix(target)))
                .replace("%message%", msgMarker);

        String receiverTemplate = ConfigHandler.PM_FORMAT_RECEIVER.get()
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

    private static int processBroadcast(CommandSourceStack src, String rawMsg) {
        final boolean fromPlayer = src.getEntity() instanceof ServerPlayer;
        final ServerPlayer sender = fromPlayer ? (ServerPlayer) src.getEntity() : null;

        if (fromPlayer) {
            if (ConfigHandler.BROADCAST_PERMISSION_REQUIRED.get() && !LuckPermsHelper.hasPermission(sender, "xchat.broadcast")) {
                sender.sendSystemMessage(LegacyFormatter.parse(ConfigHandler.NO_BROADCAST_PERMISSION.get()));
                return 0;
            }
        }

        final boolean allowColors = !fromPlayer
                || !ConfigHandler.COLOR_PERMISSION_REQUIRED.get()
                || LuckPermsHelper.hasPermission(sender, "xchat.color");
        final String text = allowColors ? rawMsg : stripLegacyCodes(rawMsg);

        String template = ConfigHandler.BROADCAST_FORMAT.get()
                .replace("%sender-prefix%", fromPlayer ? safe(LuckPermsHelper.getPrefix(sender)) : "")
                .replace("%sender%", fromPlayer ? sender.getGameProfile().getName() : ConfigHandler.safeGetString(ConfigHandler.CONSOLE_FORMAT, "Console"))
                .replace("%sender-suffix%", fromPlayer ? safe(LuckPermsHelper.getSuffix(sender)) : "")
                .replace("%message%", "{#MSG#}");

        Component msgComp = fromPlayer ? MentionUtil.buildMentionsComponent(sender, text, false) : LegacyFormatter.parse(text);

        Map<String, Component> inserts = new HashMap<>();
        inserts.put("{#MSG#}", msgComp);

        Component finalMsg = TemplateUtil.render(template, inserts);

        src.getServer().getPlayerList().broadcastSystemMessage(finalMsg, false);
        src.getServer().sendSystemMessage(finalMsg);
        return 1;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    private static String stripLegacyCodes(String s) {
        if (s == null || s.isEmpty()) return s;
        return LEGACY_COLOR_PATTERN.matcher(s).replaceAll("");
    }
}