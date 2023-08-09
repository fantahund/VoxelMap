package com.mamiyaotaru.voxelmap;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.Map;

public class VoxelmapSettingsChannelHandler implements ClientPlayNetworking.PlayChannelHandler {

    private static final Identifier CHANNEL_IDENTIFIER = new Identifier("voxelmap", "settings");

    public VoxelmapSettingsChannelHandler() {
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_IDENTIFIER, this);
    }
    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buffer, PacketSender responseSender) {
        buffer.readByte(); // ignore
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> settings = new Gson().fromJson(buffer.readString(), java.util.Map.class);
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String setting = entry.getKey();
            Object value = entry.getValue();
            switch (setting) {
                case "worldName" -> {
                    if (value != null) {
                        VoxelConstants.getVoxelMapInstance().newSubWorldName((String) value, true);
                    }
                }
                case "radarAllowed" ->
                        VoxelConstants.getVoxelMapInstance().getRadarOptions().radarAllowed = (Boolean) value;
                case "radarMobsAllowed" ->
                        VoxelConstants.getVoxelMapInstance().getRadarOptions().radarMobsAllowed = (Boolean) value;
                case "radarPlayersAllowed" ->
                        VoxelConstants.getVoxelMapInstance().getRadarOptions().radarPlayersAllowed = (Boolean) value;
                case "cavesAllowed" ->
                        VoxelConstants.getVoxelMapInstance().getMapOptions().cavesAllowed = (Boolean) value;
                case "teleportCommand" ->
                        VoxelConstants.getVoxelMapInstance().getMapOptions().serverTeleportCommand = (String) value;
                default -> VoxelConstants.getLogger().warn("Unknown configuration option " + setting);
            }
        }
    }
}
