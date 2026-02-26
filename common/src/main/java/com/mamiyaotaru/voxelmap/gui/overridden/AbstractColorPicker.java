package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class AbstractColorPicker implements Renderable, GuiEventListener {
    private final OnColorChange onColorChange;
    private boolean focused;
    protected int color;
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    public AbstractColorPicker(int x, int y, int width, int height, OnColorChange onColorChange) {
        this.onColorChange = onColorChange;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setColor(int i) {
        this.updateColor(i);
    }

    protected void updateColor(int i) {
        if (i != this.color) {
            this.color = i;
            this.onColorChange.onColorChange(this);
        }
    }

    @Override
    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float delta);

    @Override
    public abstract boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick);

    @Override
    public abstract boolean mouseReleased(MouseButtonEvent mouseButtonEvent);

    @Override
    public abstract boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY);

    @Override
    public void setFocused(boolean bl) {
        this.focused = bl;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    public int getColor() {
        return this.color;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setX(int i) {
        this.x = i;
    }

    public void setY(int i) {
        this.y = i;
    }

    public void setWidth(int i) {
        this.width = i;
    }

    public void setHeight(int i) {
        this.height = i;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public interface OnColorChange {
        void onColorChange(AbstractColorPicker picker);
    }
}
