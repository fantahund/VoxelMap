package com.mamiyaotaru.voxelmap.interfaces;

import net.minecraft.world.chunk.WorldChunk;

public interface IChangeObserver {
    void handleChangeInWorld(int chunkX, int chunkZ);

    void processChunk(WorldChunk chunk);
}
