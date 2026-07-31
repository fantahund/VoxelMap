package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class VoxelMapForgeClientMod {
    private VoxelMapForgeClientMod() {
    }

    public static void init(FMLJavaModLoadingContext context) {
        VoxelConstants.setModVersion(context.getContainer().getModInfo().getVersion().toString());
        VoxelConstants.setEvents(new ForgeEvents());
        VoxelConstants.setPacketBridge(new ForgePacketBridge());
        VoxelConstants.setModApiBride(new ForgeModApiBridge());
        VoxelConstants.setPackRegistrar(new ForgePackRegistrar());

        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((_, parentGui) -> VoxelConstants.getVoxelMapInstance().openOptionsScreen(parentGui))
        );
    }
}
