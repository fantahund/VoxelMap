package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.util.GLShim;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.List;

public class GuiScreenMinimap extends Screen {
    protected GuiScreenMinimap() {
        this(new LiteralText(""));
    }

    protected GuiScreenMinimap(LiteralText textComponent_1) {
        super(textComponent_1);
        this.setZOffset(0);
    }

    public void drawMap(MatrixStack matrixStack) {
        if (!VoxelMap.instance.getMapOptions().showUnderMenus) {
            VoxelMap.instance.getMap().drawMinimap(matrixStack, this.client);
            GLShim.glClear(256);
        }

    }

    public void removed() {
        MapSettingsManager.instance.saveAll();
    }

    public void renderTooltip(MatrixStack matrixStack, Text text, int x, int y) {
        if (text != null && text.getString() != null && !text.getString().equals("")) {
            super.renderTooltip(matrixStack, text, x, y);
        }

    }

    public MinecraftClient getMinecraft() {
        return this.client;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public List getButtonList() {
        return this.children();
    }

    public TextRenderer getFontRenderer() {
        return this.textRenderer;
    }
}
