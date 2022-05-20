package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.interfaces.ISettingsManager;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class GuiOptionSliderMinimap extends SliderWidget {
    private final ISettingsManager options;
    private final EnumOptionsMinimap option;

    public GuiOptionSliderMinimap(int x, int y, EnumOptionsMinimap optionIn, float sliderValue, ISettingsManager options) {
        super(x, y, 150, 20, Text.literal(options.getKeyText(optionIn)), (double) sliderValue);
        this.options = options;
        this.option = optionIn;
    }

    protected void updateMessage() {
        this.setMessage(Text.literal(this.options.getKeyText(this.option)));
    }

    protected void applyValue() {
        this.options.setOptionFloatValue(this.option, (float) this.value);
    }

    public EnumOptionsMinimap returnEnumOptions() {
        return this.option;
    }

    public void setValue(float value) {
        if (!this.isHovered()) {
            this.value = value;
            this.updateMessage();
        }

    }
}
