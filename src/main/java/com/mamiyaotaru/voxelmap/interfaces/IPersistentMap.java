package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.SettingsAndLightingChangeNotifier;
import com.mamiyaotaru.voxelmap.persistent.CachedRegion;
import com.mamiyaotaru.voxelmap.persistent.PersistentMapSettingsManager;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public interface IPersistentMap extends IChangeObserver {
    void newWorld(ClientWorld var1);

    void onTick();

    SettingsAndLightingChangeNotifier getSettingsAndLightingChangeNotifier();

    void setLightMapArray(int[] var1);

    void getAndStoreData(AbstractMapData var1, World var2, WorldChunk var3, MutableBlockPos var4, boolean var5, int var6, int var7, int var8, int var9);

    int getPixelColor(AbstractMapData var1, ClientWorld var2, MutableBlockPos var3, MutableBlockPos var4, boolean var5, int var6, int var7, int var8, int var9, int var10);

    CachedRegion[] getRegions(int var1, int var2, int var3, int var4);

    boolean isRegionLoaded(int var1, int var2);

    boolean isGroundAt(int var1, int var2);

    int getHeightAt(int var1, int var2);

    void purgeCachedRegions();

    void renameSubworld(String var1, String var2);

    PersistentMapSettingsManager getOptions();

    void compress();
}
