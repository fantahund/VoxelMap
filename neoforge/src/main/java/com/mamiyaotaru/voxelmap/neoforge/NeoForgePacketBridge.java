package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.WorldIdC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class NeoForgePacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(WorldIdC2S.PACKET_ID.id())){
            ClientPacketDistributor.sendToServer(new WorldIdC2S());
        }
    }
}
