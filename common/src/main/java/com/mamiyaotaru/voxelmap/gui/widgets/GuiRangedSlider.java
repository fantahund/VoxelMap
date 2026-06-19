package com.mamiyaotaru.voxelmap.gui.widgets;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

public class GuiRangedSlider extends AbstractSliderButton {
    private final double min;
    private final double max;
    private final Consumer<GuiRangedSlider> onChange;

    public GuiRangedSlider(int x, int y, int width, int height, Component component, double value, double min, double max, Consumer<GuiRangedSlider> onChange) {
        super(x, y, width, height, component, Mth.inverseLerp(value, min, max));
        this.min = min;
        this.max = max;
        this.onChange = onChange;
    }

    @Override
    protected void updateMessage() {
    }

    @Override
    protected void applyValue() {
        onChange.accept(this);
    }

    public double getUnscaled() {
        return Mth.lerp(value, min, max);
    }

    public void setUnscaled(double value) {
        this.value = Mth.inverseLerp(value, min, max);
    }
}
