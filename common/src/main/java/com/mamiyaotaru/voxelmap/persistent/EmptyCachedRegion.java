package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.SettingsAndLightingChangeNotifier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.LevelChunk;

public class EmptyCachedRegion extends CachedRegion {
    @Override
    public void notifyOfActionableChange(SettingsAndLightingChangeNotifier notifier) {
    }

    @Override
    public void refresh(boolean forceCompress) {
    }

    @Override
    public void handleChangedChunk(LevelChunk chunk) {
    }

    @Override
    public long getMostRecentView() {
        return 0L;
    }

    @Override
    public String getKey() {
        return "";
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getZ() {
        return 0;
    }

    @Override
    public int getWidth() {
        return 256;
    }

    @Override
    public Identifier getTextureLocation() {
        return null;
    }

    @Override
    public CompressibleMapData getMapData() {
        return null;
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isGroundAt(int blockX, int blockZ) {
        return false;
    }

    @Override
    public int getHeightAt(int blockX, int blockZ) {
        return Short.MIN_VALUE;
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void compress() {
    }
}
