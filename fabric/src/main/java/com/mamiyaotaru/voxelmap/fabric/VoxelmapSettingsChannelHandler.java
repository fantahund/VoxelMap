package com.mamiyaotaru.voxelmap.fabric;

import com.google.gson.Gson;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking.Context;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;

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
