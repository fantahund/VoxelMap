package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class VoxelmapWorldIdChannelHandlerNeoForge {

    public static void handleDataOnMain(final WorldIdS2C data, final PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> WorldIdS2C.updateWorld(data));
    }
}
