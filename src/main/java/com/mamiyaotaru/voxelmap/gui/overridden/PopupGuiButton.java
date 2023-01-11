package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class PopupGuiButton extends ButtonWidget {
    final IPopupGuiScreen parentScreen;

    public PopupGuiButton(int x, int y, int width, int height, Text message, ButtonWidget.PressAction onPress, IPopupGuiScreen parentScreen) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.parentScreen = parentScreen;
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        boolean canHover = this.parentScreen.overPopup(mouseX, mouseY);
        if (!canHover) {
            mouseX = 0;
            mouseY = 0;
        }

        super.render(matrices, mouseX, mouseY, delta);
    }
}
