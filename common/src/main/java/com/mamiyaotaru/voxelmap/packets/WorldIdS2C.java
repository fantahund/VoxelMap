package com.mamiyaotaru.voxelmap.packets;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WorldIdS2C(String worldName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WorldIdS2C> PACKET_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("worldinfo", "world_id"));
    public static final StreamCodec<FriendlyByteBuf, WorldIdS2C> PACKET_CODEC = StreamCodec.ofMember(WorldIdS2C::write, WorldIdS2C::new);

    public WorldIdS2C(FriendlyByteBuf buf) {
        this(parse(buf));
    }

    private static String parse(FriendlyByteBuf buf) {
        buf.readByte(); // ignore
        int length;
        int b = buf.readUnsignedByte();
        if (b == 42) {
            // "new" packet
            length = buf.readUnsignedByte();
        } else if (b == 0) {
            // length == 0 ?
            VoxelConstants.getLogger().warn("Received unknown world_id packet");
            return null;
        } else {
            // probably "legacy" packet
            VoxelConstants.getLogger().warn("Assuming legacy world_id packet. " +
                    "The support might be removed in the future versions.");
            length = b;
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeByte(0);
        buf.writeByte(42);
        byte[] bytes = worldName.getBytes(StandardCharsets.UTF_8);
        buf.writeByte(bytes.length);
        buf.writeBytes(bytes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
