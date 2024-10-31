package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.WorldIdC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class FabricPacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket() {
        if (ClientPlayNetworking.canSend(WorldIdC2S.PACKET_ID)) {
            ClientPlayNetworking.send(new WorldIdC2S());
        }
    }
}