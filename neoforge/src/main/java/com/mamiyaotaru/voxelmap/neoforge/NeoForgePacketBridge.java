package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;

public class NeoForgePacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection instanceof ICommonPacketListener listener && listener.hasChannel(WorldIdS2C.PACKET_ID)) {
            ClientPacketDistributor.sendToServer(new WorldIdS2C(""));
        }
    }
}
