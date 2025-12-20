package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VoxelmapWorldIdChannelHandlerNeoForge {

    public static void handleDataOnMain(final WorldIdS2C data, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> WorldIdS2C.updateWorld(data));
        ctx.get().setPacketHandled(true);
    }
}
