package com.mamiyaotaru.voxelmap.util;

import com.google.common.collect.BiMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class BiomeParser {
    private BiomeParser() {
    }

    public static void parseLine(Level world, String line, BiMap<Biome, Integer> map) {
        String[] lineParts = line.split(" ");

        int id = Integer.parseInt(lineParts[0]);
        Biome biome = world.registryAccess().lookupOrThrow(Registries.BIOME).get(new ResourceLocation(lineParts[1])).get().value();
        if (biome != null) {
            map.forcePut(biome, id);
        }
    }

    public static void populateLegacyBiomeMap(ClientLevel world, BiMap<Biome, Integer> map) {
        Registry<Biome> registry = world.registryAccess().lookupOrThrow(Registries.BIOME);
        registry.forEach(biome -> {
            map.forcePut(biome, registry.getId(biome));
        });
    }
}
