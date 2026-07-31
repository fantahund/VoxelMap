package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class VoxelMapNeoForgeClientMod {
    private VoxelMapNeoForgeClientMod() {
    }

    public static void init(ModContainer container) {
        VoxelConstants.setModVersion(container.getModInfo().getVersion().toString());
        VoxelConstants.setEvents(new NeoForgeEvents());
        VoxelConstants.setPacketBridge(new NeoForgePacketBridge());
        VoxelConstants.setModApiBride(new NeoForgeModApiBridge());
        VoxelConstants.setPackRegistrar(new NeoForgePackRegistrar());

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (_, parentGui) -> VoxelConstants.getVoxelMapInstance().openOptionsScreen(parentGui)
        );
    }
}
