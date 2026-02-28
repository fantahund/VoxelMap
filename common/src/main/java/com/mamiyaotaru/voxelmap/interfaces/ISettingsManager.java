package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;

public interface ISettingsManager {
    String getKeyText(EnumOptionsMinimap option);

    void loadAll();

    void saveAll();

    boolean getBooleanValue(EnumOptionsMinimap option);

    void toggleBooleanValue(EnumOptionsMinimap option);

    String getListValue(EnumOptionsMinimap option);

    void cycleListValue(EnumOptionsMinimap option);

    float getFloatValue(EnumOptionsMinimap option);

    void setFloatValue(EnumOptionsMinimap option, float value);
}
