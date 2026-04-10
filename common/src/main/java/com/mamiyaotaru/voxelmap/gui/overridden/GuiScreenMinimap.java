package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import net.minecraft.client.gui.GuiGraphics;
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.drawCenteredString(getFont(), getTitle(), width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parentGui);
    }

    @Override
    public void removed() {
        MapSettingsManager.instance.saveAll();
    }
}