package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;

public class PopupGuiButton extends ButtonWidget {
   IPopupGuiScreen parentScreen;

   public PopupGuiButton(int buttonId, int x, int y, Text buttonText, ButtonWidget.PressAction onPress, IPopupGuiScreen parentScreen) {
      this(x, y, 200, 20, buttonText, onPress, parentScreen);
   }

   public PopupGuiButton(int x, int y, int widthIn, int heightIn, Text buttonText, ButtonWidget.PressAction pressAction, IPopupGuiScreen parentScreen) {
      super(x, y, widthIn, heightIn, buttonText, pressAction);
      this.parentScreen = parentScreen;
   }

   public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      boolean canHover = !this.parentScreen.overPopup(mouseX, mouseY);
      if (!canHover) {
         mouseX = 0;
         mouseY = 0;
      }

      super.render(matrixStack, mouseX, mouseY, partialTicks);
   }
}
