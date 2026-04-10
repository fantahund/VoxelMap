package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public abstract class AbstractColorPicker extends AbstractWidget {
    private final OnColorChange onColorChange;
    protected int color;

    public AbstractColorPicker(int x, int y, int width, int height, OnColorChange onColorChange) {
        super(x, y, width, height, Component.empty());
        this.onColorChange = onColorChange;
    }

    public void setColor(int i) {
        updateColor(i);
    }

    protected void updateColor(int i) {
        if (i != color) {
            color = i;
            onColorChange.onColorChange(this);
        }
    }

    public int getColor() {
        return color;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public interface OnColorChange {
        void onColorChange(AbstractColorPicker picker);
    }
}
