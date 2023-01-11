package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.SettingsAndLightingChangeNotifier;
import com.mamiyaotaru.voxelmap.persistent.CachedRegion;
import com.mamiyaotaru.voxelmap.persistent.PersistentMapSettingsManager;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public interface IPersistentMap extends IChangeObserver {
    void newWorld(ClientWorld world);

    void onTick();

    SettingsAndLightingChangeNotifier getSettingsAndLightingChangeNotifier();

    void setLightMapArray(int[] lights);

    void getAndStoreData(AbstractMapData mapData, World world, WorldChunk chunk, MutableBlockPos pos, boolean underground, int startX, int startZ, int imageX, int imageY);

    int getPixelColor(AbstractMapData mapData, ClientWorld world, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, boolean underground, int multi, int startX, int startZ, int imageX, int imageY);

    CachedRegion[] getRegions(int left, int right, int top, int bottom);

    boolean isRegionLoaded(int blockX, int blockZ);

    boolean isGroundAt(int blockX, int blockZ);

    int getHeightAt(int blockX, int blockZ);

    void purgeCachedRegions();

    void renameSubworld(String oldName, String newName);

    PersistentMapSettingsManager getOptions();

    void compress();
}
