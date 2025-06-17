package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking.Context;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class VoxelmapSettingsChannelHandler implements ClientPlayNetworking.PlayPayloadHandler<VoxelmapSettingsS2C>, ClientConfigurationNetworking.ConfigurationPayloadHandler<VoxelmapSettingsS2C> {
    public VoxelmapSettingsChannelHandler() {
        PayloadTypeRegistry.playS2C().register(VoxelmapSettingsS2C.PACKET_ID, VoxelmapSettingsS2C.PACKET_CODEC);
        PayloadTypeRegistry.configurationS2C().register(VoxelmapSettingsS2C.PACKET_ID, VoxelmapSettingsS2C.PACKET_CODEC);

        ClientPlayNetworking.registerGlobalReceiver(VoxelmapSettingsS2C.PACKET_ID, this);
        ClientConfigurationNetworking.registerGlobalReceiver(VoxelmapSettingsS2C.PACKET_ID, this);
    }

    @Override
    public void receive(VoxelmapSettingsS2C payload, Context context) {
        VoxelmapSettingsS2C.parsePacket(payload);
    }

    @Override
    public void receive(VoxelmapSettingsS2C payload, net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context context) {
        VoxelmapSettingsS2C.parsePacket(payload);
    }
}
