package com.mamiyaotaru.voxelmap.interfaces;

import net.minecraft.world.chunk.WorldChunk;

public interface IChangeObserver {
    void handleChangeInWorld(int var1, int var2);

    void processChunk(WorldChunk var1);
}
