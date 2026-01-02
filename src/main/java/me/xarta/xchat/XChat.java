package me.xarta.xchat;

import com.mojang.logging.LogUtils;
import me.xarta.xchat.config.ConfigHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@SuppressWarnings("unused")
@Mod(XChat.MODID) // Declare this class as mod's main class
public class XChat {

    public static final String MODID = "xchat"; // Define modification's ID
    public static final Logger LOGGER = LogUtils.getLogger(); // Create logger

    public XChat(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("xChat is initializing..."); // Print initialization message

        // Register config for the mod
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                ConfigHandler.SPEC,
                "xchat.toml"
        );

        LOGGER.info("xChat is on."); // Print success message
    }

}