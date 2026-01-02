package me.xarta.xchat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ConfigHandler {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> FIRST_JOIN_FORMAT;
    public static final ModConfigSpec.ConfigValue<String> JOIN_FORMAT;
    public static final ModConfigSpec.ConfigValue<String> LEAVE_FORMAT;
    public static final ModConfigSpec.ConfigValue<String> CHAT_FORMAT;

    static {
        BUILDER.push("xChat Configuration");
        BUILDER.comment("You can change chat's appearance there.");

        FIRST_JOIN_FORMAT = BUILDER
                .comment("Message shown when player joins for the first time")
                .define("first-join-message", "%prefix%%player%%suffix% &fjoined the server for the first time!");

        JOIN_FORMAT = BUILDER
                .comment("Message shown when player joins")
                .define("join-message", "%prefix%%player%%suffix% &fjoined the server");

        LEAVE_FORMAT = BUILDER
                .comment("Message shown when player leaves")
                .define("leave-message", "%prefix%%player%%suffix% &fleft the server");

        CHAT_FORMAT = BUILDER
                .comment("How the chat should look like")
                        .define("chat-format", "%prefix%%player%%suffix%&7: &a%message%");

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}