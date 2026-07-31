package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class VoxelMapModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parentGui) -> VoxelConstants.getVoxelMapInstance().openOptionsScreen(parentGui);
    }
}
