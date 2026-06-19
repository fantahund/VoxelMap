package com.mamiyaotaru.voxelmap.gui.widgets;

import com.mamiyaotaru.voxelmap.gui.IPopupGuiScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class PopupGuiButton extends Button.Plain {
    private final IPopupGuiScreen parentGui;

    public PopupGuiButton(int x, int y, int width, int height, Component message, OnPress onPress, IPopupGuiScreen parentGui) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.parentGui = parentGui;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean canHover = parentGui.overPopup(mouseX, mouseY);
        if (!canHover) {
            mouseX = 0;
            mouseY = 0;
        }
        super.extractContents(graphics, mouseX, mouseY, delta);
    }
}
