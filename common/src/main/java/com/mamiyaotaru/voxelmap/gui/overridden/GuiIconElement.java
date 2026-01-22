package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public class GuiIconElement {
    private final boolean changeCursor;
    private final OnPress onPress;
    private int x;
    private int y;
    private int width;
    private int height;
    private int iconWidth;
    private int iconHeight;

    public GuiIconElement(int x, int y, int width, int height, boolean changeCursor, OnPress onPress) {
        this(x, y, width, height, width, height, changeCursor, onPress);
    }

    public GuiIconElement(int x, int y, int width, int height, int iconWidth, int iconHeight, boolean changeCursor, OnPress onPress) {
        this.changeCursor = changeCursor;
        this.onPress = onPress;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
    }

    public boolean getHovered(int mouseX, int mouseY) {
        return getHovered((double) mouseX, (double) mouseY);
    }

    public boolean getHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        if (mouseButtonEvent.button() == 0 && this.getHovered(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.onPress.onPress(this);

            return true;
        }

        return false;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, RenderPipeline pipeline, Sprite icon, int color) {
        this.renderInternal(guiGraphics, mouseX, mouseY, pipeline, icon, color);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, RenderPipeline pipeline, Identifier icon, int color) {
        this.renderInternal(guiGraphics, mouseX, mouseY, pipeline, icon, color);
    }

    private void renderInternal(GuiGraphics guiGraphics, int mouseX, int mouseY, RenderPipeline pipeline, Object icon, int color) {
        int iconX = this.x + ((this.width - this.iconWidth) / 2);
        int iconY = this.y + ((this.height - this.iconHeight) / 2);
        this.blitIcon(guiGraphics, pipeline, icon, iconX, iconY, this.iconWidth, this.iconHeight, color);

        if (this.changeCursor && this.getHovered(mouseX, mouseY)) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void blitIcon(GuiGraphics guiGraphics, RenderPipeline pipeline, Object icon, int x, int y, int width, int height, int color) {
        if (icon instanceof Sprite sprite) {
            sprite.blit(guiGraphics, pipeline, x, y, width, height, color);
        }
        if (icon instanceof Identifier identifier) {
            guiGraphics.blit(pipeline, identifier, x, y, 0.0F, 0.0F, width, height, width, height, color);
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

    public int getIconWidth() {
        return this.iconWidth;
    }

    public int getIconHeight() {
        return this.iconHeight;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setX(int i) {
        this.x = i;
    }

    public void setY(int i) {
        this.y = i;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setWidth(int i) {
        this.width = i;
    }

    public void setHeight(int i) {
        this.height = i;
    }

    public void setIconSize(int width, int height) {
        this.iconWidth = width;
        this.iconHeight = height;
    }

    public void setIconWidth(int i) {
        this.iconWidth = i;
    }

    public void setIconHeight(int i) {
        this.iconHeight = i;
    }

    public interface OnPress {
        void onPress(GuiIconElement element);
    }
}
