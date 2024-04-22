package com.mamiyaotaru.voxelmap.packets;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WorldIdC2S() implements CustomPayload {
    public static final CustomPayload.Id<WorldIdC2S> PACKET_ID = new CustomPayload.Id<>(new Identifier("worldinfo", "world_id"));
    public static final PacketCodec<PacketByteBuf, WorldIdC2S> PACKET_CODEC = PacketCodec.of(WorldIdC2S::write, WorldIdC2S::new);

    public WorldIdC2S(PacketByteBuf buf) {
        this();
        buf.skipBytes(buf.readableBytes());
    }

    public void write(PacketByteBuf buf) {
        buf.writeByte(0);
        buf.writeByte(42);
        buf.writeByte(0);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
