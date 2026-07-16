package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import com.mamiyaotaru.voxelmap.packets.VoxelmapClientPacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class FabricSettingsChannelHandler {
    private FabricSettingsChannelHandler() {
    }

    private static void registerPacket() {
        PayloadTypeRegistry.clientboundPlay().register(VoxelmapSettingsS2C.PACKET_ID, VoxelmapSettingsS2C.PACKET_CODEC);
        PayloadTypeRegistry.clientboundConfiguration().register(VoxelmapSettingsS2C.PACKET_ID, VoxelmapSettingsS2C.PACKET_CODEC);
    }

    public static void initClient() {
        registerPacket();
        ClientPlayNetworking.registerGlobalReceiver(VoxelmapSettingsS2C.PACKET_ID, (payload, _) -> VoxelmapClientPacketHandler.applySettings(payload));
        ClientConfigurationNetworking.registerGlobalReceiver(VoxelmapSettingsS2C.PACKET_ID, (payload, _) -> VoxelmapClientPacketHandler.applySettings(payload));
    }

    public static void initServer() {
        registerPacket();
    }
}
