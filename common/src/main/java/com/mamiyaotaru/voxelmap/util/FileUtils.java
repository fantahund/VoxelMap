package com.mamiyaotaru.voxelmap.util;

import net.minecraft.client.Minecraft;

import java.io.File;

public class FileUtils {
    public static File join(File base, String... paths) {
        File result = base;
        for (String path : paths) {
            result = new File(result, path);
        }
        return result;
    }

    public static String withExtension(String name, String extension) {
        return name + "." + extension;
    }

    public static File minecraftPath() {
        return Minecraft.getInstance().gameDirectory;
    }

    public static File voxelMapPath() {
        return join(minecraftPath(), "voxelmap");
    }

    public static File legacyVoxelMapPath() {
        return join(minecraftPath(), "mods", "mamiyaotaru", "voxelmap");
    }
}
