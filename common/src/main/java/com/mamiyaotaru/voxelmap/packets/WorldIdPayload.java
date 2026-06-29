package com.mamiyaotaru.voxelmap.packets;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public record WorldIdPayload(String worldName) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger("VoxelMap");
    public static final CustomPacketPayload.Type<WorldIdPayload> PACKET_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("worldinfo", "world_id"));
    public static final StreamCodec<FriendlyByteBuf, WorldIdPayload> PACKET_CODEC = StreamCodec.ofMember(WorldIdPayload::encode, WorldIdPayload::decode);

    public static WorldIdPayload decode(FriendlyByteBuf buf) {
        buf.readByte(); // ignore
        int length;
        int b = buf.readUnsignedByte();
        if (b == 42) {
            // "new" packet
            length = buf.readUnsignedByte();
        } else if (b == 0) {
            // length == 0 ?
            LOGGER.warn("Received unknown world_id packet");
            return null;
        } else {
            // probably "legacy" packet
            LOGGER.warn("Assuming legacy world_id packet. " +
                    "The support might be removed in the future versions.");
            length = b;
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        String worldName = new String(bytes, StandardCharsets.UTF_8);
        return new WorldIdPayload(worldName);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(0);
        buf.writeByte(42);
        buf.writeByte(0);
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
