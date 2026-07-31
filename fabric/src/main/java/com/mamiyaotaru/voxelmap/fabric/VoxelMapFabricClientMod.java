package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.multiloader.MultiLoaderManager;
import net.fabricmc.api.ClientModInitializer;

public class VoxelMapFabricClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricPacketHandler.initClient();
        MultiLoaderManager.setEvents(new FabricEvents());
        MultiLoaderManager.setPacketBridge(new FabricPacketBridge());
        MultiLoaderManager.setModApiBride(new FabricModApiBridge());
        MultiLoaderManager.setPackRegistrar(new FabricPackRegistrar());
    }
}
