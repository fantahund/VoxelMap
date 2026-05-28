package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.packets.WorldIdPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;

public class ForgeWorldIdPacketHandler {
    private static Channel<CustomPacketPayload> channel;

    public static void register() {
        channel = ChannelBuilder
                .named(WorldIdPayload.PACKET_ID.id())
                .optional()
                .payloadChannel()
                    .any()
                        .bidirectional()
                            .addMain(WorldIdPayload.PACKET_ID, WorldIdPayload.PACKET_CODEC, ForgeWorldIdPacketHandler::receive)
                .build();
    }

    public static Channel<CustomPacketPayload> getChannel() {
        return channel;
    }

    private static void receive(WorldIdPayload data, CustomPayloadEvent.Context context) {
        WorldIdPayload.parsePacket(data);
    }
}
