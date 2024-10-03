package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiSlotMinimap;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import java.util.ArrayList;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

class GuiSlotDimensions extends GuiSlotMinimap {
    final GuiAddWaypoint parentGui;
    private final ArrayList<DimensionItem> dimensions;
    static final Component APPLIES = Component.translatable("minimap.waypoints.dimension.applies");
    static final Component NOT_APPLIES = Component.translatable("minimap.waypoints.dimension.notapplies");

    GuiSlotDimensions(GuiAddWaypoint par1GuiWaypoints) {
        super(101, par1GuiWaypoints.getHeight(), par1GuiWaypoints.getHeight() / 6 + 82 + 6, par1GuiWaypoints.getHeight() / 6 + 164 + 3, 18);
        this.parentGui = par1GuiWaypoints;
        this.setSlotWidth(88);
        this.setLeftPos(this.parentGui.getWidth() / 2);
        //this.setRenderSelection(false); //TODO 1.20.2
        this.setShowTopBottomBG(false);
        this.setShowSlotBG(false);
        DimensionManager dimensionManager = VoxelConstants.getVoxelMapInstance().getDimensionManager();
        this.dimensions = new ArrayList<>();
        DimensionItem first = null;

        for (DimensionContainer dim : dimensionManager.getDimensions()) {
            DimensionItem item = new DimensionItem(this.parentGui, dim);
            this.dimensions.add(item);
            if (dim.equals(this.parentGui.waypoint.dimensions.first())) {
                first = item;
            }
        }

        this.dimensions.forEach(this::addEntry);
        if (first != null) {
            this.ensureVisible(first);
        }

    }

    public void setSelected(DimensionItem entry) {
        super.setSelected(entry);
        if (this.getSelected() instanceof DimensionItem) {
            GameNarrator narratorManager = new GameNarrator(VoxelConstants.getMinecraft());
            narratorManager.sayNow((Component.translatable("narrator.select", ((DimensionItem) this.getSelected()).dim.name)).getString());
        }

        this.parentGui.setSelectedDimension(entry.dim);
    }

    protected boolean isSelectedItem(int index) {
        return this.dimensions.get(index).dim.equals(this.parentGui.selectedDimension);
    }

    public class DimensionItem extends AbstractSelectionList.Entry<DimensionItem> {
        private final GuiAddWaypoint parentGui;
        private final DimensionContainer dim;

        protected DimensionItem(GuiAddWaypoint waypointScreen, DimensionContainer dim) {
            this.parentGui = waypointScreen;
            this.dim = dim;
        }

        public void render(GuiGraphics drawContext, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            drawContext.drawCenteredString(this.parentGui.getFontRenderer(), this.dim.getDisplayName(), this.parentGui.getWidth() / 2 + GuiSlotDimensions.this.slotWidth / 2, y + 3, 16777215);
            byte padding = 4;
            byte iconWidth = 16;
            x = this.parentGui.getWidth() / 2;
            int width = GuiSlotDimensions.this.slotWidth;
            if (mouseX >= x + padding && mouseY >= y && mouseX <= x + width + padding && mouseY <= y + GuiSlotDimensions.this.itemHeight) {
                Component tooltip;
                if (this.parentGui.popupOpen() && mouseX >= x + width - iconWidth - padding && mouseX <= x + width) {
                    tooltip = this.parentGui.waypoint.dimensions.contains(this.dim) ? APPLIES : NOT_APPLIES;
                } else {
                    tooltip = null;
                }

                GuiAddWaypoint.setTooltip(this.parentGui, tooltip);
            }

            OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            OpenGL.Utils.img2("textures/gui/container/beacon.png");
            int xOffset = this.parentGui.waypoint.dimensions.contains(this.dim) ? 91 : 113;
            int yOffset = 222;
            drawContext.blit(RenderType::guiTextured, ResourceLocation.parse("textures/gui/container/beacon.png"), x + width - iconWidth, y - 2, xOffset, yOffset, 16, 16, 256, 256);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            GuiSlotDimensions.this.setSelected(this);
            int leftEdge = this.parentGui.getWidth() / 2;
            byte padding = 4;
            byte iconWidth = 16;
            int width = GuiSlotDimensions.this.slotWidth;
            if (mouseX >= (leftEdge + width - iconWidth - padding) && mouseX <= (leftEdge + width)) {
                this.parentGui.toggleDimensionSelected();
            } else if (GuiSlotDimensions.this.doubleclick) {
                this.parentGui.toggleDimensionSelected();
            }

            return true;
        }
    }
}
