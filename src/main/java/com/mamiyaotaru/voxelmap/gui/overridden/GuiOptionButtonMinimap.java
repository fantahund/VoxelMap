package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class GuiOptionButtonMinimap extends ButtonWidget {
    private final EnumOptionsMinimap enumOptions;

    public GuiOptionButtonMinimap(int x, int y, EnumOptionsMinimap par4EnumOptions, Text message, ButtonWidget.PressAction onPress) {
        super (x, y, 150, 20, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.enumOptions = par4EnumOptions;
    }

    public EnumOptionsMinimap returnEnumOptions() { return this.enumOptions; }
}