package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.world.ClientWorld;

import java.io.File;

public class GameVariableAccessShim {
    public static ClientWorld getWorld() {
        return VoxelConstants.getMinecraft().world;
    }

    public static File getDataDir() {
        return VoxelConstants.getMinecraft().runDirectory;
    }

    public static int xCoord() {
        return (int) (VoxelConstants.getMinecraft().getCameraEntity().getX() < 0.0 ? VoxelConstants.getMinecraft().getCameraEntity().getX() - 1.0 : VoxelConstants.getMinecraft().getCameraEntity().getX());
    }

    public static int zCoord() {
        return (int) (VoxelConstants.getMinecraft().getCameraEntity().getZ() < 0.0 ? VoxelConstants.getMinecraft().getCameraEntity().getZ() - 1.0 : VoxelConstants.getMinecraft().getCameraEntity().getZ());
    }

    public static int yCoord() {
        return (int) Math.ceil(VoxelConstants.getMinecraft().getCameraEntity().getY());
    }

    public static double xCoordDouble() {
        return VoxelConstants.getMinecraft().currentScreen != null && VoxelConstants.getMinecraft().currentScreen.shouldPause() ? VoxelConstants.getMinecraft().getCameraEntity().getX() : VoxelConstants.getMinecraft().getCameraEntity().prevX + (VoxelConstants.getMinecraft().getCameraEntity().getX() - VoxelConstants.getMinecraft().getCameraEntity().prevX) * (double) VoxelConstants.getMinecraft().getTickDelta();
    }

    public static double zCoordDouble() {
        return VoxelConstants.getMinecraft().currentScreen != null && VoxelConstants.getMinecraft().currentScreen.shouldPause() ? VoxelConstants.getMinecraft().getCameraEntity().getZ() : VoxelConstants.getMinecraft().getCameraEntity().prevZ + (VoxelConstants.getMinecraft().getCameraEntity().getZ() - VoxelConstants.getMinecraft().getCameraEntity().prevZ) * (double) VoxelConstants.getMinecraft().getTickDelta();
    }

    public static double yCoordDouble() {
        return VoxelConstants.getMinecraft().currentScreen != null && VoxelConstants.getMinecraft().currentScreen.shouldPause() ? VoxelConstants.getMinecraft().getCameraEntity().getY() : VoxelConstants.getMinecraft().getCameraEntity().prevY + (VoxelConstants.getMinecraft().getCameraEntity().getY() - VoxelConstants.getMinecraft().getCameraEntity().prevY) * (double) VoxelConstants.getMinecraft().getTickDelta();
    }

    public static float rotationYaw() {
        return VoxelConstants.getMinecraft().getCameraEntity().prevYaw + (VoxelConstants.getMinecraft().getCameraEntity().getYaw() - VoxelConstants.getMinecraft().getCameraEntity().prevYaw) * VoxelConstants.getMinecraft().getTickDelta();
    }
}
