package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.packets.VoxelmapClientPacketHandler;
import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;

public class ForgeWorldIdPacketHandler {
    public static Channel<CustomPacketPayload> WORLD_ID;

    public static void register() {
        WORLD_ID = ChannelBuilder
            .named("voxelmap:world_id_payload")
            .networkProtocolVersion(1)
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .payloadChannel()
            .any()
            .bidirectional()
            .addMain(WorldIdS2C.PACKET_ID, WorldIdS2C.PACKET_CODEC, ForgeWorldIdPacketHandler::handle)
            .build();
    }

    private static void handle(WorldIdS2C data, CustomPayloadEvent.Context context) {
        if (context.isClientSide()) {
            VoxelmapClientPacketHandler.updateWorld(data);
        }
    }
}
