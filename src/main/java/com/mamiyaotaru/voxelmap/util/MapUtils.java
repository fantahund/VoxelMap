package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.world.ClientWorld;

public class MapUtils {
    private static MapSettingsManager options;
    private static Random slimeRandom = new Random();
    private static String lastSeed;
    private static long lastSeedLong;
    private static int lastSlimeX;
    private static int lastSlimeZ;
    private static boolean isSlimeChunk;

    public static void reset() {
        options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    public static int doSlimeAndGrid(int color24, ClientWorld world, int mcX, int mcZ) {
        if (options.slimeChunks && isSlimeChunk(mcX, mcZ)) {
            color24 = ColorUtils.colorAdder(0x7D00FF00, color24);
        }

        if (options.chunkGrid) {
            if (mcX % 512 != 0 && mcZ % 512 != 0) {
                if (mcX % 16 == 0 || mcZ % 16 == 0) {
                    color24 = ColorUtils.colorAdder(0x7D000000, color24);
                }
            } else {
                color24 = ColorUtils.colorAdder(0x7DFF0000, color24);
            }
        }

        // if (options.worldborder) {
        // int wbEast = (int) Math.round(world.getWorldBorder().getBoundEast()); // +x
        // int wbWest = (int) Math.round(world.getWorldBorder().getBoundWest()); // -x
        // int wbSouth = (int) Math.round(world.getWorldBorder().getBoundSouth()); // +z
        // int wbNorth = (int) Math.round(world.getWorldBorder().getBoundNorth()); // -z
        // if (((mcX == wbEast || mcX == wbWest) && mcZ >= wbNorth && mcZ <= wbSouth) ||
        // ((mcZ == wbNorth || mcZ == wbSouth) && mcX >= wbWest && mcX <= wbEast)) {
        // color24 = ColorUtils.colorAdder(0xADFF0000, color24);
        // }
        // }

        return color24;
    }

    public synchronized static boolean isSlimeChunk(int mcX, int mcZ) {
        int xPosition = mcX >> 4;
        int zPosition = mcZ >> 4;
        String seedString = VoxelConstants.getVoxelMapInstance().getWorldSeed();
        if (seedString.isEmpty()) {
            return false;
        }
        if (!Objects.equals(lastSeed, seedString)) {
            lastSeed = seedString;
            lastSlimeX = Integer.MIN_VALUE;
            try {
                lastSeedLong = Long.parseLong(seedString);
            } catch (NumberFormatException var8) {
                lastSeedLong = seedString.hashCode();
            }
        }

        if (xPosition != lastSlimeX || zPosition != lastSlimeZ) {
            lastSlimeX = xPosition;
            lastSlimeZ = zPosition;
            slimeRandom.setSeed(lastSeedLong + (int) (xPosition * xPosition * 0x4C1906) + (int) (xPosition * 0x5ac0db) + (int) (zPosition * zPosition) * 0x4307a7L + (int) (zPosition * 0x5f24f) ^ 0x3ad8025fL);
            isSlimeChunk = slimeRandom.nextInt(10) == 0;
        }

        return isSlimeChunk;
    }
}
