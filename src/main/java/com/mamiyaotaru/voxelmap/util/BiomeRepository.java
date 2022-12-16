package com.mamiyaotaru.voxelmap.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

public class BiomeRepository {
    public static Biome DEFAULT;
    public static Biome FOREST;
    public static Biome SWAMP;
    public static Biome SWAMP_HILLS;
    private static final Random generator = new Random();
    private static final HashMap<Integer, Integer> IDtoColor = new HashMap<>(256);
    private static final TreeMap<String, Integer> nameToColor = new TreeMap<>();
    private static boolean dirty = false;

    public static void getBiomes() {
        DEFAULT = BuiltinRegistries.BIOME.get(BiomeKeys.OCEAN);
        FOREST = BuiltinRegistries.BIOME.get(BiomeKeys.FOREST);
        SWAMP = BuiltinRegistries.BIOME.get(BiomeKeys.SWAMP);
        SWAMP_HILLS = BuiltinRegistries.BIOME.get(BiomeKeys.SWAMP); //TODO :>
    }

    public static void loadBiomeColors() {
        File saveDir = new File(MinecraftClient.getInstance().runDirectory, "/voxelmap/");
        File settingsFile = new File(saveDir, "biomecolors.txt");
        if (settingsFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(settingsFile));

                String sCurrentLine;
                while ((sCurrentLine = br.readLine()) != null) {
                    String[] curLine = sCurrentLine.split("=");
                    if (curLine.length == 2) {
                        String name = curLine[0];
                        int color = 0;

                        try {
                            color = Integer.decode(curLine[1]);
                        } catch (NumberFormatException var10) {
                            System.out.println("Error decoding integer string for biome colors; " + curLine[1]);
                        }

                        if (nameToColor.put(name, color) != null) {
                            dirty = true;
                        }
                    }
                }

                br.close();
            } catch (Exception var12) {
                System.err.println("biome load error: " + var12.getLocalizedMessage());
                var12.printStackTrace();
            }
        }

        try {
            InputStream is = MinecraftClient.getInstance().getResourceManager().getResource(new Identifier("voxelmap", "conf/biomecolors.txt")).get().getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                String[] curLine = sCurrentLine.split("=");
                if (curLine.length == 2) {
                    String name = curLine[0];
                    int color;

                    try {
                        color = Integer.decode(curLine[1]);
                    } catch (NumberFormatException var9) {
                        System.out.println("Error decoding integer string for biome colors; " + curLine[1]);
                        color = 0;
                    }

                    if (nameToColor.get(name) == null) {
                        nameToColor.put(name, color);
                        dirty = true;
                    }
                }
            }

            br.close();
            is.close();
        } catch (IOException var11) {
            System.out.println("Error loading biome color config file from litemod!");
            var11.printStackTrace();
        }

    }

    public static void saveBiomeColors() {
        if (dirty) {
            File saveDir = new File(MinecraftClient.getInstance().runDirectory, "/voxelmap/");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            File settingsFile = new File(saveDir, "biomecolors.txt");

            try {
                PrintWriter out = new PrintWriter(new FileWriter(settingsFile));

                for (Entry<String, Integer> entry : nameToColor.entrySet()) {
                    String name = entry.getKey();
                    Integer color = entry.getValue();
                    StringBuilder hexColor = new StringBuilder(Integer.toHexString(color));

                    while (hexColor.length() < 6) {
                        hexColor.insert(0, "0");
                    }

                    hexColor.insert(0, "0x");
                    out.println(name + "=" + hexColor);
                }

                out.close();
            } catch (Exception var8) {
                System.err.println("biome save error: " + var8.getLocalizedMessage());
                var8.printStackTrace();
            }
        }

        dirty = false;
    }

    public static int getBiomeColor(int biomeID) {
        Integer color = IDtoColor.get(biomeID);
        if (color == null) {
            Biome biome = MinecraftClient.getInstance().world.getRegistryManager().get(Registry.BIOME_KEY).get(biomeID);
            if (biome != null) {
                String identifier = MinecraftClient.getInstance().world.getRegistryManager().get(Registry.BIOME_KEY).getId(biome).toString();
                color = nameToColor.get(identifier);
                if (color == null) {
                    String friendlyName = getName(biome);
                    color = nameToColor.get(friendlyName);
                    if (color != null) {
                        nameToColor.remove(friendlyName);
                        nameToColor.put(identifier, color);
                        dirty = true;
                    }
                }

                if (color == null) {
                    int r = generator.nextInt(255);
                    int g = generator.nextInt(255);
                    int b = generator.nextInt(255);
                    color = r << 16 | g << 8 | b;
                    nameToColor.put(identifier, color);
                    dirty = true;
                }
            } else {
                System.out.println("non biome");
                color = 0;
            }

            IDtoColor.put(biomeID, color);
        }

        return color;
    }

    private static String getName(Biome biome) {
        Identifier resourceLocation = MinecraftClient.getInstance().world.getRegistryManager().get(Registry.BIOME_KEY).getId(biome);
        String translationKey = Util.createTranslationKey("biome", resourceLocation);
        String name = I18nUtils.getString(translationKey);
        if (name.equals(translationKey)) {
            name = TextUtils.prettify(resourceLocation.getPath());
        }

        return name;
    }

    public static String getName(int biomeID) {
        String name = null;
        Biome biome = MinecraftClient.getInstance().world.getRegistryManager().get(Registry.BIOME_KEY).get(biomeID);
        if (biome != null) {
            name = getName(biome);
        }

        if (name == null) {
            name = "Unknown";
        }

        return name;
    }
}
