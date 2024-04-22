package com.mamiyaotaru.voxelmap.packets;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VoxelmapSettingsS2C(String settingsJson) implements CustomPayload {
    public static final CustomPayload.Id<VoxelmapSettingsS2C> PACKET_ID = new CustomPayload.Id<>(new Identifier("voxelmap", "settings"));
    public static final PacketCodec<PacketByteBuf, VoxelmapSettingsS2C> PACKET_CODEC = PacketCodec.of(VoxelmapSettingsS2C::write, VoxelmapSettingsS2C::new);

    public VoxelmapSettingsS2C(PacketByteBuf buf) {
        this(parse(buf));
    }

    private static String parse(PacketByteBuf buf) {
        buf.readByte(); // ignore
        return buf.readString();
    }

    public void write(PacketByteBuf buf) {
        buf.writeByte(0);
        buf.writeString(settingsJson);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
