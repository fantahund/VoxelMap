package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

public class GuiColorPickerContainer extends AbstractColorPicker {
    private final GuiColorPickerFull fullPicker;
    private final GuiColorPickerSimple simplePicker;
    private boolean simpleMode;

    public GuiColorPickerContainer(int x, int y, int width, int height, boolean simpleMode, OnColorChange onColorChange) {
        super(x, y, width, height, onColorChange);

        this.fullPicker = new GuiColorPickerFull(x, y, width, height, picker -> this.updateColor(picker.getColor()));
        this.simplePicker = new GuiColorPickerSimple(x, y, width, height, picker -> this.updateColor(picker.getColor()));
        this.simpleMode = simpleMode;
    }

    public void updateMode(boolean simpleMode) {
        if (this.simpleMode != simpleMode) {
            this.simpleMode = simpleMode;
            this.getColorPicker().setColor(this.color);
        }
    }

    private AbstractColorPicker getColorPicker() {
        return this.simpleMode ? this.simplePicker : this.fullPicker;
    }

    @Override
    public void setColor(int i) {
        this.fullPicker.setColor(i);
        this.simplePicker.setColor(i);

        super.setColor(i);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.getColorPicker().render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        return this.getColorPicker().mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        return this.getColorPicker().mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        return this.getColorPicker().mouseDragged(mouseButtonEvent, deltaX, deltaY);
    }

    @Override
    public void setX(int i) {
        this.fullPicker.setX(i);
        this.simplePicker.setX(i);

        super.setX(i);
    }

    @Override
    public void setY(int i) {
        this.fullPicker.setY(i);
        this.simplePicker.setY(i);

        super.setY(i);
    }

    @Override
    public void setWidth(int i) {
        this.fullPicker.setWidth(i);
        this.simplePicker.setWidth(i);

        super.setWidth(i);
    }

    @Override
    public void setHeight(int i) {
        this.fullPicker.setHeight(i);
        this.simplePicker.setHeight(i);

        super.setHeight(i);
    }

    @Override
    public void setPosition(int x, int y) {
        this.fullPicker.setPosition(x, y);
        this.simplePicker.setPosition(x, y);

        super.setPosition(x, y);
    }

    @Override
    public void setSize(int width, int height) {
        this.fullPicker.setSize(width, height);
        this.simplePicker.setSize(width, height);

        super.setSize(width, height);
    }
}
