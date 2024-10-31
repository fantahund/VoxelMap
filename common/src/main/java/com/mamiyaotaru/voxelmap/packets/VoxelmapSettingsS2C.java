package com.mamiyaotaru.voxelmap.packets;

import com.google.gson.Gson;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record VoxelmapSettingsS2C(String settingsJson) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VoxelmapSettingsS2C> PACKET_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("voxelmap", "settings"));
    public static final StreamCodec<FriendlyByteBuf, VoxelmapSettingsS2C> PACKET_CODEC = StreamCodec.ofMember(VoxelmapSettingsS2C::write, VoxelmapSettingsS2C::new);

    public VoxelmapSettingsS2C(FriendlyByteBuf buf) {
        this(parse(buf));
    }

    private static String parse(FriendlyByteBuf buf) {
        buf.readByte(); // ignore
        return buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeByte(0);
        buf.writeUtf(settingsJson);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

    public static void parsePacket(VoxelmapSettingsS2C packet) {
        @SuppressWarnings("unchecked")
        Map<String, Object> settings = new Gson().fromJson(packet.settingsJson(), Map.class);
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
