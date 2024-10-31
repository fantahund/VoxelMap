package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.packets.WorldIdC2S;
import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking.Context;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class VoxelmapWorldIdChannelHandler implements ClientPlayNetworking.PlayPayloadHandler<WorldIdS2C>, ClientConfigurationNetworking.ConfigurationPayloadHandler<WorldIdS2C> {
    public VoxelmapWorldIdChannelHandler() {
        PayloadTypeRegistry.playC2S().register(WorldIdC2S.PACKET_ID, WorldIdC2S.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(WorldIdS2C.PACKET_ID, WorldIdS2C.PACKET_CODEC);
        PayloadTypeRegistry.configurationC2S().register(WorldIdC2S.PACKET_ID, WorldIdC2S.PACKET_CODEC);
        PayloadTypeRegistry.configurationS2C().register(WorldIdS2C.PACKET_ID, WorldIdS2C.PACKET_CODEC);

        ClientPlayNetworking.registerGlobalReceiver(WorldIdS2C.PACKET_ID, this);
        ClientConfigurationNetworking.registerGlobalReceiver(WorldIdS2C.PACKET_ID, this);
    }

    @Override
    public void receive(WorldIdS2C payload, Context context) {
        WorldIdS2C.updateWorld(payload);
    }

    @Override
    public void receive(WorldIdS2C payload, net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context context) {
        WorldIdS2C.updateWorld(payload);
    }
}
