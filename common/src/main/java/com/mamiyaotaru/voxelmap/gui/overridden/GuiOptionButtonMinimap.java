package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class GuiOptionButtonMinimap extends Button.Plain {
    private final EnumOptionsMinimap option;

    public GuiOptionButtonMinimap(int x, int y, EnumOptionsMinimap option, Component message, OnPress onPress) {
        super (x, y, 150, 20, message, onPress, DEFAULT_NARRATION);
        this.option = option;
    }

    public EnumOptionsMinimap getOption() {
        return this.option;
    }
}