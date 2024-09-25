package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@FunctionalInterface
@Mixin(Biome.class)
public interface BiomeAccessor {
    @Accessor
    Biome.ClimateSettings getClimateSettings();
}