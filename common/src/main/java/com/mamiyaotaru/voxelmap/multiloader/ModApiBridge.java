package com.mamiyaotaru.voxelmap.multiloader;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.ClimateSettings;

public interface ModApiBridge {
    default boolean isModEnabled(String modID) {
        return false;
    }

    default ClimateSettings getBiomeClimateSettings(Biome biome) {
        return null;
    }

    default String getModLoader() {
        return "unknown";
    }

    default String getModVersion(String modID) {
        return "unknown";
    }
}