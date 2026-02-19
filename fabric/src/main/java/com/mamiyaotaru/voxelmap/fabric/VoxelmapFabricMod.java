package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class VoxelmapFabricMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        new VoxelmapSettingsChannelHandler();
        new VoxelmapWorldIdChannelHandler();
        new FabricPlatformResolver();
        VoxelConstants.setModVersion(FabricLoader.getInstance().getModContainer(VoxelConstants.MOD_ID).map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse(null));
        VoxelConstants.setEvents(new FabricEvents());
        VoxelConstants.setPacketBridge(new FabricPacketBridge());
        VoxelConstants.setModApiBride(new FabricModApiBridge());
    }
}
