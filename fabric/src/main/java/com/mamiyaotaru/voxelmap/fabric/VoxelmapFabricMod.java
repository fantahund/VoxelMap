package com.mamiyaotaru.voxelmap.fabric;


import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.fabricmc.api.ClientModInitializer;

public class VoxelmapFabricMod implements ClientModInitializer {

    public void onInitializeClient() {
        new VoxelmapSettingsChannelHandler();
        new VoxelmapWorldIdChannelHandler();
        VoxelConstants.setEvents(new FabricEvents());
    }
}
