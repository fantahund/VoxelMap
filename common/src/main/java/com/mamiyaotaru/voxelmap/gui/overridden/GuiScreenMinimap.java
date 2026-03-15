package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class GuiScreenMinimap extends Screen {
    protected GuiScreenMinimap() { this (Component.literal("")); }

    protected GuiScreenMinimap(Component title) {
        super (title);
    }

    @Override
    public void removed() { MapSettingsManager.instance.saveAll(); }

    public void renderTooltip(GuiGraphicsExtractor graphics, Component text, int x, int y) {
        if (!(text != null && text.getString() != null && !text.getString().isEmpty())) {
            return;
        }

        Tooltip tooltip = Tooltip.create(text);
        graphics.setTooltipForNextFrame(this.getFont(), tooltip.toCharSequence(VoxelConstants.getMinecraft()), x, y);
    }

    @Override
    public Font getFont() { return super.getFont(); }

    @Override
    public List<? extends GuiEventListener> children() { return super.children(); }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    protected Screen lastScreen;

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}