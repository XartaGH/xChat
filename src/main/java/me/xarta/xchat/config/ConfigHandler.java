package me.xarta.xchat.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.Config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ConfigHandler {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> FIRST_JOIN_FORMAT;
    public static final ModConfigSpec.ConfigValue<String> JOIN_FORMAT;
    public static final ModConfigSpec.ConfigValue<String> LEAVE_FORMAT;
    public static final ModConfigSpec.ConfigValue<UnmodifiableConfig> CHAT_FORMATS;
    public static final ModConfigSpec.ConfigValue<Boolean> RANGE_ENABLED;
    public static final ModConfigSpec.ConfigValue<Integer> LOCAL_RANGE;
    public static final ModConfigSpec.ConfigValue<String> GLOBAL_SYMBOL;
    public static final ModConfigSpec.ConfigValue<Boolean> CHAT_PERMISSION_REQUIRED;
    public static final ModConfigSpec.ConfigValue<Boolean> COLOR_PERMISSION_REQUIRED;
    public static final ModConfigSpec.ConfigValue<String> NO_CHAT_PERMISSION_MESSAGE;

    public static final ModConfigSpec.ConfigValue<String> PM_FORMAT_SENDER;
    public static final ModConfigSpec.ConfigValue<String> PM_FORMAT_RECEIVER;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> PM_COMMANDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> REPLY_COMMANDS;

    public static final ModConfigSpec.ConfigValue<Boolean> PM_PERMISSION_REQUIRED;
    public static final ModConfigSpec.ConfigValue<String> NO_PM_PERMISSION;

    public static final ModConfigSpec.ConfigValue<Boolean> REPLY_PERMISSION_REQUIRED;
    public static final ModConfigSpec.ConfigValue<String> NO_REPLY_PERMISSION;

    public static final ModConfigSpec.ConfigValue<String> NOBODY_TO_REPLY_MESSAGE;
    public static final ModConfigSpec.ConfigValue<Integer> PM_ACTIVE_TIME;

    public static final ModConfigSpec.ConfigValue<String> TARGET_IS_OFFLINE_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> CANT_PM_YOURSELF_MESSAGE;

    public static final ModConfigSpec.ConfigValue<Boolean> MENTION_ENABLED;
    public static final ModConfigSpec.ConfigValue<Boolean> MENTION_PERMISSION_REQUIRED;
    public static final ModConfigSpec.ConfigValue<String> MENTION_SYMBOL;
    public static final ModConfigSpec.ConfigValue<String> MENTION_FORMAT;
    public static final ModConfigSpec.ConfigValue<Boolean> MENTION_SOUND;
    public static final ModConfigSpec.ConfigValue<Boolean> MENTION_TOOLTIP_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> MENTION_TOOLTIP_FORMAT;

    public static final ModConfigSpec.ConfigValue<Boolean> REPLYING_TO_ENABLED;
    public static final ModConfigSpec.ConfigValue<Boolean> REPLYING_TO_TOOLTIP_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> REPLYING_TO_TOOLTIP_FORMAT;

    public static final ModConfigSpec.ConfigValue<String> CONSOLE_IDENTIFIER;

    public static final String BASE_PM = "pm";
    public static final String BASE_REPLY = "r";

    private static final List<String> PM_COMMANDS_DEFAULT = List.of("msg", "tell", "whisper");
    private static final List<String> REPLY_COMMANDS_DEFAULT = List.of("reply");

    static {
        BUILDER.push("xChat Configuration");
        BUILDER.comment("You can change chat's appearance there.");

        FIRST_JOIN_FORMAT = BUILDER
                .comment("Message shown when player joins for the first time (leave empty to disable)")
                .define("first-join-message", "%prefix%%player%%suffix% &fjoined the server for the first time!");

        JOIN_FORMAT = BUILDER
                .comment("Message shown when player joins (leave empty to disable)")
                .define("join-message", "%prefix%%player%%suffix% &fjoined the server");

        LEAVE_FORMAT = BUILDER
                .comment("Message shown when player leaves (leave empty to disable)")
                .define("leave-message", "%prefix%%player%%suffix% &fleft the server");

        Config defaults = Config.inMemory();

        Config defDefault = Config.inMemory();
        defDefault.set("no-range", "%prefix%%player%%suffix%&7: &a%message%");
        defDefault.set("local", "&b[L] %prefix%%player%%suffix%&7: &a%message%");
        defDefault.set("global", "&6[G] %prefix%%player%%suffix%&7: &a%message%");
        defaults.set("default", defDefault);

        Config defGuardian = Config.inMemory();
        defGuardian.set("no-range", "%prefix%%player%%suffix%&f: &e%message%");
        defGuardian.set("local", "&b[L] %prefix%%player%%suffix%&f: &e%message%");
        defGuardian.set("global", "&6[G] %prefix%%player%%suffix%&f: &e%message%");
        defaults.set("guardian", defGuardian);

        Predicate<Object> validatorUC = o -> o instanceof UnmodifiableConfig;
        CHAT_FORMATS = BUILDER
                .define("chat-format", defaults, validatorUC);

        RANGE_ENABLED = BUILDER
                .comment("Whether the ranged mode of chat enabled")
                .define("range-enabled", true);

        LOCAL_RANGE = BUILDER
                .comment("What is the range of local chat, if ranged mode is enabled (must be > 0 and range-enabled: true)")
                .defineInRange("local-range", 100, 1, Integer.MAX_VALUE);

        GLOBAL_SYMBOL = BUILDER
                .comment("What symbol should be used for global chat")
                .define("global-symbol", "!");

        CHAT_PERMISSION_REQUIRED = BUILDER
                .comment("Whether permission xchat.chat required to chat")
                .define("chat-permission-required", true);

        COLOR_PERMISSION_REQUIRED = BUILDER
                .comment("Whether permission xchat.color required to use color codes (&0-&9, &a-&f, &k, &r, &n, &o, &m, &l)")
                .define("color-permission-required", true);

        NO_CHAT_PERMISSION_MESSAGE = BUILDER
                .comment("When user don't have xchat.chat permission and chat-permission-required = true")
                .define("no-permission-message", "&cYou don't have permission to use chat");

        PM_FORMAT_SENDER = BUILDER
                .comment("How does PM look like for sender")
                .define(
                "pm-format-sender",
                "&fYou &6-> %receiver-prefix%%receiver%%receiver-suffix%&7: &f%message%"
        );
        PM_FORMAT_RECEIVER = BUILDER
                .comment("How does PM look like for receiver")
                .define(
                "pm-format-receiver",
                "%sender-prefix%%sender%%sender-suffix% &6-> &fYou&7: &f%message%"
        );

        Predicate<String> aliasValidator = s -> s != null && s.matches("^[A-Za-z][A-Za-z0-9_\\-]{0,31}$");
        Predicate<Object> aliasListValidator = o -> {
            if (!(o instanceof List<?> list)) return false;
            for (Object e : list) {
                if (!(e instanceof String s) || !aliasValidator.test(s)) return false;
            }
            return true;
        };

        PM_COMMANDS = BUILDER
                .comment("What commands can be used excluding /pm for pm")
                .define("pm-commands", PM_COMMANDS_DEFAULT, aliasListValidator);

        PM_PERMISSION_REQUIRED = BUILDER
                .comment("Whether permission xchat.pm required for /pm")
                .define("pm-permission-required", true);

        NO_PM_PERMISSION = BUILDER
                .comment("Message shown when player has no permission for /pm")
                .define("no-pm-permission", "&cYou don't have permission to send private messages");

        REPLY_COMMANDS = BUILDER
                .comment("What commands can be used excluding /r for reply")
                .define("reply-commands", REPLY_COMMANDS_DEFAULT, aliasListValidator);

        REPLY_PERMISSION_REQUIRED = BUILDER
                .comment("Whether permission xchat.reply required for /r")
                .define("reply-permission-required", true);

        NO_REPLY_PERMISSION = BUILDER
                .comment("Message shown when player has no permission for /r")
                .define("no-reply-permission", "&cYou don't have permission to reply");

        NOBODY_TO_REPLY_MESSAGE = BUILDER
                .comment("Message shown when there's no target for /r")
                .define("nobody-to-reply-message", "&cNobody has PMd you in past 2 minutes");

        PM_ACTIVE_TIME = BUILDER
                .comment("Message shown when there's no active PM for reply")
                .defineInRange("pm-active-time", 120, 1, Integer.MAX_VALUE);

        TARGET_IS_OFFLINE_MESSAGE = BUILDER
                .comment("Message shown when target is offline")
                .define("target-is-offline-message", "&cThe receiver is offline");

        CANT_PM_YOURSELF_MESSAGE = BUILDER
                .comment("Message shown when player tries to PM themself")
                .define("cant-pm-yourself-message", "&cYou can't PM yourself");

        MENTION_ENABLED = BUILDER
                .comment("Whether mentioning is enabled")
                .define("mention-enabled", true);

        MENTION_PERMISSION_REQUIRED = BUILDER
                .comment("Whether permission xchat.mention is required for mentioning")
                .define("mention-permission-required", true);

        MENTION_SYMBOL = BUILDER
                .comment("What symbol is used for mentioning")
                .define("mention-symbol", "@");

        MENTION_FORMAT = BUILDER
                .comment("How should mention look like")
                .define("mention-format", "&a&l&n");

        MENTION_SOUND = BUILDER
                .comment("Whether mentioned player hears a click sound when they are mentioned")
                .define("mention-sound", true);

        MENTION_TOOLTIP_ENABLED = BUILDER
                .comment("Whether mention tooltip is enabled")
                .define("mention-tooltip-enabled", true);

        MENTION_TOOLTIP_FORMAT = BUILDER
                .comment("How does mention tooltip look like (%mentioned_player% is available)")
                .define("mention-tooltip-format", "&3Click to &f/pm &3%mentioned_player%");

        REPLYING_TO_ENABLED = BUILDER
                .comment("Whether replying to system is enabled")
                .define("replying-to-enabled", true);

        REPLYING_TO_TOOLTIP_ENABLED = BUILDER
                .comment("Whether replying to tooltip is enabled")
                .define("replying-to-tooltip-enabled", true);

        REPLYING_TO_TOOLTIP_FORMAT = BUILDER
                .comment("How does replying to tooltip look like (%player% is available)")
                .define("replying-to-tooltip-format", "&3Click to &f/pm &3%player%");

        CONSOLE_IDENTIFIER = BUILDER
                .comment("How to chat with console")
                .define("console-identifier", "#CONSOLE");

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static Set<String> getAllPmAliases() {
        Set<String> set = new LinkedHashSet<>();
        set.add(BASE_PM);
        set.addAll(safeGetList(PM_COMMANDS, PM_COMMANDS_DEFAULT));
        return set;
    }

    public static Set<String> getAllReplyAliases() {
        Set<String> set = new LinkedHashSet<>();
        set.add(BASE_REPLY);
        set.addAll(safeGetList(REPLY_COMMANDS, REPLY_COMMANDS_DEFAULT));
        return set;
    }


    public static String safeGetString(ModConfigSpec.ConfigValue<String> cv, String deflt) {
        try {
            String v = cv.get();
            return (v.isBlank()) ? deflt : v;
        } catch (IllegalStateException ex) {
            return deflt;
        }
    }


    private static List<String> safeGetList(ModConfigSpec.ConfigValue<List<? extends String>> cv, List<String> deflt) {
        try {
            var list = cv.get();
            return new ArrayList<>(list);
        } catch (IllegalStateException ex) {
            return deflt;
        }
    }
}