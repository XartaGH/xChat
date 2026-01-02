package me.xarta.xchat.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.Config;

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

        Predicate<Object> validator = o -> o instanceof UnmodifiableConfig;
        CHAT_FORMATS = BUILDER.define("chat-format", defaults, validator);

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

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}