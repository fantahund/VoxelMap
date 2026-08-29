package com.mamiyaotaru.voxelmap.persistent;

import java.util.BitSet;

final class DirtyPixelMask {
    private DirtyPixelMask() {}

    static BitSet fromChunks(BitSet chunks) {
        BitSet pixels = new BitSet(CachedRegion.REGION_WIDTH * CachedRegion.REGION_WIDTH);
        for (int chunk = chunks.nextSetBit(0); chunk >= 0; chunk = chunks.nextSetBit(chunk + 1)) {
            int chunkX = chunk % 16;
            int chunkZ = chunk / 16;
            int minX = Math.max(0, chunkX * 16 - 1);
            int maxX = Math.min(CachedRegion.REGION_WIDTH - 1, chunkX * 16 + 16);
            int minZ = Math.max(0, chunkZ * 16 - 1);
            int maxZ = Math.min(CachedRegion.REGION_WIDTH - 1, chunkZ * 16 + 16);
            for (int z = minZ; z <= maxZ; ++z) {
                int rowStart = z * CachedRegion.REGION_WIDTH;
                pixels.set(rowStart + minX, rowStart + maxX + 1);
            }
        }
        return pixels;
    }
}
