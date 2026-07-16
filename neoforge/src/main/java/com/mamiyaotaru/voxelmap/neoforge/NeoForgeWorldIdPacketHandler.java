package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.packets.VoxelmapClientPacketHandler;
import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NeoForgeWorldIdPacketHandler {

    public static void handleDataOnMain(final WorldIdS2C data, final IPayloadContext context) {
        VoxelmapClientPacketHandler.updateWorld(data);
    }
}
