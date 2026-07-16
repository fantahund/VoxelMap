package com.mamiyaotaru.voxelmap.gui.settings;

import java.util.List;
import net.minecraft.network.chat.Component;

public record SettingsCategory(String id, Component title, List<SettingsGroup> groups, SpecialView specialView) {
    public enum SpecialView {
        NONE,
        KEY_BINDINGS
    }

    public SettingsCategory(String id, String titleKey, List<SettingsGroup> groups) {
        this(id, Component.translatable(titleKey), List.copyOf(groups), SpecialView.NONE);
    }

    public SettingsCategory(String id, String titleKey, List<SettingsGroup> groups, SpecialView specialView) {
        this(id, Component.translatable(titleKey), List.copyOf(groups), specialView);
    }
}
