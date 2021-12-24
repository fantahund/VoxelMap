package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiButtonText;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public class GuiMinimapPerformance extends GuiScreenMinimap {
   private static final EnumOptionsMinimap[] relevantOptions = new EnumOptionsMinimap[]{
      EnumOptionsMinimap.LIGHTING,
      EnumOptionsMinimap.TERRAIN,
      EnumOptionsMinimap.WATERTRANSPARENCY,
      EnumOptionsMinimap.BLOCKTRANSPARENCY,
      EnumOptionsMinimap.BIOMES,
      EnumOptionsMinimap.FILTERING,
      EnumOptionsMinimap.CHUNKGRID,
      EnumOptionsMinimap.BIOMEOVERLAY,
      EnumOptionsMinimap.SLIMECHUNKS
   };
   private GuiButtonText worldSeedButton;
   private GuiOptionButtonMinimap slimeChunksButton;
   private Screen parentScreen;
   protected String screenTitle = "Details / Performance";
   private MapSettingsManager options;
   IVoxelMap master;

   public GuiMinimapPerformance(Screen par1GuiScreen, IVoxelMap master) {
      this.parentScreen = par1GuiScreen;
      this.options = master.getMapOptions();
      this.master = master;
   }

   private int getLeftBorder() {
      return this.getWidth() / 2 - 155;
   }

   public void init() {
      this.screenTitle = I18nUtils.getString("options.minimap.detailsperformance");
      this.getMinecraft().keyboard.setRepeatEvents(true);
      int leftBorder = this.getLeftBorder();
      int var2 = 0;

      for(int t = 0; t < relevantOptions.length; ++t) {
         EnumOptionsMinimap option = relevantOptions[t];
         String text = this.options.getKeyText(option);
         if ((option == EnumOptionsMinimap.WATERTRANSPARENCY || option == EnumOptionsMinimap.BLOCKTRANSPARENCY || option == EnumOptionsMinimap.BIOMES)
            && !this.options.multicore
            && this.options.getOptionBooleanValue(option)) {
            text = "§c" + text;
         }

         GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(
            leftBorder + var2 % 2 * 160, this.getHeight() / 6 + 24 * (var2 >> 1), option, new LiteralText(text), button -> this.optionClicked(button)
         );
         this.addDrawableChild(optionButton);
         ++var2;
         if (optionButton.returnEnumOptions().equals(EnumOptionsMinimap.SLIMECHUNKS)) {
            this.slimeChunksButton = optionButton;
            this.slimeChunksButton.active = this.getMinecraft().isIntegratedServerRunning() || !this.master.getWorldSeed().equals("");
         }
      }

      String worldSeedDisplay = this.master.getWorldSeed();
      if (worldSeedDisplay.equals("")) {
         worldSeedDisplay = I18nUtils.getString("selectWorld.versionUnknown");
      }

      String buttonText = I18nUtils.getString("options.minimap.worldseed") + ": " + worldSeedDisplay;
      this.worldSeedButton = new GuiButtonText(
         this.getFontRenderer(),
         leftBorder + var2 % 2 * 160,
         this.getHeight() / 6 + 24 * (var2 >> 1),
         150,
         20,
         new LiteralText(buttonText),
         button -> this.worldSeedButton.setEditing(true)
      );
      this.worldSeedButton.setText(this.master.getWorldSeed());
      this.worldSeedButton.active = !this.getMinecraft().isIntegratedServerRunning();
      this.addDrawableChild(this.worldSeedButton);
      ++var2;
      this.addDrawableChild(
         new ButtonWidget(
            this.getWidth() / 2 - 100,
            this.getHeight() / 6 + 168,
            200,
            20,
            new TranslatableText("gui.done"),
            button -> this.getMinecraft().setScreen(this.parentScreen)
         )
      );
   }

   @Override
   public void removed() {
      this.getMinecraft().keyboard.setRepeatEvents(false);
   }

   protected void optionClicked(ButtonWidget par1GuiButton) {
      EnumOptionsMinimap option = ((GuiOptionButtonMinimap)par1GuiButton).returnEnumOptions();
      this.options.setOptionValue(option);
      String perfBomb = "";
      if ((option == EnumOptionsMinimap.WATERTRANSPARENCY || option == EnumOptionsMinimap.BLOCKTRANSPARENCY || option == EnumOptionsMinimap.BIOMES)
         && !this.options.multicore
         && this.options.getOptionBooleanValue(option)) {
         perfBomb = "§c";
      }

      par1GuiButton.setMessage(new LiteralText(perfBomb + this.options.getKeyText(option)));
   }

   public boolean keyPressed(int keysm, int scancode, int b) {
      if (keysm == 258) {
         this.worldSeedButton.keyPressed(keysm, scancode, b);
      }

      if ((keysm == 257 || keysm == 335) && this.worldSeedButton.isEditing()) {
         this.newSeed();
      }

      return super.keyPressed(keysm, scancode, b);
   }

   public boolean charTyped(char character, int keycode) {
      boolean OK = super.charTyped(character, keycode);
      if (character == '\r' && this.worldSeedButton.isEditing()) {
         this.newSeed();
      }

      return OK;
   }

   private void newSeed() {
      String newSeed = this.worldSeedButton.getText();
      this.master.setWorldSeed(newSeed);
      String worldSeedDisplay = this.master.getWorldSeed();
      if (worldSeedDisplay.equals("")) {
         worldSeedDisplay = I18nUtils.getString("selectWorld.versionUnknown");
      }

      String buttonText = I18nUtils.getString("options.minimap.worldseed") + ": " + worldSeedDisplay;
      this.worldSeedButton.setMessage(new LiteralText(buttonText));
      this.worldSeedButton.setText(this.master.getWorldSeed());
      this.master.getMap().forceFullRender(true);
      this.slimeChunksButton.active = this.getMinecraft().isIntegratedServerRunning() || !this.master.getWorldSeed().equals("");
   }

   public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      super.drawMap(matrixStack);
      this.renderBackground(matrixStack);
      drawCenteredText(matrixStack, this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
      super.render(matrixStack, mouseX, mouseY, partialTicks);
   }

   public void tick() {
      this.worldSeedButton.tick();
   }
}
