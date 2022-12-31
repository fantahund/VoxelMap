package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.persistent.CachedRegion;

import java.util.concurrent.CopyOnWriteArraySet;

public interface ISettingsAndLightingChangeNotifier {
    CopyOnWriteArraySet<CachedRegion> listeners = new CopyOnWriteArraySet<>();

    void addObserver(CachedRegion var1);

    void removeObserver(CachedRegion var1);

    void notifyOfChanges();
}
