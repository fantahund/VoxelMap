package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgePacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket() {
        PacketDistributor.sendToServer(new WorldIdS2C(""));
    }
}
