package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.multiloader.MultiLoaderManager;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class VoxelMapNeoForgeClientMod {
    private VoxelMapNeoForgeClientMod() {
    }

    public static void init(ModContainer container) {
        MultiLoaderManager.setEvents(new NeoForgeEvents());
        MultiLoaderManager.setPacketBridge(new NeoForgePacketBridge());
        MultiLoaderManager.setModApiBride(new NeoForgeModApiBridge());
        MultiLoaderManager.setPackRegistrar(new NeoForgePackRegistrar());

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (_, parentGui) -> VoxelConstants.openConfigScreen(parentGui)
        );
    }
}
