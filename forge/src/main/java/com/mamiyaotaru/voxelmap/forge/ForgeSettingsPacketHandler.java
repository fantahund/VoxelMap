package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.packets.SettingsPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;

public class ForgeSettingsPacketHandler {
    private static Channel<CustomPacketPayload> channel;

    public static void register() {
        channel = ChannelBuilder
                .named(SettingsPayload.PACKET_ID.id())
                .optional()
                .payloadChannel()
                    .any()
                        .clientbound()
                            .addMain(SettingsPayload.PACKET_ID, SettingsPayload.PACKET_CODEC, ForgeSettingsPacketHandler::receive)
                .build();
    }

    public static Channel<CustomPacketPayload> getChannel() {
        return channel;
    }

    private static void receive(SettingsPayload data, CustomPayloadEvent.Context context) {
        SettingsPayload.parsePacket(data);
    }
}
