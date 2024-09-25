package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.interfaces.ISettingsManager;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class GuiOptionSliderMinimap extends AbstractSliderButton {
    private final ISettingsManager options;
    private final EnumOptionsMinimap option;

    public GuiOptionSliderMinimap(int x, int y, EnumOptionsMinimap optionIn, float value, ISettingsManager options) {
        super (x, y, 150, 20, Component.literal(options.getKeyText(optionIn)), value);
        this.options = options;
        this.option = optionIn;
    }

    protected void updateMessage() { setMessage(Component.literal(this.options.getKeyText(this.option))); }

    protected void applyValue() { this.options.setOptionFloatValue(option, (float) this.value); }

    public EnumOptionsMinimap returnEnumOptions() { return option; }

    public void setValue(float value) {
        if (isHovered()) return;

        this.value = value;
        this.updateMessage();
    }
}