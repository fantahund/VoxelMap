package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.WorldIdC2S;

public class NeoForgePacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket() {
        ForgeEvents.CHANNEL.sendToServer(new WorldIdC2S());
    }
}
