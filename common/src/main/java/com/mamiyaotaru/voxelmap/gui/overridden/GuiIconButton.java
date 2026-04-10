package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.util.VoxelMapGuiGraphics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class GuiIconButton extends Button.Plain {
    private Identifier icon;
    private int iconWidth;
    private int iconHeight;
    private float u0 = 0.0F;
    private float u1 = 1.0F;
    private float v0 = 0.0F;
    private float v1 = 1.0F;
    private int color;

    public GuiIconButton(int x, int y, int width, int height, Button.OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    public void clearIcon() {
        icon = null;
    }

    public void setIcon(Sprite icon, int color) {
        setIcon(icon, getWidth(), getHeight(), color);
    }

    public void setIcon(Sprite icon, int iconWidth, int iconHeight, int color) {
        setIcon(icon.getIdentifier(), iconWidth, iconHeight, icon.getMinU(), icon.getMaxU(), icon.getMinV(), icon.getMaxV(), color);
    }

    public void setIcon(Identifier icon, int color) {
        setIcon(icon, getWidth(), getHeight(), color);
    }

    public void setIcon(Identifier icon, int iconWidth, int iconHeight, int color) {
        setIcon(icon, iconWidth, iconHeight, 0.0F, 1.0F, 0.0F, 1.0F, color);
    }

    public void setIcon(Identifier icon, float u0, float u1, float v0, float v1, int color) {
        setIcon(icon, getWidth(), getHeight(), u0, u1, v0, v1, color);
    }

    public void setIcon(Identifier icon, int iconWidth, int iconHeight, float u0, float u1, float v0, float v1, int color) {
        this.icon = icon;
        this.u0 = u0;
        this.u1 = u1;
        this.v0 = v0;
        this.v1 = v1;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
        this.color = color;
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        if (icon == null) {
            return;
        }

        float iconX = getX() + ((getWidth() - iconWidth) / 2.0F);
        float iconY = getY() + ((getHeight() - iconHeight) / 2.0F);

        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, width, height, u0, u1, v0, v1, color);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }
}
