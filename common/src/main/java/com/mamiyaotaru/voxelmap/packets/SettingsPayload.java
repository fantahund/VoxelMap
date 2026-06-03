package com.mamiyaotaru.voxelmap.packets;

import com.google.gson.Gson;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.options.ServerSettingsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Map;

public record SettingsPayload(String settingsJson) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SettingsPayload> PACKET_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "settings"));
    public static final StreamCodec<FriendlyByteBuf, SettingsPayload> PACKET_CODEC = StreamCodec.ofMember(SettingsPayload::encode, SettingsPayload::decode);

    public static SettingsPayload decode(FriendlyByteBuf buf) {
        buf.readByte();
        return new SettingsPayload(buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(0);
        buf.writeUtf(settingsJson);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

    public static void parsePacket(SettingsPayload packet) {
        @SuppressWarnings("unchecked")
        Map<String, Object> settings = new Gson().fromJson(packet.settingsJson(), Map.class);
        ServerSettingsManager serverSettings = VoxelConstants.getVoxelMapInstance().getServerSettings();
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            switch (key) {
                case "worldName" -> {
                    VoxelConstants.getVoxelMapInstance().newSubWorldName((String) value, true);
                    VoxelConstants.getLogger().info("Received world name from settings: {}", value);
                }
                case "minimapAllowed" -> serverSettings.minimapAllowed.set(value);
                case "worldmapAllowed" -> serverSettings.worldmapAllowed.set(value);
                case "cavesAllowed" -> serverSettings.cavesAllowed.set(value);
                case "manualCavesAllowed" -> serverSettings.manualCavesAllowed.set(value);
                case "waypointsAllowed" -> serverSettings.waypointsAllowed.set(value);
                case "deathWaypointAllowed" -> serverSettings.deathpointsAllowd.set(value);
                case "radarAllowed" -> serverSettings.radarAllowed.set(value);
                case "radarMobsAllowed" -> serverSettings.radarMobsAllowed.set(value);
                case "radarPlayersAllowed" -> serverSettings.radarPlayersAllowed.set(value);
                case "serverTeleportCommand" -> serverSettings.serverTeleportCommand.set(value);
            }
        }
        VoxelConstants.getVoxelMapInstance().getOptionsManager().updateOptionsAllowed(serverSettings, "Server Packet");
    }
}
