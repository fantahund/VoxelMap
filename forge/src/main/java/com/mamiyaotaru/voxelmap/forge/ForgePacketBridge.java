package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.VoxelMapWorldIdPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class ForgePacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket(String worldId) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && ForgePacketHandler.worldIdChannel().isRemotePresent(connection.getConnection())) {
            ForgePacketHandler.worldIdChannel().send(new VoxelMapWorldIdPayload(worldId), connection.getConnection());
        }
    }
}
