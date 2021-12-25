package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.persistent.PersistentMapSettingsManager;
import com.mamiyaotaru.voxelmap.util.WorldUpdateListener;

public interface IVoxelMap {
    MapSettingsManager getMapOptions();

    RadarSettingsManager getRadarOptions();

    PersistentMapSettingsManager getPersistentMapOptions();

    IMap getMap();

    IRadar getRadar();

    IColorManager getColorManager();

    IWaypointManager getWaypointManager();

    IDimensionManager getDimensionManager();

    IPersistentMap getPersistentMap();

    void setPermissions(boolean var1, boolean var2, boolean var3, boolean var4);

    void newSubWorldName(String var1, boolean var2);

    void newSubWorldHash(String var1);

    ISettingsAndLightingChangeNotifier getSettingsAndLightingChangeNotifier();

    String getWorldSeed();

    void setWorldSeed(String var1);

    void sendPlayerMessageOnMainThread(String var1);

    WorldUpdateListener getWorldUpdateListener();
}
