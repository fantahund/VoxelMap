package com.mamiyaotaru.voxelmap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class VoxelConstants {
    private static final Logger LOGGER = LogManager.getLogger("VoxelMap");

    private VoxelConstants() {}

    @NotNull
    public static MinecraftClient getMinecraft() { return MinecraftClient.getInstance(); }

    public static boolean isSystemMacOS() { return MinecraftClient.IS_SYSTEM_MAC; }

    public static boolean isFabulousGraphicsOrBetter() { return MinecraftClient.isFabulousGraphicsOrBetter(); }

    @NotNull
    public static Logger getLogger() { return LOGGER; }

    public static Optional<IntegratedServer> getIntegratedServer() { return Optional.ofNullable(getMinecraft().getServer()); }
}