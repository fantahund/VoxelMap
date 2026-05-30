package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import com.mamiyaotaru.voxelmap.server.VoxelmapServerConfigManager;
import com.mojang.brigadier.context.CommandContext;
import java.nio.file.Path;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VoxelmapFabricModServer implements DedicatedServerModInitializer {
    private static final Logger LOGGER = LogManager.getLogger("VoxelMap");
    private Path configFile;
    private VoxelmapServerConfigManager configManager;

    @Override
    public void onInitializeServer() {
        FabricSettingsChannelHandler.initServer();
        configFile = FabricLoader.getInstance().getConfigDir().resolve("voxelmap.json");
        configManager = new VoxelmapServerConfigManager(configFile);
        loadInitialConfig();

        ServerPlayConnectionEvents.JOIN.register((listener, _, _) -> sendVoxelmapSettings("join", listener.getPlayer()));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, _, _) -> sendVoxelmapSettings("changelevel", player));
        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> dispatcher.register(Commands.literal("voxelmap")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("reload").executes(this::reloadCommand))));
    }

    private void loadInitialConfig() {
        try {
            configManager.loadOrCreate();
            LOGGER.info("Loaded VoxelMap server config from " + configManager.getConfigFile());
        } catch (Exception e) {
            LOGGER.error("Failed to load VoxelMap server config from " + configManager.getConfigFile() + ". Built-in defaults will be used.", e);
        }
    }

    private int reloadCommand(CommandContext<CommandSourceStack> context) {
        try {
            configManager.reload();
            int sentPackets = sendVoxelmapSettingsToAll(context.getSource().getServer(), "reload");
            context.getSource().sendSuccess(() -> Component.literal("Reloaded VoxelMap server config and sent settings to " + sentPackets + " player(s)."), false);
            LOGGER.info("Reloaded VoxelMap server config from " + configManager.getConfigFile());
            return sentPackets;
        } catch (Exception e) {
            LOGGER.error("Failed to reload VoxelMap server config from " + configManager.getConfigFile(), e);
            context.getSource().sendFailure(Component.literal("Failed to reload VoxelMap server config: " + e.getMessage()));
            return 0;
        }
    }

    private int sendVoxelmapSettingsToAll(MinecraftServer server, String event) {
        int sentPackets = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (sendVoxelmapSettings(event, player)) {
                sentPackets++;
            }
        }
        return sentPackets;
    }

    private boolean sendVoxelmapSettings(String event, ServerPlayer player) {
        if (player.level() == null) {
            LOGGER.warn("Skipping VoxelMap settings send for " + player.getName().getString() + " (" + event + "): player has no level");
            return false;
        }

        if (!ServerPlayNetworking.canSend(player, VoxelmapSettingsS2C.PACKET_ID)) {
            LOGGER.debug("Skipping VoxelMap settings send for " + player.getName().getString() + " (" + event + "): client cannot receive settings packet");
            return false;
        }

        String worldId = player.level().dimension().identifier().toString();
        String settingsJson = configManager.createSettingsJson(worldId);
        ServerPlayNetworking.send(player, new VoxelmapSettingsS2C(settingsJson));
        LOGGER.info("Sent VoxelMap settings to " + player.getName().getString() + " (" + event + ") for world " + worldId);
        return true;
    }
}
