package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.options.containers.MapOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Objects;
import java.util.Random;

public final class MapUtils {
    private static MapOptions options;
    private static Random slimeRandom = new Random();
    private static String lastSeed;
    private static long lastSeedLong;
    private static int lastSlimeX;
    private static int lastSlimeZ;
    private static boolean isSlimeChunk;

    private MapUtils() {
    }

    public static void reset() {
        options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    public static boolean isUnderground(ClientLevel world, BlockPos blockPos, int compareY) {
        if (world.dimensionType().hasCeiling() || !world.dimensionType().hasSkyLight()) {
            return compareY < world.getChunk(blockPos).getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX(), blockPos.getZ());
        }
        return world.getBrightness(LightLayer.SKY, blockPos) <= 0;
    }

    public static int doSlimeAndGrid(int color24, ClientLevel world, int mcX, int mcZ) {
        if (options.slimeChunks.get() && isSlimeChunk(mcX, mcZ)) {
            color24 = ColorUtils.colorAdder(0x7D00FF00, color24);
        }

        if (options.chunkGrid.get()) {
            if (mcX % 512 != 0 && mcZ % 512 != 0) {
                if (mcX % 16 == 0 || mcZ % 16 == 0) {
                    color24 = ColorUtils.colorAdder(0x7D000000, color24);
                }
            } else {
                color24 = ColorUtils.colorAdder(0x7DFF0000, color24);
            }
        }

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
