package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.gui.overridden.GuiSlotMinimap;
import com.mamiyaotaru.voxelmap.interfaces.IDimensionManager;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import java.util.ArrayList;

import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.TranslatableText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;

class GuiSlotDimensions extends GuiSlotMinimap {
   private IDimensionManager dimensionManager;
   final GuiAddWaypoint parentGui;
   private ArrayList dimensions;
   final TranslatableText APPLIES = new TranslatableText("minimap.waypoints.dimension.applies");
   final TranslatableText NOT_APPLIES = new TranslatableText("minimap.waypoints.dimension.notapplies");

   public GuiSlotDimensions(GuiAddWaypoint par1GuiWaypoints) {
      super(
         MinecraftClient.getInstance(), 101, par1GuiWaypoints.getHeight(), par1GuiWaypoints.getHeight() / 6 + 82 + 6, par1GuiWaypoints.getHeight() / 6 + 164 + 3, 18
      );
      this.parentGui = par1GuiWaypoints;
      this.setSlotWidth(88);
      this.setLeftPos(this.parentGui.getWidth() / 2);
      this.setRenderSelection(false);
      this.setShowTopBottomBG(false);
      this.setShowSlotBG(false);
      this.dimensionManager = this.parentGui.master.getDimensionManager();
      this.dimensions = new ArrayList();
      DimensionItem first = null;

      for(DimensionContainer dim : this.dimensionManager.getDimensions()) {
         DimensionItem item = new DimensionItem(this.parentGui, dim);
         this.dimensions.add(item);
         if (dim.equals(this.parentGui.waypoint.dimensions.first())) {
            first = item;
         }
      }

      this.dimensions.forEach(x$0 -> this.addEntry((EntryListWidget.Entry)x$0));
      if (first != null) {
         this.ensureVisible(first);
      }

   }

   public void setSelected(DimensionItem item) {
      super.setSelected(item);
      if (this.getSelectedOrNull() instanceof DimensionItem) {
         NarratorManager.INSTANCE
            .narrate((new TranslatableText("narrator.select", new Object[]{((DimensionItem)this.getSelectedOrNull()).dim.name})).getString());
      }

      this.parentGui.setSelectedDimension(item.dim);
   }

   protected boolean isSelectedEntry(int par1) {
      return ((DimensionItem)this.dimensions.get(par1)).dim.equals(this.parentGui.selectedDimension);
   }

   public void renderBackground(MatrixStack matrixStack) {
   }

   public class DimensionItem extends EntryListWidget.Entry<DimensionItem> {
      private final GuiAddWaypoint parentGui;
      private final DimensionContainer dim;

      protected DimensionItem(GuiAddWaypoint waypointScreen, DimensionContainer dim) {
         this.parentGui = waypointScreen;
         this.dim = dim;
      }

      public void render(
         MatrixStack matrixStack,
         int slotIndex,
         int slotYPos,
         int leftEdge,
         int entryWidth,
         int entryHeight,
         int mouseX,
         int mouseY,
         boolean mouseOver,
         float partialTicks
      ) {
         DrawableHelper.drawCenteredText(
            matrixStack,
            this.parentGui.getFontRenderer(),
            this.dim.getDisplayName(),
            this.parentGui.getWidth() / 2 + GuiSlotDimensions.this.slotWidth / 2,
            slotYPos + 3,
            16777215
         );
         byte padding = 4;
         byte iconWidth = 16;
         leftEdge = this.parentGui.getWidth() / 2;
         int width = GuiSlotDimensions.this.slotWidth;
         if (mouseX >= leftEdge + padding
            && mouseY >= slotYPos
            && mouseX <= leftEdge + width + padding
            && mouseY <= slotYPos + GuiSlotDimensions.this.itemHeight) {
            TranslatableText tooltip = null;
            if (!this.parentGui.popupOpen() && mouseX >= leftEdge + width - iconWidth - padding && mouseX <= leftEdge + width) {
               tooltip = this.parentGui.waypoint.dimensions.contains(this.dim) ? GuiSlotDimensions.this.APPLIES : GuiSlotDimensions.this.NOT_APPLIES;
            } else {
               tooltip = null;
            }

            GuiAddWaypoint.setTooltip(this.parentGui, tooltip);
         }

         GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GLUtils.img2("textures/gui/container/beacon.png");
         int xOffset = this.parentGui.waypoint.dimensions.contains(this.dim) ? 91 : 113;
         int yOffset = 222;
         this.parentGui.drawTexture(matrixStack, leftEdge + width - iconWidth, slotYPos - 2, xOffset, yOffset, 16, 16);
      }

      public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
         GuiSlotDimensions.this.setSelected(this);
         int leftEdge = this.parentGui.getWidth() / 2;
         byte padding = 4;
         byte iconWidth = 16;
         int width = GuiSlotDimensions.this.slotWidth;
         if (mouseX >= (double)(leftEdge + width - iconWidth - padding) && mouseX <= (double)(leftEdge + width)) {
            this.parentGui.toggleDimensionSelected();
         } else if (GuiSlotDimensions.this.doubleclick) {
            this.parentGui.toggleDimensionSelected();
         }

         return true;
      }
   }
}
