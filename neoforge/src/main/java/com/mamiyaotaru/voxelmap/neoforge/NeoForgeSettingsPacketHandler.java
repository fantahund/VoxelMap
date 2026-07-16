package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import com.mamiyaotaru.voxelmap.packets.VoxelmapClientPacketHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NeoForgeSettingsPacketHandler {

    public static void handleDataOnMain(final VoxelmapSettingsS2C data, final IPayloadContext context) {
        VoxelmapClientPacketHandler.applySettings(data);
    }
}
