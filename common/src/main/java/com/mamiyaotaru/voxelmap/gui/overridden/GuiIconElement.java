package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.util.VoxelMapGuiGraphics;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public class GuiIconElement implements Renderable, GuiEventListener {
    private final boolean cursorEvent;
    private final OnPress onPress;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean focused;

    private RenderPipeline pipeline;
    private Object icon;
    private int iconWidth;
    private int iconHeight;
    private int iconColor;

    public GuiIconElement(int x, int y, int width, int height, boolean cursorEvent, OnPress onPress) {
        this.cursorEvent = cursorEvent;
        this.onPress = onPress;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setIconForRender(RenderPipeline pipeline, Object icon, int color) {
        this.setIconForRender(pipeline, icon, this.width, this.height, color);
    }

    public void setIconForRender(RenderPipeline pipeline, Object icon, int width, int height, int color) {
        this.pipeline = pipeline;
        this.icon = icon;
        this.iconWidth = width;
        this.iconHeight = height;
        this.iconColor = color;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        if (mouseButtonEvent.button() == 0 && this.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.onPress.onPress(this);

            return true;
        }

        return false;
    }

    @Override
    public void setFocused(boolean bl) {
        this.focused = bl;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (!this.canRender()) {
            return;
        }

        float iconX = this.x + ((this.width - this.iconWidth) / 2.0F);
        float iconY = this.y + ((this.height - this.iconHeight) / 2.0F);
        this.blitIcon(guiGraphics, this.pipeline, this.icon, iconX, iconY, this.iconWidth, this.iconHeight, this.iconColor);

        if (this.cursorEvent && this.isMouseOver(mouseX, mouseY)) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private boolean canRender() {
        return this.pipeline != null && this.icon != null && this.iconWidth > 0 && this.iconHeight > 0;
    }

    private void blitIcon(GuiGraphics guiGraphics, RenderPipeline pipeline, Object icon, float x, float y, int width, int height, int color) {
        if (icon instanceof Sprite sprite) {
            sprite.blit(guiGraphics, pipeline, x, y, width, height, color);
        }
        if (icon instanceof Identifier identifier) {
            VoxelMapGuiGraphics.blitFloat(guiGraphics, pipeline, identifier, x, y, width, height, 0.0F, 1.0F, 0.0F, 1.0F, color);
        }
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

    public void setPosition(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    public void setX(int i) {
        this.x = i;
    }

    public void setY(int i) {
        this.y = i;
    }

    public void setSize(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);
    }

    public void setWidth(int i) {
        this.width = i;
    }

    public void setHeight(int i) {
        this.height = i;
    }

    public interface OnPress {
        void onPress(GuiIconElement element);
    }
}
