package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;

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
        ClientTooltipComponent clientTooltipComponent = ClientTooltipComponent.create(text.getVisualOrderText());
        drawContext.renderTooltip(VoxelConstants.getMinecraft().font, List.of(clientTooltipComponent), x, y, DefaultTooltipPositioner.INSTANCE, null);
    }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public List<? extends GuiEventListener> getButtonList() { return children(); }

    public Font getFontRenderer() { return font; }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
    }

    public void renderBackgroundTexture(GuiGraphics context) {
        this.renderBlurredBackground(context);
        this.renderMenuBackground(context);
    }
}