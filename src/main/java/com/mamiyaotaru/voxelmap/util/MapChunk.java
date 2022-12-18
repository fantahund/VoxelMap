package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.interfaces.IChangeObserver;
import net.minecraft.world.chunk.WorldChunk;

public class MapChunk {
    private int x = 0;
    private int z = 0;
    private WorldChunk chunk;
    private boolean isChanged = false;
    private boolean isLoaded = false;
    private boolean isSurroundedByLoaded = false;

    public MapChunk(int x, int z) {
        this.x = x;
        this.z = z;
        this.chunk = VoxelConstants.getMinecraft().world.getChunk(x, z);
        this.isLoaded = this.chunk != null && !this.chunk.isEmpty() && VoxelConstants.getMinecraft().world.isChunkLoaded(x, z);
        this.isSurroundedByLoaded = false;
        this.isChanged = true;
    }

    public void checkIfChunkChanged(IChangeObserver changeObserver) {
        if (this.hasChunkLoadedOrUnloaded() || this.isChanged) {
            changeObserver.processChunk(this.chunk);
            this.isChanged = false;
        }

    }

    private boolean hasChunkLoadedOrUnloaded() {
        boolean hasChanged = false;
        if (!this.isLoaded) {
            this.chunk = VoxelConstants.getMinecraft().world.getChunk(this.x, this.z);
            if (this.chunk != null && !this.chunk.isEmpty() && VoxelConstants.getMinecraft().world.isChunkLoaded(this.x, this.z)) {
                this.isLoaded = true;
                hasChanged = true;
            }
        } else if (this.chunk == null || this.chunk.isEmpty() || !VoxelConstants.getMinecraft().world.isChunkLoaded(this.x, this.z)) {
            this.isLoaded = false;
            hasChanged = true;
        }

        return hasChanged;
    }

    public void checkIfChunkBecameSurroundedByLoaded(IChangeObserver changeObserver) {
        this.chunk = VoxelConstants.getMinecraft().world.getChunk(this.x, this.z);
        this.isLoaded = this.chunk != null && !this.chunk.isEmpty() && VoxelConstants.getMinecraft().world.isChunkLoaded(this.x, this.z);
        if (this.isLoaded) {
            boolean formerSurroundedByLoaded = this.isSurroundedByLoaded;
            this.isSurroundedByLoaded = this.isSurroundedByLoaded();
            if (!formerSurroundedByLoaded && this.isSurroundedByLoaded) {
                changeObserver.processChunk(this.chunk);
            }
        } else {
            this.isSurroundedByLoaded = false;
        }

    }

    public boolean isSurroundedByLoaded() {
        this.chunk = VoxelConstants.getMinecraft().world.getChunk(this.x, this.z);
        this.isLoaded = this.chunk != null && !this.chunk.isEmpty() && VoxelConstants.getMinecraft().world.isChunkLoaded(this.x, this.z);
        boolean neighborsLoaded = this.isLoaded;

        for (int t = this.x - 1; t <= this.x + 1 && neighborsLoaded; ++t) {
            for (int s = this.z - 1; s <= this.z + 1 && neighborsLoaded; ++s) {
                WorldChunk neighborChunk = VoxelConstants.getMinecraft().world.getChunk(t, s);
                neighborsLoaded = neighborChunk != null && !neighborChunk.isEmpty() && VoxelConstants.getMinecraft().world.isChunkLoaded(t, s);
            }
        }

        return neighborsLoaded;
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public void setModified(boolean isModified) {
        this.isChanged = isModified;
    }
}
