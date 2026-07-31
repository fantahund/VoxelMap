package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.multiloader.PacketBridge;
import com.mamiyaotaru.voxelmap.packets.VoxelMapWorldIdPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;

public class NeoForgePacketBridge implements PacketBridge {
    @Override
    public void sendWorldIDPacket(String worldId) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection instanceof ICommonPacketListener listener && listener.hasChannel(VoxelMapWorldIdPayload.PACKET_ID)) {
            ClientPacketDistributor.sendToServer(new VoxelMapWorldIdPayload(worldId));
        }
    }
}
