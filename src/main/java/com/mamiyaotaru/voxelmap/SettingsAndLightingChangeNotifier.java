package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.ISettingsAndLightingChangeListener;
import com.mamiyaotaru.voxelmap.interfaces.ISettingsAndLightingChangeNotifier;

public class SettingsAndLightingChangeNotifier implements ISettingsAndLightingChangeNotifier {
    @Override
    public final void addObserver(ISettingsAndLightingChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public final void removeObserver(ISettingsAndLightingChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void notifyOfChanges() {
        for (ISettingsAndLightingChangeListener listener : listeners) {
            listener.notifyOfActionableChange(this);
        }

    }
}
