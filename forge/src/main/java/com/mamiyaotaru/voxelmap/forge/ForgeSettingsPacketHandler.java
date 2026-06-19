package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import com.mamiyaotaru.voxelmap.packets.VoxelmapClientPacketHandler;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;

public class ForgeSettingsPacketHandler {
    public static Channel<CustomPacketPayload> SETTINGS;

    public static void register() {
        SETTINGS = ChannelBuilder
            .named("voxelmap:settings_payload")
            .networkProtocolVersion(1)
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .payloadChannel()
            .any()
            .bidirectional()
            .addMain(VoxelmapSettingsS2C.PACKET_ID, VoxelmapSettingsS2C.PACKET_CODEC, ForgeSettingsPacketHandler::handle)
            .build();
    }

    private static void handle(VoxelmapSettingsS2C data, CustomPayloadEvent.Context context) {
        if (context.isClientSide()) {
            VoxelmapClientPacketHandler.applySettings(data);
        }
    }
}
