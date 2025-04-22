package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.ModApiBridge;
import net.neoforged.fml.ModList;

public class NeoForgeModApiBridge implements ModApiBridge {
    @Override
    public boolean isModEnabled(String modID) {
        return ModList.get().isLoaded(modID);
    }
}
