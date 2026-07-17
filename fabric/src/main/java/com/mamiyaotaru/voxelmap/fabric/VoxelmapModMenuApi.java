package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.gui.GuiMinimapOptions;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class VoxelmapModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return GuiMinimapOptions::new;
    }
}
