package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.interfaces.ISettingsManager;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class GuiOptionSliderMinimap extends AbstractSliderButton {
    private final ISettingsManager options;
    private final EnumOptionsMinimap option;

    public GuiOptionSliderMinimap(int x, int y, EnumOptionsMinimap option, float value, ISettingsManager options) {
        super (x, y, 150, 20, Component.literal(options.getKeyText(option)), value);
        this.options = options;
        this.option = option;
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(options.getKeyText(option)));
    }

    @Override
    protected void applyValue() {
        options.setFloatValue(option, (float) value);
    }

    public EnumOptionsMinimap getOption() {
        return option;
    }

    public void setValue(float value) {
        if (isHovered()) {
            return;
        }

        this.value = value;
        this.updateMessage();
    }
}