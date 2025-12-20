package com.mamiyaotaru.voxelmap.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record WorldIdC2S() {
    public static final ResourceLocation PACKET_ID = new ResourceLocation("worldinfo", "world_id");

    public WorldIdC2S(FriendlyByteBuf buf) {
        this();
        buf.skipBytes(buf.readableBytes());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeByte(0);
        buf.writeByte(42);
        buf.writeByte(0);
    }
}
