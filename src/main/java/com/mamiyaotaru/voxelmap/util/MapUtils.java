package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;

import java.util.Random;

public class MapUtils {
    private static MapSettingsManager options;
    private static Random slimeRandom;
    private static int lastSlimeX;
    private static int lastSlimeZ;
    private static boolean isSlimeChunk;

    public static void reset() {
        options = VoxelConstants.getVoxelMapInstance().getMapOptions();
        slimeRandom = null;
        lastSlimeX = -120000;
        lastSlimeZ = 10000;
        isSlimeChunk = false;
    }

    public static int doSlimeAndGrid(int color24, int mcX, int mcZ) {
        if (options.slimeChunks && isSlimeChunk(mcX, mcZ)) {
            color24 = ColorUtils.colorAdder(2097217280, color24);
        }

        if (options.chunkGrid) {
            if (mcX % 512 != 0 && mcZ % 512 != 0) {
                if (mcX % 16 == 0 || mcZ % 16 == 0) {
                    color24 = ColorUtils.colorAdder(2097152000, color24);
                }
            } else {
                color24 = ColorUtils.colorAdder(2113863680, color24);
            }
        }

        return color24;
    }

    public static boolean isSlimeChunk(int mcX, int mcZ) {
        int xPosition = mcX >> 4;
        int zPosition = mcZ >> 4;
        String seedString = VoxelConstants.getVoxelMapInstance().getWorldSeed();
        if (!seedString.equals("")) {
            long seed;

            try {
                seed = Long.parseLong(seedString);
            } catch (NumberFormatException var8) {
                seed = seedString.hashCode();
            }

            if (xPosition != lastSlimeX || zPosition != lastSlimeZ || slimeRandom == null) {
                lastSlimeX = xPosition;
                lastSlimeZ = zPosition;
                slimeRandom = new Random(seed + ((long) xPosition * xPosition * 4987142) + (xPosition * 5947611L) + (long) zPosition * zPosition * 4392871L + (zPosition * 389711L) ^ 987234911L);
                isSlimeChunk = slimeRandom.nextInt(10) == 0;
            }
        }

        return isSlimeChunk;
    }
}
