package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

public class GameVariableAccessShim {
    public static ClientLevel getWorld() {
        return Minecraft.getInstance().level;
    }

    public static int xCoord() {
        return Mth.floor(Minecraft.getInstance().getCameraEntity().getX());
    }

    public static int zCoord() {
        return Mth.floor(Minecraft.getInstance().getCameraEntity().getZ());
    }

    public static int yCoord() {
        return Mth.floor(Minecraft.getInstance().getCameraEntity().getY());
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
