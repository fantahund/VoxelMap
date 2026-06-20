package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.packets.SettingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class FabricSettingsChannelHandler {
    private FabricSettingsChannelHandler() {
    }

    public static void initClient() {
        initPayloads();
        ClientPlayNetworking.registerGlobalReceiver(SettingsPayload.PACKET_ID, (payload, _) -> SettingsPayload.parsePacket(payload));
        ClientConfigurationNetworking.registerGlobalReceiver(SettingsPayload.PACKET_ID, (payload, _) -> SettingsPayload.parsePacket(payload));
    }

    public static void initServer() {
        initPayloads();
    }

    private static void initPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(SettingsPayload.PACKET_ID, SettingsPayload.PACKET_CODEC);
        PayloadTypeRegistry.clientboundConfiguration().register(SettingsPayload.PACKET_ID, SettingsPayload.PACKET_CODEC);
    }
}
