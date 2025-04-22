package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.ModApiBridge;
import net.fabricmc.loader.api.FabricLoader;

public class FabricModApiBridge implements ModApiBridge {
    @Override
    public boolean isModEnabled(String modID) {
        return FabricLoader.getInstance().isModLoaded(modID);
    }
}
