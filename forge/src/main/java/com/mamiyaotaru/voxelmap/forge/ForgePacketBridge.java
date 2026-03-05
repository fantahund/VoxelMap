package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraftforge.network.PacketDistributor;

public class ForgePacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && ForgeWorldIdPacketHandler.WORLD_ID.isRemotePresent(connection.getConnection())) {
            ForgeWorldIdPacketHandler.WORLD_ID.send(new WorldIdS2C(""), PacketDistributor.SERVER.noArg());
        }
    }
}
