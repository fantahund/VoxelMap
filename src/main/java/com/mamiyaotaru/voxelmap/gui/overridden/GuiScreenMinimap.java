package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public class GuiScreenMinimap extends Screen {
    protected GuiScreenMinimap() { this (Text.literal("")); }

    protected GuiScreenMinimap(Text title) {
        super (title);
    }

    public void removed() { MapSettingsManager.instance.saveAll(); }

    public void renderTooltip(DrawContext drawContext, Text text, int x, int y) {
        if (!(text != null && text.getString() != null && !text.getString().isEmpty())) return;
        drawContext.drawTooltip(VoxelConstants.getMinecraft().textRenderer, text, x, y);
    }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public List<? extends Element> getButtonList() { return children(); }

    public TextRenderer getFontRenderer() { return textRenderer; }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void renderBackgroundTexture(DrawContext context) {
        context.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
        context.drawTexture(VoxelConstants.getOptionsBackgroundTexture(), 0, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}