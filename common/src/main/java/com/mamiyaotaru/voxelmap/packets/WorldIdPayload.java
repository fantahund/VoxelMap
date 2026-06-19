package com.mamiyaotaru.voxelmap.packets;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WorldIdPayload(String worldName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WorldIdPayload> PACKET_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "world_id"));
    public static final StreamCodec<FriendlyByteBuf, WorldIdPayload> PACKET_CODEC = StreamCodec.ofMember(WorldIdPayload::encode, WorldIdPayload::decode);

    public static WorldIdPayload decode(FriendlyByteBuf buf) {
        buf.readByte();
        return new WorldIdPayload(buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(0);
        buf.writeUtf(worldName);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

    public static void parsePacket(WorldIdPayload packet) {
        String worldName = packet.worldName;
        VoxelConstants.getLogger().info("Received world_id: {}", worldName);
        if (worldName != null) {
            VoxelConstants.getVoxelMapInstance().newSubWorldName(worldName, true);
        }
    }
}
