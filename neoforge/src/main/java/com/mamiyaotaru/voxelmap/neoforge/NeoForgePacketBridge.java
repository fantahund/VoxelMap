package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class NeoForgePacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket() {
        ClientPacketDistributor.sendToServer(new WorldIdS2C(""));
    }
}
