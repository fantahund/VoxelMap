package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.interfaces.ISettingsManager;
import net.minecraft.text.LiteralText;
import net.minecraft.client.gui.widget.SliderWidget;

public class GuiOptionSliderMinimap extends SliderWidget {
   private ISettingsManager options;
   private EnumOptionsMinimap option = null;

   public GuiOptionSliderMinimap(int x, int y, EnumOptionsMinimap optionIn, float sliderValue, ISettingsManager options) {
      super(x, y, 150, 20, new LiteralText(options.getKeyText(optionIn)), (double)sliderValue);
      this.options = options;
      this.option = optionIn;
   }

   protected void updateMessage() {
      this.setMessage(new LiteralText(this.options.getKeyText(this.option)));
   }

   protected void applyValue() {
      this.options.setOptionFloatValue(this.option, (float)this.value);
   }

   public EnumOptionsMinimap returnEnumOptions() {
      return this.option;
   }

   public void setValue(float value) {
      if (!this.isHovered()) {
         this.value = (double)value;
         this.updateMessage();
      }

   }
}
