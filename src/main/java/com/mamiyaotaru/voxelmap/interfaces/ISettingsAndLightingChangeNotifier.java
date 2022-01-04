package com.mamiyaotaru.voxelmap.interfaces;

import java.util.concurrent.CopyOnWriteArraySet;

public interface ISettingsAndLightingChangeNotifier {
    CopyOnWriteArraySet<ISettingsAndLightingChangeListener> listeners = new CopyOnWriteArraySet<>();

    void addObserver(ISettingsAndLightingChangeListener var1);

    void removeObserver(ISettingsAndLightingChangeListener var1);

    void notifyOfChanges();
}
