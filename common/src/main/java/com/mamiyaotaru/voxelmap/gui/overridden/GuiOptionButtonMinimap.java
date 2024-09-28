package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class GuiOptionButtonMinimap extends Button {
    private final EnumOptionsMinimap enumOptions;

    public GuiOptionButtonMinimap(int x, int y, EnumOptionsMinimap par4EnumOptions, Component message, OnPress onPress) {
        super (x, y, 150, 20, message, onPress, DEFAULT_NARRATION);
        this.enumOptions = par4EnumOptions;
    }

    public EnumOptionsMinimap returnEnumOptions() { return this.enumOptions; }
}