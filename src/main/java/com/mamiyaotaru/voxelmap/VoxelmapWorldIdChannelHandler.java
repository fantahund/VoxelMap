package com.mamiyaotaru.voxelmap;

import java.nio.charset.StandardCharsets;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientConfigurationNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class VoxelmapWorldIdChannelHandler implements ClientPlayNetworking.PlayChannelHandler, ClientConfigurationNetworking.ConfigurationChannelHandler {

    private static final Identifier CHANNEL_IDENTIFIER = new Identifier("worldinfo", "world_id");

    public VoxelmapWorldIdChannelHandler() {
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_IDENTIFIER, this);
        ClientConfigurationNetworking.registerGlobalReceiver(CHANNEL_IDENTIFIER, this);
    }

    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buffer, PacketSender responseSender) {
        parsePacket(client, buffer);
    }

    @Override
    public void receive(MinecraftClient client, ClientConfigurationNetworkHandler handler, PacketByteBuf buffer, PacketSender responseSender) {
        parsePacket(client, buffer);
    }

    private void parsePacket(MinecraftClient client, PacketByteBuf buffer) {
        buffer.readByte(); // ignore
        int length;
        int b = buffer.readByte();
        if (b == 42) {
            // "new" packet
            length = buffer.readByte();
        } else if (b == 0) {
            // length == 0 ?
            VoxelConstants.getLogger().warn("Received unknown world_id packet");
            return;
        } else {
            // probably "legacy" packet
            VoxelConstants.getLogger().warn("Assuming legacy world_id packet. " +
                    "The support might be removed in the future versions.");
            length = b;
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        String subWorldName = new String(bytes, StandardCharsets.UTF_8);
        client.execute(() -> {
            VoxelConstants.getLogger().info("Received world_id: " + subWorldName);
            VoxelConstants.getVoxelMapInstance().newSubWorldName(subWorldName, true);
        });
    }
}
