package com.mamiyaotaru.voxelmap.packets;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WorldIdS2C(String worldName) implements CustomPayload {
    public static final CustomPayload.Id<WorldIdS2C> PACKET_ID = new CustomPayload.Id<>(new Identifier("worldinfo", "world_id"));
    public static final PacketCodec<PacketByteBuf, WorldIdS2C> PACKET_CODEC = PacketCodec.of(WorldIdS2C::write, WorldIdS2C::new);

    public WorldIdS2C(PacketByteBuf buf) {
        this(parse(buf));
    }

    private static String parse(PacketByteBuf buf) {
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

    public void write(PacketByteBuf buf) {
        buf.writeByte(0);
        buf.writeByte(42);
        byte[] bytes = worldName.getBytes(StandardCharsets.UTF_8);
        buf.writeByte(bytes.length);
        buf.writeBytes(bytes);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
