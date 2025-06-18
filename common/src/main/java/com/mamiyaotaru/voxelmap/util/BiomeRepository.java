package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import net.minecraft.Util;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;

public final class BiomeRepository {
    private static final Random generator = new Random();
    private static final HashMap<Biome, Integer> IDtoColor = new HashMap<>(256);
    private static final TreeMap<String, Integer> nameToColor = new TreeMap<>();
    private static boolean dirty;

    private BiomeRepository() {}

    public static void loadBiomeColors() {
        File saveDir = new File(VoxelConstants.getMinecraft().gameDirectory, "/voxelmap/");
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
                            VoxelConstants.getLogger().warn("Error decoding integer string for biome colors; " + curLine[1]);
                        }

                        if (nameToColor.put(name, color) != null) {
                            dirty = true;
                        }
                    }
                }

                br.close();
            } catch (IOException var12) {
                VoxelConstants.getLogger().error("biome load error: " + var12.getLocalizedMessage(), var12);
            }
        }

        try {
            InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("voxelmap", "conf/biomecolors.txt")).get().open();
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
                        VoxelConstants.getLogger().warn("Error decoding integer string for biome colors; " + curLine[1]);
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
            VoxelConstants.getLogger().error("Error loading biome color config file from litemod!", var11);
        }

    }

    public static void saveBiomeColors() {
        if (dirty) {
            File saveDir = new File(VoxelConstants.getMinecraft().gameDirectory, "/voxelmap/");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            File settingsFile = new File(saveDir, "biomecolors.txt");

            try {
                PrintWriter out = new PrintWriter(new FileWriter(settingsFile));

                for (Map.Entry<String, Integer> entry : nameToColor.entrySet()) {
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
            } catch (IOException var8) {
                VoxelConstants.getLogger().error("biome save error: " + var8.getLocalizedMessage(), var8);
            }
        }

        dirty = false;
    }

    public static int getBiomeColor(Biome biome) {
        Integer color = IDtoColor.get(biome);

        if (color != null) return color;

        if (biome == null) {
            VoxelConstants.getLogger().warn("non biome");

            return 0;
        }

        String identifier = VoxelConstants.getPlayer().level().registryAccess().lookupOrThrow(Registries.BIOME).getKey(biome).toString();
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

        IDtoColor.put(biome, color);
        return color;
    }

    @NotNull
    public static String getName(Biome biome) {
        ResourceLocation resourceLocation = VoxelConstants.getPlayer().level().registryAccess().lookupOrThrow(Registries.BIOME).getKey(biome);
        String translationKey = Util.makeDescriptionId("biome", resourceLocation);

        String name = I18n.get(translationKey);

        if (name.equals(translationKey)) return TextUtils.prettify(resourceLocation.getPath());
        return name;
    }

    @NotNull
    public static String getName(int biomeID) {
        Biome biome = VoxelConstants.getPlayer().level().registryAccess().lookupOrThrow(Registries.BIOME).byId(biomeID);

        if (biome != null) return getName(biome);
        return "Unknown";
    }
}
