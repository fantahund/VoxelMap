package com.mamiyaotaru.voxelmap.util;

import com.google.common.collect.BiMap;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class BiomeParser {
    private BiomeParser() {
    }

    public static void parseLine(World world, String line, BiMap<Biome, Integer> map) {
        String[] lineParts = line.split(" ");

        int id = Integer.parseInt(lineParts[0]);
        Biome biome = world.getRegistryManager().get(RegistryKeys.BIOME).get(Identifier.of(lineParts[1]));
        if (biome != null) {
            map.forcePut(biome, id);
        }
    }

    public static void populateLegacyBiomeMap(ClientWorld world, BiMap<Biome, Integer> map) {
        Registry<Biome> registry = world.getRegistryManager().get(RegistryKeys.BIOME);
        registry.forEach(biome -> {
            map.forcePut(biome, registry.getRawId(biome));
        });
    }
}
