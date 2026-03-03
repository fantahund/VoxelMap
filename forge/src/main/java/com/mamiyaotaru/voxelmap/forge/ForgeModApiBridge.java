package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.ModApiBridge;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.fml.ModList;

public class ForgeModApiBridge implements ModApiBridge {
    @Override
    public boolean isModEnabled(String modID) {
        return ModList.get().isLoaded(modID);
    }

    @Override
    public Biome.ClimateSettings getBiomeClimateSettings(Biome biome) {
        return biome.getModifiedClimateSettings();
    }

    @Override
    public String getModLoader() {
        return "forge";
    }
}
