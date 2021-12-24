package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.ButtonWidget;

public class GuiOptionButtonMinimap extends ButtonWidget {
   private final EnumOptionsMinimap enumOptions;

   public GuiOptionButtonMinimap(int buttonId, int x, int y, Text buttonText, ButtonWidget.PressAction press) {
      this(x, y, (EnumOptionsMinimap)null, buttonText, press);
   }

   public GuiOptionButtonMinimap(int x, int y, EnumOptionsMinimap par4EnumOptions, Text buttonText, ButtonWidget.PressAction press) {
      super(x, y, 150, 20, buttonText, press);
      this.enumOptions = par4EnumOptions;
   }

   public EnumOptionsMinimap returnEnumOptions() {
      return this.enumOptions;
   }
}
