package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;

public interface ISettingsManager {
    String getKeyText(EnumOptionsMinimap options);

    void setOptionFloatValue(EnumOptionsMinimap options, float value);

    float getOptionFloatValue(EnumOptionsMinimap options);
}
