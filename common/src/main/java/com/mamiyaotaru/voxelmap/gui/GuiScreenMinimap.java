package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiScreenMinimap extends Screen {
    protected final Screen parentGui;

    protected GuiScreenMinimap(Screen parentGui, Component title) {
        super(title);
        this.parentGui = parentGui;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.centeredText(getFont(), getTitle(), width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parentGui);
    }

    @Override
    public void removed() {
        VoxelConstants.getVoxelMapInstance().getOptionsManager().saveAll();
    }
}