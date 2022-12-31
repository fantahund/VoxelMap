package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.ISettingsAndLightingChangeNotifier;
import com.mamiyaotaru.voxelmap.persistent.CachedRegion;

public class SettingsAndLightingChangeNotifier implements ISettingsAndLightingChangeNotifier {
    @Override
    public final void addObserver(CachedRegion listener) {
        listeners.add(listener);
    }

    @Override
    public final void removeObserver(CachedRegion listener) {
        listeners.remove(listener);
    }

    @Override
    public void notifyOfChanges() {
        for (CachedRegion listener : listeners) {
            listener.notifyOfActionableChange(this);
        }

    }
}
