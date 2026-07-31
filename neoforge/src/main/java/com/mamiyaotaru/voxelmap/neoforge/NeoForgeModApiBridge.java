package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.multiloader.ModApiBridge;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.ClimateSettings;
import net.neoforged.fml.ModList;

public class NeoForgeModApiBridge implements ModApiBridge {
    @Override
    public boolean isModEnabled(String modID) {
        return ModList.get().isLoaded(modID);
    }

    @Override
    public ClimateSettings getBiomeClimateSettings(Biome biome) {
        return biome.getModifiedClimateSettings();
    }

    @Override
    public String getModLoader() {
        return "neoforge";
    }

    @Override
    public String getModVersion(String modID) {
        return ModList.get().getModContainerById(modID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
}
