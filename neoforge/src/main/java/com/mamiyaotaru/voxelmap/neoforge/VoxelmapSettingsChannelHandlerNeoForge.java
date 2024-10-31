package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class VoxelmapSettingsChannelHandlerNeoForge {

    public static void handleDataOnMain(final VoxelmapSettingsS2C data, final IPayloadContext context) {
        VoxelmapSettingsS2C.parsePacket(data);
    }
}
