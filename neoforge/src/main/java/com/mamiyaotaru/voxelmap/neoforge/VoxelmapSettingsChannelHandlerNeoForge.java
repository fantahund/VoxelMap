package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VoxelmapSettingsChannelHandlerNeoForge {

    public static void handleDataOnMain(final VoxelmapSettingsS2C data, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> VoxelmapSettingsS2C.parsePacket(data));
        ctx.get().setPacketHandled(true);
    }
}
