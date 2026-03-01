package com.mamiyaotaru.voxelmap.interfaces;

import java.io.File;
import java.io.PrintWriter;

public interface ISubSettingsManager extends ISettingsManager {
    @Override
    default void loadAll() {
    }

    @Override
    default void saveAll() {
    }

    void loadAll(File settingsFile);

    void saveAll(PrintWriter out);
}
