package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.ModApiBridge;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.ClimateSettings;

public class FabricModApiBridge implements ModApiBridge {
    @Override
    public boolean isModEnabled(String modID) {
        return FabricLoader.getInstance().isModLoaded(modID);
    }

    @Override
    public ClimateSettings getBiomeClimateSettings(Biome biome) {
        return biome.climateSettings;
    }
}
