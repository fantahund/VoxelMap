package com.mamiyaotaru.voxelmap.fabric;


import com.mamiyaotaru.voxelmap.VoxelmapSettingsChannelHandler;
import com.mamiyaotaru.voxelmap.VoxelmapWorldIdChannelHandler;
import net.fabricmc.api.ClientModInitializer;

public class VoxelmapFabricMod implements ClientModInitializer {

    public void onInitializeClient() {
        new VoxelmapSettingsChannelHandler();
        new VoxelmapWorldIdChannelHandler();
    }
}
