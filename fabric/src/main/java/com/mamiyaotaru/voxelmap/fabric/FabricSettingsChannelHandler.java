package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.packets.SettingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class FabricSettingsChannelHandler implements ClientPlayNetworking.PlayPayloadHandler<SettingsPayload>, ClientConfigurationNetworking.ConfigurationPayloadHandler<SettingsPayload> {
    public FabricSettingsChannelHandler() {
        PayloadTypeRegistry.playS2C().register(SettingsPayload.PACKET_ID, SettingsPayload.PACKET_CODEC);
        PayloadTypeRegistry.configurationS2C().register(SettingsPayload.PACKET_ID, SettingsPayload.PACKET_CODEC);

        ClientPlayNetworking.registerGlobalReceiver(SettingsPayload.PACKET_ID, this);
        ClientConfigurationNetworking.registerGlobalReceiver(SettingsPayload.PACKET_ID, this);
    }

    @Override
    public void receive(SettingsPayload payload, ClientConfigurationNetworking.Context context) {
        SettingsPayload.parsePacket(payload);
    }

    @Override
    public void receive(SettingsPayload payload, ClientPlayNetworking.Context context) {
        SettingsPayload.parsePacket(payload);
    }
}
