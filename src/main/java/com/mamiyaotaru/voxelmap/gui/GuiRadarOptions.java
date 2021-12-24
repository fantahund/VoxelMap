package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import net.minecraft.text.Text;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public class GuiRadarOptions extends GuiScreenMinimap {
   private static final EnumOptionsMinimap[] relevantOptionsFull = new EnumOptionsMinimap[]{
      EnumOptionsMinimap.SHOWRADAR,
      EnumOptionsMinimap.RADARMODE,
      EnumOptionsMinimap.SHOWHOSTILES,
      EnumOptionsMinimap.SHOWNEUTRALS,
      EnumOptionsMinimap.SHOWPLAYERS,
      EnumOptionsMinimap.SHOWPLAYERNAMES,
      EnumOptionsMinimap.SHOWPLAYERHELMETS,
      EnumOptionsMinimap.SHOWMOBHELMETS,
      EnumOptionsMinimap.RADARFILTERING,
      EnumOptionsMinimap.RADAROUTLINES
   };
   private static final EnumOptionsMinimap[] relevantOptionsSimple = new EnumOptionsMinimap[]{
      EnumOptionsMinimap.SHOWRADAR,
      EnumOptionsMinimap.RADARMODE,
      EnumOptionsMinimap.SHOWHOSTILES,
      EnumOptionsMinimap.SHOWNEUTRALS,
      EnumOptionsMinimap.SHOWPLAYERS,
      EnumOptionsMinimap.SHOWFACING
   };
   private static EnumOptionsMinimap[] relevantOptions;
   private final Screen parent;
   private final RadarSettingsManager options;
   protected Text screenTitle;

   public GuiRadarOptions(Screen parent, IVoxelMap master) {
      this.parent = parent;
      this.options = master.getRadarOptions();
   }

   public void init() {
      this.getButtonList().clear();
      this.children().clear();
      int var2 = 0;
      this.screenTitle = new TranslatableText("options.minimap.radar.title");
      if (this.options.radarMode == 2) {
         relevantOptions = relevantOptionsFull;
      } else {
         relevantOptions = relevantOptionsSimple;
      }

      for(int t = 0; t < relevantOptions.length; ++t) {
         EnumOptionsMinimap option = relevantOptions[t];
         GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(
            this.getWidth() / 2 - 155 + var2 % 2 * 160,
            this.getHeight() / 6 + 24 * (var2 >> 1),
            option,
            new LiteralText(this.options.getKeyText(option)),
            buttonx -> this.optionClicked(buttonx)
         );
         this.addDrawableChild(optionButton);
         ++var2;
      }

      for(Object buttonObj : this.getButtonList()) {
         if (buttonObj instanceof GuiOptionButtonMinimap) {
            GuiOptionButtonMinimap button = (GuiOptionButtonMinimap)buttonObj;
            if (!button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWRADAR)) {
               button.active = this.options.showRadar;
            }

            if (button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWPLAYERS)) {
               button.active = button.active && (this.options.radarAllowed || this.options.radarPlayersAllowed);
            } else if (!button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWNEUTRALS)
               && !button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWHOSTILES)) {
               if (!button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWPLAYERHELMETS)
                  && !button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWPLAYERNAMES)) {
                  if (button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWMOBHELMETS)) {
                     button.active = button.active
                        && (this.options.showNeutrals || this.options.showHostiles)
                        && (this.options.radarAllowed || this.options.radarMobsAllowed);
                  }
               } else {
                  button.active = button.active && this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed);
               }
            } else {
               button.active = button.active && (this.options.radarAllowed || this.options.radarMobsAllowed);
            }
         }
      }

      if (this.options.radarMode == 2) {
         this.addDrawableChild(
            new ButtonWidget(
               this.getWidth() / 2 - 155,
               this.getHeight() / 6 + 144 - 6,
               150,
               20,
               new TranslatableText("options.minimap.radar.selectmobs"),
               buttonx -> this.getMinecraft().setScreen(new GuiMobs(this, this.options))
            )
         );
      }

      this.addDrawableChild(
         new ButtonWidget(
            this.getWidth() / 2 - 100,
            this.getHeight() / 6 + 168,
            200,
            20,
            new TranslatableText("gui.done"),
            buttonx -> this.getMinecraft().setScreen(this.parent)
         )
      );
   }

   protected void optionClicked(ButtonWidget buttonClicked) {
      EnumOptionsMinimap option = ((GuiOptionButtonMinimap)buttonClicked).returnEnumOptions();
      this.options.setOptionValue(option);
      if (((GuiOptionButtonMinimap)buttonClicked).returnEnumOptions().equals(EnumOptionsMinimap.RADARMODE)) {
         this.init();
      } else {
         buttonClicked.setMessage(new LiteralText(this.options.getKeyText(option)));

         for(Object buttonObj : this.getButtonList()) {
            if (buttonObj instanceof GuiOptionButtonMinimap) {
               GuiOptionButtonMinimap button = (GuiOptionButtonMinimap)buttonObj;
               if (!button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWRADAR)) {
                  button.active = this.options.showRadar;
               }

               if (button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWPLAYERS)) {
                  button.active = button.active && (this.options.radarAllowed || this.options.radarPlayersAllowed);
               } else if (!button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWNEUTRALS)
                  && !button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWHOSTILES)) {
                  if (!button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWPLAYERHELMETS)
                     && !button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWPLAYERNAMES)) {
                     if (button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWMOBHELMETS)) {
                        button.active = button.active
                           && (this.options.showNeutrals || this.options.showHostiles)
                           && (this.options.radarAllowed || this.options.radarMobsAllowed);
                     }
                  } else {
                     button.active = button.active && this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed);
                  }
               } else {
                  button.active = button.active && (this.options.radarAllowed || this.options.radarMobsAllowed);
               }
            }
         }

      }
   }

   public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      super.drawMap(matrixStack);
      this.renderBackground(matrixStack);
      drawCenteredText(matrixStack, this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
      super.render(matrixStack, mouseX, mouseY, partialTicks);
   }
}
