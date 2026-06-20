package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class VoxelMapFabricMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricSettingsChannelHandler.initClient();
        FabricWorldIdChannelHandler.initClient();
        VoxelConstants.setModVersion(FabricLoader.getInstance().getModContainer(VoxelConstants.MOD_ID).map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse(null));
        VoxelConstants.setEvents(new FabricEvents());
        VoxelConstants.setPacketBridge(new FabricPacketBridge());
        VoxelConstants.setModApiBride(new FabricModApiBridge());
    }
}
