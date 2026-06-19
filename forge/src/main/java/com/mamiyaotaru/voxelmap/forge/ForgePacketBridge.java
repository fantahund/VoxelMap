package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.WorldIdPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class ForgePacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket(String worldName) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && ForgeWorldIdPacketHandler.getChannel().isRemotePresent(connection.getConnection())) {
            ForgeWorldIdPacketHandler.getChannel().send(new WorldIdPayload(worldName), connection.getConnection());
        }
    }
}
