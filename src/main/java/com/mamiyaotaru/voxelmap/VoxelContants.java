package com.mamiyaotaru.voxelmap;

import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

public class VoxelContants {
    private VoxelContants() {}

    @NotNull
    public static MinecraftClient getMinecraft() { return MinecraftClient.getInstance(); }

    public static boolean isSystemMacOS() { return MinecraftClient.IS_SYSTEM_MAC; }

    public static boolean isFabulousGraphicsOrBetter() { return MinecraftClient.isFabulousGraphicsOrBetter(); }
}