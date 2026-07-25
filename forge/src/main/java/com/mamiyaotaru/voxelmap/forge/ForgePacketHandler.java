package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.packets.VoxelMapSettingsPayload;
import com.mamiyaotaru.voxelmap.packets.VoxelMapWorldIdPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;

public class ForgePacketHandler {
    private ForgePacketHandler() {
    }

    private static Channel<CustomPacketPayload> settingsChannel;
    private static Channel<CustomPacketPayload> worldIdChannel;

    public static void register() {
        settingsChannel = ChannelBuilder
                .named(VoxelMapSettingsPayload.PACKET_ID.id())
                .networkProtocolVersion(1)
                .clientAcceptedVersions((status, version) -> true)
                .serverAcceptedVersions((status, version) -> true)
                .payloadChannel()
                    .any()
                        .bidirectional()
                            .addMain(VoxelMapSettingsPayload.PACKET_ID, VoxelMapSettingsPayload.PACKET_CODEC, (payload, _) -> VoxelMapSettingsPayload.parse(payload))
                .build();

        worldIdChannel = ChannelBuilder
                .named(VoxelMapWorldIdPayload.PACKET_ID.id())
                .networkProtocolVersion(1)
                .clientAcceptedVersions((status, version) -> true)
                .serverAcceptedVersions((status, version) -> true)
                .payloadChannel()
                    .any()
                        .bidirectional()
                            .addMain(VoxelMapWorldIdPayload.PACKET_ID, VoxelMapWorldIdPayload.PACKET_CODEC, (payload, _) -> VoxelMapWorldIdPayload.parse(payload))
                .build();
    }

    public static Channel<CustomPacketPayload> settingsChannel() {
        return settingsChannel;
    }

    public static Channel<CustomPacketPayload> worldIdChannel() {
        return worldIdChannel;
    }
}
