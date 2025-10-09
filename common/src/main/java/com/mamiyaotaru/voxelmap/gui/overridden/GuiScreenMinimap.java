package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

    public void renderTooltip(GuiGraphics drawContext, Component text, int x, int y) {
        if (!(text != null && text.getString() != null && !text.getString().isEmpty())) {
            return;
        }

//        ClientTooltipComponent clientTooltipComponent = ClientTooltipComponent.create(text.getVisualOrderText());
//        drawContext.renderTooltip(VoxelConstants.getMinecraft().font, List.of(clientTooltipComponent), x, y, DefaultTooltipPositioner.INSTANCE, null);

        Tooltip tooltip = Tooltip.create(text);
        drawContext.setTooltipForNextFrame(this.getFont(), tooltip.toCharSequence(VoxelConstants.getMinecraft()), x, y);
    }

    @Override
    public Font getFont() { return super.getFont(); }

    @Override
    public List<? extends GuiEventListener> children() { return super.children(); }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBlurredBackground(context);
        this.renderMenuBackground(context);
    }
}