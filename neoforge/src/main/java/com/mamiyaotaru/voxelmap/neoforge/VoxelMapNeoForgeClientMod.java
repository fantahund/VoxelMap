package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.neoforged.fml.ModContainer;

public final class VoxelMapNeoForgeClientMod {
    private VoxelMapNeoForgeClientMod() {
    }

    public static void init(ModContainer container) {
        VoxelConstants.setModVersion(container.getModInfo().getVersion().toString());
        VoxelConstants.setEvents(new NeoForgeEvents());
        VoxelConstants.setPacketBridge(new NeoForgePacketBridge());
        VoxelConstants.setModApiBride(new NeoForgeModApiBridge());
    }
}
