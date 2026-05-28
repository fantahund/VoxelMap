package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.WorldIdPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class FabricPacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket(String worldName) {
        if (ClientPlayNetworking.canSend(WorldIdPayload.PACKET_ID)) {
            ClientPlayNetworking.send(new WorldIdPayload(worldName));
        }
    }
}