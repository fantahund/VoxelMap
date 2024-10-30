package com.mamiyaotaru.voxelmap.fabric;

import com.google.gson.Gson;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking.Context;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;

public class VoxelmapSettingsChannelHandler implements ClientPlayNetworking.PlayPayloadHandler<VoxelmapSettingsS2C>, ClientConfigurationNetworking.ConfigurationPayloadHandler<VoxelmapSettingsS2C> {
    public VoxelmapSettingsChannelHandler() {
        PayloadTypeRegistry.playS2C().register(VoxelmapSettingsS2C.PACKET_ID, VoxelmapSettingsS2C.PACKET_CODEC);
        PayloadTypeRegistry.configurationS2C().register(VoxelmapSettingsS2C.PACKET_ID, VoxelmapSettingsS2C.PACKET_CODEC);

        ClientPlayNetworking.registerGlobalReceiver(VoxelmapSettingsS2C.PACKET_ID, this);
        ClientConfigurationNetworking.registerGlobalReceiver(VoxelmapSettingsS2C.PACKET_ID, this);
    }

    @Override
    public void receive(VoxelmapSettingsS2C payload, Context context) {
        parsePacket(payload);
    }

    @Override
    public void receive(VoxelmapSettingsS2C payload, net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context context) {
        parsePacket(payload);
    }

    private void parsePacket(VoxelmapSettingsS2C packet) {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> settings = new Gson().fromJson(packet.settingsJson(), java.util.Map.class);
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String setting = entry.getKey();
            Object value = entry.getValue();
            switch (setting) {
                case "worldName" -> {
                    if (value instanceof String worldName) {
                        Minecraft.getInstance().execute(() -> {
                            VoxelConstants.getLogger().info("Received world name from settings: " + worldName);
                            VoxelConstants.getVoxelMapInstance().newSubWorldName(worldName, true);
                        });
                    } else {
                        VoxelConstants.getLogger().warn("Invalid world name: " + value);
                    }
                }
                case "radarAllowed" -> VoxelConstants.getVoxelMapInstance().getRadarOptions().radarAllowed = (Boolean) value;
                case "radarMobsAllowed" -> VoxelConstants.getVoxelMapInstance().getRadarOptions().radarMobsAllowed = (Boolean) value;
                case "radarPlayersAllowed" -> VoxelConstants.getVoxelMapInstance().getRadarOptions().radarPlayersAllowed = (Boolean) value;
                case "cavesAllowed" -> VoxelConstants.getVoxelMapInstance().getMapOptions().cavesAllowed = (Boolean) value;
                case "minimapAllowed" -> VoxelConstants.getVoxelMapInstance().getMapOptions().minimapAllowed = (Boolean) value;
                case "worldmapAllowed" -> VoxelConstants.getVoxelMapInstance().getMapOptions().worldmapAllowed = (Boolean) value;
                case "waypointsAllowed" -> VoxelConstants.getVoxelMapInstance().getMapOptions().waypointsAllowed = (Boolean) value;
                case "deathWaypointAllowed" -> VoxelConstants.getVoxelMapInstance().getMapOptions().deathWaypointAllowed = (Boolean) value;
                case "teleportCommand" -> VoxelConstants.getVoxelMapInstance().getMapOptions().serverTeleportCommand = (String) value;
                default -> VoxelConstants.getLogger().warn("Unknown configuration option " + setting);
            }
        }
    }
}
