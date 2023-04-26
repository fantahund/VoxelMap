package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.interfaces.IChangeObserver;
import net.minecraft.world.chunk.WorldChunk;

public class MapChunk {
    private final int x;
    private final int z;
    private WorldChunk chunk;
    private boolean isChanged;
    private boolean isLoaded;
    private boolean isSurroundedByLoaded;

    public MapChunk(int x, int z) {
        this.x = x;
        this.z = z;
        this.chunk = VoxelConstants.getPlayer().getWorld().getChunk(x, z);
        this.isLoaded = this.chunk != null && !this.chunk.isEmpty() && VoxelConstants.getPlayer().getWorld().isChunkLoaded(x, z);
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
            this.chunk = VoxelConstants.getPlayer().getWorld().getChunk(this.x, this.z);
            if (this.chunk != null && !this.chunk.isEmpty() && VoxelConstants.getPlayer().getWorld().isChunkLoaded(this.x, this.z)) {
                this.isLoaded = true;
                hasChanged = true;
            }
        } else if (this.chunk == null || this.chunk.isEmpty() || !VoxelConstants.getPlayer().getWorld().isChunkLoaded(this.x, this.z)) {
            this.isLoaded = false;
            hasChanged = true;
        }

        return hasChanged;
    }

    public void checkIfChunkBecameSurroundedByLoaded(IChangeObserver changeObserver) {
        this.chunk = VoxelConstants.getPlayer().getWorld().getChunk(this.x, this.z);
        this.isLoaded = this.chunk != null && !this.chunk.isEmpty() && VoxelConstants.getPlayer().getWorld().isChunkLoaded(this.x, this.z);
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
        this.chunk = VoxelConstants.getPlayer().getWorld().getChunk(this.x, this.z);
        this.isLoaded = this.chunk != null && !this.chunk.isEmpty() && VoxelConstants.getPlayer().getWorld().isChunkLoaded(this.x, this.z);
        boolean neighborsLoaded = this.isLoaded;

        for (int t = this.x - 1; t <= this.x + 1 && neighborsLoaded; ++t) {
            for (int s = this.z - 1; s <= this.z + 1 && neighborsLoaded; ++s) {
                WorldChunk neighborChunk = VoxelConstants.getPlayer().getWorld().getChunk(t, s);
                neighborsLoaded = neighborChunk != null && !neighborChunk.isEmpty() && VoxelConstants.getPlayer().getWorld().isChunkLoaded(t, s);
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
