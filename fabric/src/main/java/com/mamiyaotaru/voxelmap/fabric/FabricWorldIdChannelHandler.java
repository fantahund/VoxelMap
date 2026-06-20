package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.packets.WorldIdPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class FabricWorldIdChannelHandler {
    private FabricWorldIdChannelHandler() {
    }

    public static void initClient() {
        initPayloads();
        ClientPlayNetworking.registerGlobalReceiver(WorldIdPayload.PACKET_ID, (payload, _) -> WorldIdPayload.parsePacket(payload));
        ClientConfigurationNetworking.registerGlobalReceiver(WorldIdPayload.PACKET_ID, (payload, _) -> WorldIdPayload.parsePacket(payload));
    }

    public static void initServer() {
        initPayloads();
    }

    private static void initPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(WorldIdPayload.PACKET_ID, WorldIdPayload.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WorldIdPayload.PACKET_ID, WorldIdPayload.PACKET_CODEC);
        PayloadTypeRegistry.serverboundConfiguration().register(WorldIdPayload.PACKET_ID, WorldIdPayload.PACKET_CODEC);
        PayloadTypeRegistry.clientboundConfiguration().register(WorldIdPayload.PACKET_ID, WorldIdPayload.PACKET_CODEC);
    }
}
