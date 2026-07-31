package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.multiloader.MultiLoaderManager;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class VoxelMapForgeClientMod {
    private VoxelMapForgeClientMod() {
    }

    public static void init(FMLJavaModLoadingContext context) {
        MultiLoaderManager.setEvents(new ForgeEvents());
        MultiLoaderManager.setPacketBridge(new ForgePacketBridge());
        MultiLoaderManager.setModApiBride(new ForgeModApiBridge());
        MultiLoaderManager.setPackRegistrar(new ForgePackRegistrar());

        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((_, parentGui) -> VoxelConstants.openConfigScreen(parentGui))
        );
    }
}
