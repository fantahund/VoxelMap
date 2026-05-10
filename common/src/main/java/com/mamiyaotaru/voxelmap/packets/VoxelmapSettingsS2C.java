package com.mamiyaotaru.voxelmap.packets;

import com.google.gson.Gson;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.options.MapPermissionsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Map;

public record VoxelmapSettingsS2C(String settingsJson) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VoxelmapSettingsS2C> PACKET_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "settings"));
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
        MapPermissionsManager permissions = VoxelConstants.getVoxelMapInstance().getPermissionsManager();
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.equals("worldName")) {
                if (value instanceof String worldName) {
                    Minecraft.getInstance().execute(() -> {
                        VoxelConstants.getVoxelMapInstance().newSubWorldName(worldName, true);
                        VoxelConstants.getLogger().info("Received world name from settings: {}", worldName);
                    });
                } else {
                    VoxelConstants.getLogger().warn("Invalid world name: {}", value);
                }
            } else {
                permissions.set(key, value);
            }
        }
        VoxelConstants.getVoxelMapInstance().getOptionsManager().updateOptionsAllowed(permissions, "Server Packet");
    }
}
