package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelContants;
import net.minecraft.client.world.ClientWorld;

import java.io.File;

public class GameVariableAccessShim {
    public static ClientWorld getWorld() {
        return VoxelContants.getMinecraft().world;
    }

    public static File getDataDir() {
        return VoxelContants.getMinecraft().runDirectory;
    }

    public static int xCoord() {
        return (int) (VoxelContants.getMinecraft().getCameraEntity().getX() < 0.0 ? VoxelContants.getMinecraft().getCameraEntity().getX() - 1.0 : VoxelContants.getMinecraft().getCameraEntity().getX());
    }

    public static int zCoord() {
        return (int) (VoxelContants.getMinecraft().getCameraEntity().getZ() < 0.0 ? VoxelContants.getMinecraft().getCameraEntity().getZ() - 1.0 : VoxelContants.getMinecraft().getCameraEntity().getZ());
    }

    public static int yCoord() {
        return (int) Math.ceil(VoxelContants.getMinecraft().getCameraEntity().getY());
    }

    public static double xCoordDouble() {
        return VoxelContants.getMinecraft().currentScreen != null && VoxelContants.getMinecraft().currentScreen.shouldPause() ? VoxelContants.getMinecraft().getCameraEntity().getX() : VoxelContants.getMinecraft().getCameraEntity().prevX + (VoxelContants.getMinecraft().getCameraEntity().getX() - VoxelContants.getMinecraft().getCameraEntity().prevX) * (double) VoxelContants.getMinecraft().getTickDelta();
    }

    public static double zCoordDouble() {
        return VoxelContants.getMinecraft().currentScreen != null && VoxelContants.getMinecraft().currentScreen.shouldPause() ? VoxelContants.getMinecraft().getCameraEntity().getZ() : VoxelContants.getMinecraft().getCameraEntity().prevZ + (VoxelContants.getMinecraft().getCameraEntity().getZ() - VoxelContants.getMinecraft().getCameraEntity().prevZ) * (double) VoxelContants.getMinecraft().getTickDelta();
    }

    public static double yCoordDouble() {
        return VoxelContants.getMinecraft().currentScreen != null && VoxelContants.getMinecraft().currentScreen.shouldPause() ? VoxelContants.getMinecraft().getCameraEntity().getY() : VoxelContants.getMinecraft().getCameraEntity().prevY + (VoxelContants.getMinecraft().getCameraEntity().getY() - VoxelContants.getMinecraft().getCameraEntity().prevY) * (double) VoxelContants.getMinecraft().getTickDelta();
    }

    public static float rotationYaw() {
        return VoxelContants.getMinecraft().getCameraEntity().prevYaw + (VoxelContants.getMinecraft().getCameraEntity().getYaw() - VoxelContants.getMinecraft().getCameraEntity().prevYaw) * VoxelContants.getMinecraft().getTickDelta();
    }
}
