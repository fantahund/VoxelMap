package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public class ForgeSettingsPacketHandler {
    public static final SimpleChannel SETTINGS = ChannelBuilder
            .named(VoxelmapSettingsS2C.PACKET_ID.id())
            .networkProtocolVersion(1)
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .simpleChannel();

    public static void register() {
        SETTINGS.messageBuilder(VoxelmapSettingsS2C.class, 0)
                .encoder(VoxelmapSettingsS2C::write)
                .decoder(VoxelmapSettingsS2C::new)
                .consumerMainThread(ForgeSettingsPacketHandler::handle)
                .add();
    }

    private static void handle(VoxelmapSettingsS2C data, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> VoxelmapSettingsS2C.parsePacket(data));
        context.setPacketHandled(true);
    }
}
