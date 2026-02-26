package com.mamiyaotaru.voxelmap;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.ClimateSettings;

public interface ModApiBridge {
    default boolean isModEnabled(String modID) {
        return false;
    }

    default ClimateSettings getBiomeClimateSettings(Biome biome) {
        return null;
    }

    String getModLoader();
}