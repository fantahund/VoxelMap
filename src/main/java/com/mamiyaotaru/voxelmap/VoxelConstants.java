package com.mamiyaotaru.voxelmap;

import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class VoxelConstants {
    private static final Logger LOGGER = LogManager.getLogger("VoxelMap");

    private VoxelConstants() {}

    @NotNull
    public static MinecraftClient getMinecraft() { return MinecraftClient.getInstance(); }

    public static boolean isSystemMacOS() { return MinecraftClient.IS_SYSTEM_MAC; }

    public static boolean isFabulousGraphicsOrBetter() { return MinecraftClient.isFabulousGraphicsOrBetter(); }

    @NotNull
    public static Logger getLogger() { return LOGGER; }
}