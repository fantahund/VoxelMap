package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class VoxelmapSettingsChannelHandlerNeoForge {

    public static void handleDataOnMain(final VoxelmapSettingsS2C data, final PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> VoxelmapSettingsS2C.parsePacket(data));
    }
}
