package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

public class GuiColorPickerContainer extends AbstractColorPicker {
    private final GuiColorPickerFull fullPicker;
    private final GuiColorPickerSimple simplePicker;
    private boolean simpleMode;

    public GuiColorPickerContainer(int x, int y, int width, int height, boolean simpleMode, OnColorChange onColorChange) {
        super(x, y, width, height, onColorChange);

        fullPicker = new GuiColorPickerFull(x, y, width, height, picker -> updateColor(picker.getColor()));
        simplePicker = new GuiColorPickerSimple(x, y, width, height, picker -> updateColor(picker.getColor()));
        this.simpleMode = simpleMode;
    }

    public void updateMode(boolean simpleMode) {
        if (this.simpleMode != simpleMode) {
            this.simpleMode = simpleMode;
            getColorPicker().setColor(color);
        }
    }

    private AbstractColorPicker getColorPicker() {
        return simpleMode ? simplePicker : fullPicker;
    }

    @Override
    public void setColor(int i) {
        fullPicker.setColor(i);
        simplePicker.setColor(i);

        super.setColor(i);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        getColorPicker().render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        return getColorPicker().mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        return getColorPicker().mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        return getColorPicker().mouseDragged(mouseButtonEvent, deltaX, deltaY);
    }

    @Override
    public void setX(int i) {
        fullPicker.setX(i);
        simplePicker.setX(i);

        super.setX(i);
    }

    @Override
    public void setY(int i) {
        fullPicker.setY(i);
        simplePicker.setY(i);

        super.setY(i);
    }

    @Override
    public void setWidth(int i) {
        fullPicker.setWidth(i);
        simplePicker.setWidth(i);

        super.setWidth(i);
    }

    @Override
    public void setHeight(int i) {
        fullPicker.setHeight(i);
        simplePicker.setHeight(i);

        super.setHeight(i);
    }

    @Override
    public void setPosition(int x, int y) {
        fullPicker.setPosition(x, y);
        simplePicker.setPosition(x, y);

        super.setPosition(x, y);
    }

    @Override
    public void setSize(int width, int height) {
        fullPicker.setSize(width, height);
        simplePicker.setSize(width, height);

        super.setSize(width, height);
    }
}
