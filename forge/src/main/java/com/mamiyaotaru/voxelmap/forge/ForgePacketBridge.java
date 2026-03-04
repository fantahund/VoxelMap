package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.WorldIdC2S;
import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraftforge.network.PacketDistributor;

public class ForgePacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && ForgeWorldIdPacketHandler.WORLD_ID.isRemotePresent(connection.getConnection())) {
            // ohh stop crashing please
//            ForgeWorldIdPacketHandler.WORLD_ID.send(new WorldIdC2S(), PacketDistributor.SERVER.noArg());
        }
    }
}
