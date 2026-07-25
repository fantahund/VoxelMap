package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.packets.VoxelMapSettingsPayload;
import com.mamiyaotaru.voxelmap.packets.VoxelMapWorldIdPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class FabricPacketHandler {
    private FabricPacketHandler() {
    }

    private static void initCommon() {
        // Register Settings
        PayloadTypeRegistry.clientboundPlay().register(VoxelMapSettingsPayload.PACKET_ID, VoxelMapSettingsPayload.PACKET_CODEC);
        PayloadTypeRegistry.clientboundConfiguration().register(VoxelMapSettingsPayload.PACKET_ID, VoxelMapSettingsPayload.PACKET_CODEC);

        // Register World ID
        PayloadTypeRegistry.clientboundPlay().register(VoxelMapWorldIdPayload.PACKET_ID, VoxelMapWorldIdPayload.PACKET_CODEC);
        PayloadTypeRegistry.clientboundConfiguration().register(VoxelMapWorldIdPayload.PACKET_ID, VoxelMapWorldIdPayload.PACKET_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VoxelMapWorldIdPayload.PACKET_ID, VoxelMapWorldIdPayload.PACKET_CODEC);
        PayloadTypeRegistry.serverboundConfiguration().register(VoxelMapWorldIdPayload.PACKET_ID, VoxelMapWorldIdPayload.PACKET_CODEC);
    }

    public static void initClient() {
        initCommon();

        // Receive Settings
        ClientPlayNetworking.registerGlobalReceiver(VoxelMapSettingsPayload.PACKET_ID, (payload, _) -> VoxelMapSettingsPayload.parse(payload));
        ClientConfigurationNetworking.registerGlobalReceiver(VoxelMapSettingsPayload.PACKET_ID, (payload, _) -> VoxelMapSettingsPayload.parse(payload));

        // Receive WorldID
        ClientPlayNetworking.registerGlobalReceiver(VoxelMapWorldIdPayload.PACKET_ID, (payload, _) -> VoxelMapWorldIdPayload.parse(payload));
        ClientConfigurationNetworking.registerGlobalReceiver(VoxelMapWorldIdPayload.PACKET_ID, (payload, _) -> VoxelMapWorldIdPayload.parse(payload));
    }

    public static void initServer() {
        initCommon();
    }
}
