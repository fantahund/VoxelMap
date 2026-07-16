package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import com.mamiyaotaru.voxelmap.server.VoxelmapServerConfigManager;
import com.mojang.brigadier.context.CommandContext;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class VoxelmapForgeServer {
    private static final Logger LOGGER = LogManager.getLogger("VoxelMap");

    private VoxelmapServerConfigManager configManager;
    private boolean configLoaded;

    public void init() {
        ServerStartingEvent.BUS.addListener(this::onServerStarting);
        RegisterCommandsEvent.BUS.addListener(this::onRegisterCommands);
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(this::onPlayerLoggedIn);
        PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(this::onPlayerChangedDimension);
    }

    private void onServerStarting(ServerStartingEvent event) {
        loadInitialConfig();
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("voxelmap")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("reload").executes(this::reloadCommand)));
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendVoxelmapSettings("join", player);
        }
    }

    private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendVoxelmapSettings("changedimension", player);
        }
    }

    private synchronized VoxelmapServerConfigManager getConfigManager() {
        if (configManager == null) {
            Path configFile = FMLPaths.CONFIGDIR.get().resolve("voxelmap.json");
            configManager = new VoxelmapServerConfigManager(configFile);
        }
        return configManager;
    }

    private synchronized void loadInitialConfig() {
        if (configLoaded) {
            return;
        }

        VoxelmapServerConfigManager manager = getConfigManager();
        try {
            manager.loadOrCreate();
            LOGGER.info("Loaded VoxelMap server config from " + manager.getConfigFile());
        } catch (Exception e) {
            LOGGER.error("Failed to load VoxelMap server config from " + manager.getConfigFile() + ". Built-in defaults will be used.", e);
        } finally {
            configLoaded = true;
        }
    }

    private int reloadCommand(CommandContext<CommandSourceStack> context) {
        VoxelmapServerConfigManager manager = getConfigManager();
        try {
            manager.reload();
            configLoaded = true;
            int sentPackets = sendVoxelmapSettingsToAll(context.getSource().getServer(), "reload");
            context.getSource().sendSuccess(() -> Component.literal("Reloaded VoxelMap server config and sent settings to " + sentPackets + " player(s)."), false);
            LOGGER.info("Reloaded VoxelMap server config from " + manager.getConfigFile());
            return sentPackets;
        } catch (Exception e) {
            LOGGER.error("Failed to reload VoxelMap server config from " + manager.getConfigFile(), e);
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
        loadInitialConfig();

        if (player.level() == null) {
            LOGGER.warn("Skipping VoxelMap settings send for " + player.getName().getString() + " (" + event + "): player has no level");
            return false;
        }

        if (ForgeSettingsPacketHandler.SETTINGS == null || !ForgeSettingsPacketHandler.SETTINGS.isRemotePresent(player.connection.getConnection())) {
            LOGGER.debug("Skipping VoxelMap settings send for " + player.getName().getString() + " (" + event + "): client cannot receive settings packet");
            return false;
        }

        String worldId = player.level().dimension().identifier().toString();
        String settingsJson = getConfigManager().createSettingsJson(worldId);
        ForgeSettingsPacketHandler.SETTINGS.send(new VoxelmapSettingsS2C(settingsJson), PacketDistributor.PLAYER.with(player));
        LOGGER.info("Sent VoxelMap settings to " + player.getName().getString() + " (" + event + ") for world " + worldId);
        return true;
    }
}
