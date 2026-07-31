package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class VoxelMapFabricClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricPacketHandler.initClient();
        VoxelConstants.setModVersion(FabricLoader.getInstance().getModContainer(VoxelConstants.MOD_ID).map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse(null));
        VoxelConstants.setEvents(new FabricEvents());
        VoxelConstants.setPacketBridge(new FabricPacketBridge());
        VoxelConstants.setModApiBride(new FabricModApiBridge());
        VoxelConstants.setPackRegistrar(new FabricPackRegistrar());
    }
}
