package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.multiplayer.ClientLevel;

public class GameVariableAccessShim {
    public static ClientLevel getWorld() {
        return VoxelConstants.getMinecraft().level;
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
        return VoxelConstants.getMinecraft().screen != null && VoxelConstants.getMinecraft().screen.isPauseScreen() ? VoxelConstants.getMinecraft().getCameraEntity().getX() : VoxelConstants.getMinecraft().getCameraEntity().xo + (VoxelConstants.getMinecraft().getCameraEntity().getX() - VoxelConstants.getMinecraft().getCameraEntity().xo) * VoxelConstants.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    public static double zCoordDouble() {
        return VoxelConstants.getMinecraft().screen != null && VoxelConstants.getMinecraft().screen.isPauseScreen() ? VoxelConstants.getMinecraft().getCameraEntity().getZ() : VoxelConstants.getMinecraft().getCameraEntity().zo + (VoxelConstants.getMinecraft().getCameraEntity().getZ() - VoxelConstants.getMinecraft().getCameraEntity().zo) * VoxelConstants.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    public static double yCoordDouble() {
        return VoxelConstants.getMinecraft().screen != null && VoxelConstants.getMinecraft().screen.isPauseScreen() ? VoxelConstants.getMinecraft().getCameraEntity().getY() : VoxelConstants.getMinecraft().getCameraEntity().yo + (VoxelConstants.getMinecraft().getCameraEntity().getY() - VoxelConstants.getMinecraft().getCameraEntity().yo) * VoxelConstants.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    public static float rotationYaw() {
        return VoxelConstants.getMinecraft().getCameraEntity().yRotO + (VoxelConstants.getMinecraft().getCameraEntity().getYRot() - VoxelConstants.getMinecraft().getCameraEntity().yRotO) * VoxelConstants.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }
}
