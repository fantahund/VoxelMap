package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.packets.WorldIdPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class FabricWorldIdChannelHandler implements ClientPlayNetworking.PlayPayloadHandler<WorldIdPayload>, ClientConfigurationNetworking.ConfigurationPayloadHandler<WorldIdPayload> {
    public FabricWorldIdChannelHandler() {
        PayloadTypeRegistry.playC2S().register(WorldIdPayload.PACKET_ID, WorldIdPayload.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(WorldIdPayload.PACKET_ID, WorldIdPayload.PACKET_CODEC);
        PayloadTypeRegistry.configurationC2S().register(WorldIdPayload.PACKET_ID, WorldIdPayload.PACKET_CODEC);
        PayloadTypeRegistry.configurationS2C().register(WorldIdPayload.PACKET_ID, WorldIdPayload.PACKET_CODEC);

        ClientPlayNetworking.registerGlobalReceiver(WorldIdPayload.PACKET_ID, this);
        ClientConfigurationNetworking.registerGlobalReceiver(WorldIdPayload.PACKET_ID, this);
    }

    @Override
    public void receive(WorldIdPayload payload, ClientConfigurationNetworking.Context context) {
        WorldIdPayload.parsePacket(payload);
    }

    @Override
    public void receive(WorldIdPayload payload, ClientPlayNetworking.Context context) {
        WorldIdPayload.parsePacket(payload);
    }
}
