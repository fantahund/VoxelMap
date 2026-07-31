package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.multiloader.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.VoxelMapWorldIdPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class FabricPacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket(String worldId) {
        if (ClientPlayNetworking.canSend(VoxelMapWorldIdPayload.PACKET_ID)) {
            ClientPlayNetworking.send(new VoxelMapWorldIdPayload(worldId));
        }
    }
}