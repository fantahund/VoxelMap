package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public class ForgeWorldIdPacketHandler {
    public static final SimpleChannel WORLD_ID = ChannelBuilder
            .named(WorldIdS2C.PACKET_ID.id())
            .networkProtocolVersion(1)
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .simpleChannel();

    public static void register() {
        WORLD_ID.messageBuilder(WorldIdS2C.class, 0)
                .encoder(WorldIdS2C::write)
                .decoder(WorldIdS2C::new)
                .consumerMainThread(ForgeWorldIdPacketHandler::handle)
                .add();
    }

    private static void handle(WorldIdS2C data, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> WorldIdS2C.updateWorld(data));
        context.setPacketHandled(true);
    }
}
