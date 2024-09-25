package com.mamiyaotaru.voxelmap.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

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
}
