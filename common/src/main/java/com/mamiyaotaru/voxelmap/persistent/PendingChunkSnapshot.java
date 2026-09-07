package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.util.FullMapData;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Transient copy of the persistent-map values for one 16 by 16 chunk. Keeping this separate from
 * a {@link CachedRegion} lets the client chunk be released before the region file has been loaded.
 */
final class PendingChunkSnapshot {
    static final int SIZE = 16;

    private final int chunkX;
    private final int chunkZ;
    private final FullMapData data;

    PendingChunkSnapshot(int chunkX, int chunkZ, FullMapData data) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.data = data;
    }

    static PendingChunkSnapshot capture(PersistentMap persistentMap, ClientLevel world, LevelChunk chunk) {
        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        FullMapData data = new FullMapData(SIZE, SIZE);
        MutableBlockPos blockPos = new MutableBlockPos(0, 0, 0);
        int startX = chunkX * SIZE;
        int startZ = chunkZ * SIZE;
        boolean underground = persistentMap.isUnderground(world);

        for (int z = 0; z < SIZE; ++z) {
            for (int x = 0; x < SIZE; ++x) {
                persistentMap.getAndStoreData(data, world, chunk, blockPos, underground, startX, startZ, x, z);
            }
        }

        return new PendingChunkSnapshot(chunkX, chunkZ, data);
    }

    int chunkX() {
        return this.chunkX;
    }

    int chunkZ() {
        return this.chunkZ;
    }

    FullMapData data() {
        return this.data;
    }

    void applyTo(CompressibleMapData target, int regionX, int regionZ) {
        target.applyChunkSnapshot(this, regionX, regionZ);
    }
}
