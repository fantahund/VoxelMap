package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.util.RenderUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapGuiGraphics;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class GuiIconElement implements Renderable, GuiEventListener, NarratableEntry {
    private final OnPress onPress;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean focused;

    private Component tooltip;

    private Object icon;
    private int iconWidth;
    private int iconHeight;
    private int color;

    private float u0 = 0.0F;
    private float u1 = 1.0F;
    private float v0 = 0.0F;
    private float v1 = 1.0F;

    public GuiIconElement(int x, int y, int width, int height, OnPress onPress) {
        this.onPress = onPress;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public GuiIconElement setTooltip(Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public GuiIconElement setIcon(Object icon, int color) {
        return this.setIcon(icon, this.width, this.height, color);
    }

    public GuiIconElement setIcon(Object icon, int width, int height, int color) {
        this.icon = icon;
        this.iconWidth = width;
        this.iconHeight = height;
        this.color = color;

        return this;
    }

    public GuiIconElement setUV(float u0, float u1, float v0, float v1) {
        this.u0 = u0;
        this.u1 = u1;
        this.v0 = v0;
        this.v1 = v1;

        return this;
    };

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
        this.blitIcon(guiGraphics, this.icon, iconX, iconY, this.iconWidth, this.iconHeight, this.u0, this.u1, this.v0, this.v1, this.color);

        if (this.isMouseOver(mouseX, mouseY)) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            RenderUtils.drawTooltip(guiGraphics, this.tooltip, mouseX, mouseY, true);
        }
    }

    private boolean canRender() {
        return this.icon != null && this.iconWidth > 0 && this.iconHeight > 0;
    }

    private void blitIcon(GuiGraphics guiGraphics, Object icon, float x, float y, int width, int height, float u0, float u1, float v0, float v1, int color) {
        if (icon instanceof Identifier identifier) {
            VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, identifier, x, y, width, height, u0, u1, v0, v1, color);
        }
        if (icon instanceof Sprite sprite) {
            sprite.blit(guiGraphics, RenderPipelines.GUI_TEXTURED, x, y, width, height, color);
        }
        if (icon instanceof AbstractTexture texture) {
            VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, texture, x, y, width, height, u0, u1, v0, v1, color);
        }
        if (icon instanceof GpuTextureView textureView) {
            VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, textureView, x, y, width, height, u0, u1, v0, v1, color);
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

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
    }

    public interface OnPress {
        void onPress(GuiIconElement element);
    }
}
