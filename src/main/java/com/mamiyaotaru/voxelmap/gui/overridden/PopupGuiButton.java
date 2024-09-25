package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class PopupGuiButton extends Button {
    final IPopupGuiScreen parentScreen;

    public PopupGuiButton(int x, int y, int width, int height, Component message, OnPress onPress, IPopupGuiScreen parentScreen) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.parentScreen = parentScreen;
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        boolean canHover = this.parentScreen.overPopup(mouseX, mouseY);
        if (!canHover) {
            mouseX = 0;
            mouseY = 0;
        }

        super.render(drawContext, mouseX, mouseY, delta);
    }
}
