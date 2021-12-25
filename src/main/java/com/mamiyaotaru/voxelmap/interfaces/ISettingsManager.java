package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;

public interface ISettingsManager {
    String getKeyText(EnumOptionsMinimap var1);

    void setOptionFloatValue(EnumOptionsMinimap var1, float var2);

    float getOptionFloatValue(EnumOptionsMinimap var1);
}
