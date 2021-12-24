package com.mamiyaotaru.voxelmap.util;

import java.io.File;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

public class GameVariableAccessShim {
   private static MinecraftClient minecraft = MinecraftClient.getInstance();

   public static MinecraftClient getMinecraft() {
      return minecraft;
   }

   public static ClientWorld getWorld() {
      return minecraft.world;
   }

   public static File getDataDir() {
      return minecraft.runDirectory;
   }

   public static int xCoord() {
      return (int)(minecraft.getCameraEntity().getX() < 0.0 ? minecraft.getCameraEntity().getX() - 1.0 : minecraft.getCameraEntity().getX());
   }

   public static int zCoord() {
      return (int)(minecraft.getCameraEntity().getZ() < 0.0 ? minecraft.getCameraEntity().getZ() - 1.0 : minecraft.getCameraEntity().getZ());
   }

   public static int yCoord() {
      return (int)Math.ceil(minecraft.getCameraEntity().getY());
   }

   public static double xCoordDouble() {
      return minecraft.currentScreen != null && minecraft.currentScreen.shouldPause()
         ? minecraft.getCameraEntity().getX()
         : minecraft.getCameraEntity().prevX
            + (minecraft.getCameraEntity().getX() - minecraft.getCameraEntity().prevX) * (double)minecraft.getTickDelta();
   }

   public static double zCoordDouble() {
      return minecraft.currentScreen != null && minecraft.currentScreen.shouldPause()
         ? minecraft.getCameraEntity().getZ()
         : minecraft.getCameraEntity().prevZ
            + (minecraft.getCameraEntity().getZ() - minecraft.getCameraEntity().prevZ) * (double)minecraft.getTickDelta();
   }

   public static double yCoordDouble() {
      return minecraft.currentScreen != null && minecraft.currentScreen.shouldPause()
         ? minecraft.getCameraEntity().getY()
         : minecraft.getCameraEntity().prevY
            + (minecraft.getCameraEntity().getY() - minecraft.getCameraEntity().prevY) * (double)minecraft.getTickDelta();
   }

   public static float rotationYaw() {
      return minecraft.getCameraEntity().prevYaw + (minecraft.getCameraEntity().getYaw() - minecraft.getCameraEntity().prevYaw) * minecraft.getTickDelta();
   }
}
