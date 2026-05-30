package com.mamiyaotaru.voxelmap.fabric;

import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VoxelmapFabricModServer implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger("VoxelMap");
    private Path configFile;

    @Override
    public void onInitialize() {
        FabricSettingsChannelHandler.initServer();
        ServerPlayConnectionEvents.JOIN.register((listener, _, _) -> sendVoxelmapSettings("join", listener.getPlayer()));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, _, _) -> sendVoxelmapSettings("changelevel", player));
        configFile = FabricLoader.getInstance().getConfigDir().resolve("voxelmap.json");
    }

    private void sendVoxelmapSettings(String event, ServerPlayer player) {
        LOGGER.info("Player changed world (" + event + "): " + (player.level() != null ? player.level().dimension().identifier().toString() : "null"));
    }
}
