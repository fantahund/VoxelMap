package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class GuiIconElement {
    private final boolean changeCursor;
    private final OnPress onPress;
    private int x;
    private int y;
    private int width;
    private int height;

    public GuiIconElement(int x, int y, int width, int height, boolean changeCursor, OnPress onPress) {
        this.changeCursor = changeCursor;
        this.onPress = onPress;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean getHovered(int mouseX, int mouseY) {
        return getHovered((double) mouseX, (double) mouseY);
    }

    public boolean getHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        if (this.getHovered(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.onPress.onPress(this);

            return true;
        }

        return false;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, Sprite icon, int color) {
        this.renderInternal(guiGraphics, mouseX, mouseY, icon, this.width, this.height, color);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, Sprite icon, int iconWidth, int iconHeight, int color) {
        this.renderInternal(guiGraphics, mouseX, mouseY, icon, iconWidth, iconHeight, color);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, Identifier icon, int color) {
        this.renderInternal(guiGraphics, mouseX, mouseY, icon, this.width, this.height, color);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, Identifier icon, int iconWidth, int iconHeight, int color) {
        this.renderInternal(guiGraphics, mouseX, mouseY, icon, iconWidth, iconHeight, color);
    }

    private void renderInternal(GuiGraphics guiGraphics, int mouseX, int mouseY, Object icon, int iconWidth, int iconHeight, int color) {
        int iconX = this.x + ((this.width - iconWidth) / 2);
        int iconY = this.y + ((this.height - iconHeight) / 2);;
        if (icon instanceof Sprite sprite) {
            sprite.blit(guiGraphics, RenderPipelines.GUI_TEXTURED, iconX, iconY, iconWidth, iconHeight, color);
        }
        if (icon instanceof Identifier identifier) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, identifier, iconX, iconY, 0.0F, 0.0f, iconWidth, iconHeight, iconWidth, iconHeight, color);
        }
        if (this.changeCursor && this.getHovered(mouseX, mouseY)) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
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
        this.x = x;
        this.y = y;
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

    public interface OnPress {
        void onPress(GuiIconElement element);
    }
}
