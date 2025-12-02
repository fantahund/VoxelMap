package com.mamiyaotaru.voxelmap.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WorldIdC2S() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WorldIdC2S> PACKET_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("worldinfo", "world_id"));
    public static final StreamCodec<FriendlyByteBuf, WorldIdC2S> PACKET_CODEC = StreamCodec.ofMember(WorldIdC2S::write, WorldIdC2S::new);

    public WorldIdC2S(FriendlyByteBuf buf) {
        this();
        buf.skipBytes(buf.readableBytes());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeByte(0);
        buf.writeByte(42);
        buf.writeByte(0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
