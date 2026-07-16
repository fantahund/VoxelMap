package com.mamiyaotaru.voxelmap.paper;

import com.mamiyaotaru.voxelmap.server.VoxelmapServerConfigManager;
import com.mamiyaotaru.voxelmap.server.VoxelmapSettingsPayloadEncoder;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class VoxelmapPaperPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private static final String SETTINGS_CHANNEL = "voxelmap:settings";

    private VoxelmapServerConfigManager configManager;

    @Override
    public void onEnable() {
        Path configFile = getDataFolder().toPath().resolve("voxelmap.json");
        configManager = new VoxelmapServerConfigManager(configFile);
        loadInitialConfig();

        getServer().getMessenger().registerOutgoingPluginChannel(this, SETTINGS_CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand command = getCommand("voxelmap");
        if (command == null) {
            getLogger().severe("Command 'voxelmap' is missing from plugin.yml");
        } else {
            command.setExecutor(this);
        }
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, SETTINGS_CHANNEL);
    }

    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        if (SETTINGS_CHANNEL.equals(event.getChannel())) {
            sendVoxelmapSettings("registerchannel", event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        sendVoxelmapSettingsNextTick("join", event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        sendVoxelmapSettingsNextTick("changeworld", event.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !"reload".equalsIgnoreCase(args[0])) {
            return false;
        }

        try {
            configManager.reload();
            int sentPackets = sendVoxelmapSettingsToAll("reload");
            sender.sendMessage("Reloaded VoxelMap server config and sent settings to " + sentPackets + " player(s).");
            getLogger().info("Reloaded VoxelMap server config from " + configManager.getConfigFile());
        } catch (Exception e) {
            getLogger().severe("Failed to reload VoxelMap server config from " + configManager.getConfigFile() + ": " + e.getMessage());
            sender.sendMessage("Failed to reload VoxelMap server config: " + e.getMessage());
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && "reload".startsWith(args[0])) {
            return List.of("reload");
        }
        return List.of();
    }

    private void loadInitialConfig() {
        try {
            configManager.loadOrCreate();
            getLogger().info("Loaded VoxelMap server config from " + configManager.getConfigFile());
        } catch (Exception e) {
            getLogger().severe("Failed to load VoxelMap server config from " + configManager.getConfigFile() + ". Built-in defaults will be used: " + e.getMessage());
        }
    }

    private void sendVoxelmapSettingsNextTick(String event, Player player) {
        Bukkit.getScheduler().runTask(this, () -> sendVoxelmapSettingsIfListening(event, player));
    }

    private int sendVoxelmapSettingsToAll(String event) {
        int sentPackets = 0;
        for (Player player : getServer().getOnlinePlayers()) {
            if (sendVoxelmapSettingsIfListening(event, player)) {
                sentPackets++;
            }
        }
        return sentPackets;
    }

    private boolean sendVoxelmapSettingsIfListening(String event, Player player) {
        if (!player.getListeningPluginChannels().contains(SETTINGS_CHANNEL)) {
            getLogger().fine("Skipping VoxelMap settings send for " + player.getName() + " (" + event + "): client has not registered " + SETTINGS_CHANNEL);
            return false;
        }

        return sendVoxelmapSettings(event, player);
    }

    private boolean sendVoxelmapSettings(String event, Player player) {
        String worldId = player.getWorld().getKey().toString();
        String settingsJson = configManager.createSettingsJson(worldId);
        player.sendPluginMessage(this, SETTINGS_CHANNEL, VoxelmapSettingsPayloadEncoder.encode(settingsJson));
        getLogger().info("Sent VoxelMap settings to " + player.getName() + " (" + event + ") for world " + worldId);
        return true;
    }
}
