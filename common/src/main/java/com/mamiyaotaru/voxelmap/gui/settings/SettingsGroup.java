package com.mamiyaotaru.voxelmap.gui.settings;

import java.util.List;
import net.minecraft.network.chat.Component;

public record SettingsGroup(Component title, List<SettingsOption<?>> options) {
    public SettingsGroup(String titleKey, List<SettingsOption<?>> options) {
        this(Component.translatable(titleKey), List.copyOf(options));
    }
}
