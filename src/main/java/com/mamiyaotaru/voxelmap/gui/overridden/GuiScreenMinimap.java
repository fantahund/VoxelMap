package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.List;

public class GuiScreenMinimap extends Screen {
    protected GuiScreenMinimap() { this (Text.literal("")); }

    protected GuiScreenMinimap(Text title) {
        super (title);
        setZOffset(0);
    }

    public void drawMap(MatrixStack matrixStack) {
        if (VoxelConstants.getVoxelMapInstance().getMapOptions().showUnderMenus) return;

        VoxelConstants.getVoxelMapInstance().getMap().drawMinimap(matrixStack);
        OpenGL.glClear(256);
    }

    public void removed() { MapSettingsManager.instance.saveAll(); }

    public void renderTooltip(MatrixStack matrices, Text text, int x, int y) {
        if (!(text != null && text.getString() != null && !text.getString().isEmpty())) return;
        super.renderTooltip(matrices, text, x, y);
    }

    public final void setZOffset(int zOffset) { super.setZOffset(zOffset); }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public List<? extends Element> getButtonList() { return children(); }

    public TextRenderer getFontRenderer() { return textRenderer; }
}