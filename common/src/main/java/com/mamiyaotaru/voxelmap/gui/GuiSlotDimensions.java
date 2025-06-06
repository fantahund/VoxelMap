package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import java.util.ArrayList;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

class GuiSlotDimensions extends AbstractSelectionList<GuiSlotDimensions.DimensionItem> {
    private static final Component APPLIES = Component.translatable("minimap.waypoints.dimension.applies");
    private static final Component NOT_APPLIES = Component.translatable("minimap.waypoints.dimension.notapplies");
    private static final ResourceLocation CONFIRM = ResourceLocation.parse("textures/gui/sprites/container/beacon/confirm.png");
    private static final ResourceLocation CANCEL = ResourceLocation.parse("textures/gui/sprites/container/beacon/cancel.png");

    private final GuiAddWaypoint parentGui;
    private final ArrayList<DimensionItem> dimensions;

    protected long lastClicked;
    public boolean doubleClicked;

    GuiSlotDimensions(GuiAddWaypoint par1GuiWaypoints) {
        super(VoxelConstants.getMinecraft(), 101, 64, par1GuiWaypoints.getHeight() / 6 + 90, 18);
        this.parentGui = par1GuiWaypoints;
        this.setX(this.parentGui.getWidth() / 2);
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

    @Override
    public int getRowWidth() {
        return 100;
    }

    public void setSelected(DimensionItem entry) {
        super.setSelected(entry);
        if (this.getSelected() instanceof DimensionItem) {
            GameNarrator narratorManager = new GameNarrator(VoxelConstants.getMinecraft());
            narratorManager.sayChatQueued(Component.translatable("narrator.select", (this.getSelected()).dim.name));
        }

        this.parentGui.setSelectedDimension(entry.dim);
    }

    protected boolean isSelectedItem(int index) {
        return this.dimensions.get(index).dim.equals(this.parentGui.selectedDimension);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.doubleClicked = System.currentTimeMillis() - this.lastClicked < 200L;
        this.lastClicked = System.currentTimeMillis();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public class DimensionItem extends AbstractSelectionList.Entry<DimensionItem> {
        private final GuiAddWaypoint parentGui;
        private final DimensionContainer dim;

        protected DimensionItem(GuiAddWaypoint waypointScreen, DimensionContainer dim) {
            this.parentGui = waypointScreen;
            this.dim = dim;
        }

        public void render(GuiGraphics drawContext, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            drawContext.drawCenteredString(this.parentGui.getFontRenderer(), this.dim.getDisplayName(), this.parentGui.getWidth() / 2 + GuiSlotDimensions.this.width / 2, y + 3, 16777215);
            byte padding = 4;
            byte iconWidth = 18;
            x = this.parentGui.getWidth() / 2;
            int width = GuiSlotDimensions.this.width;
            if (mouseX >= x + padding && mouseY >= y && mouseX <= x + width + padding && mouseY <= y + GuiSlotDimensions.this.itemHeight) {
                Component tooltip;
                if (this.parentGui.popupOpen() && mouseX >= x + width - iconWidth - padding && mouseX <= x + width) {
                    tooltip = this.parentGui.waypoint.dimensions.contains(this.dim) ? APPLIES : NOT_APPLIES;
                } else {
                    tooltip = null;
                }

                GuiAddWaypoint.setTooltip(this.parentGui, tooltip);
            }

            // show check mark / cross
            // 2 int: x,y screen
            // 2 float: u,v start texture (in pixels - see last 2 int)
            // 2 int: height, width on screen
            // 2 int: height, width full texture in pixels
            drawContext.blit(RenderPipelines.GUI_TEXTURED, this.parentGui.waypoint.dimensions.contains(this.dim) ? CONFIRM : CANCEL, x + width - iconWidth, y - 3, 0, 0, 18, 18, 18, 18);
            // drawContext.flush(); // FIXME 1.21.6
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseY < GuiSlotDimensions.this.getY() || mouseY > GuiSlotDimensions.this.getBottom()) {
                return false;
            }

            GuiSlotDimensions.this.setSelected(this);
            byte iconWidth = 18;
            int rightEdge = GuiSlotDimensions.this.getX() + GuiSlotDimensions.this.getWidth();
            boolean inRange = mouseX >= (rightEdge - iconWidth) && mouseX <= rightEdge;
            if (inRange && GuiSlotDimensions.this.doubleClicked) {
                this.parentGui.toggleDimensionSelected();
            }

            return true;
        }
    }
}
