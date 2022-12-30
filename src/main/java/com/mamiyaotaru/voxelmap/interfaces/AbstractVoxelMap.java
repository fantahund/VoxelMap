package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.persistent.PersistentMapSettingsManager;
import com.mamiyaotaru.voxelmap.util.WorldUpdateListener;

public abstract class AbstractVoxelMap {
    public static AbstractVoxelMap instance = null;

    public abstract MapSettingsManager getMapOptions();

    public abstract RadarSettingsManager getRadarOptions();

    public abstract PersistentMapSettingsManager getPersistentMapOptions();

    public abstract IMap getMap();

    public abstract IRadar getRadar();

    public abstract IColorManager getColorManager();

    public abstract IWaypointManager getWaypointManager();

    public abstract IDimensionManager getDimensionManager();

    public abstract IPersistentMap getPersistentMap();

    public abstract void setPermissions(boolean hasFullRadarPermission, boolean hasPlayersOnRadarPermission, boolean hasMobsOnRadarPermission, boolean hasCavemodePermission);

    public abstract void newSubWorldName(String name, boolean fromServer);

    public abstract void newSubWorldHash(String hash);

    public abstract ISettingsAndLightingChangeNotifier getSettingsAndLightingChangeNotifier();

    public abstract String getWorldSeed();

    public abstract void setWorldSeed(String seed);

    public abstract void sendPlayerMessageOnMainThread(String message);

    public abstract WorldUpdateListener getWorldUpdateListener();

    public static AbstractVoxelMap getInstance() { return instance; }
}