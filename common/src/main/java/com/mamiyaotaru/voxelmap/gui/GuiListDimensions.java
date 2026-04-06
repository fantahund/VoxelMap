package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiIconElement;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

class GuiListDimensions extends AbstractSelectionList<GuiListDimensions.DimensionItem> {
    private final GuiAddWaypoint parentGui;
    private final ArrayList<DimensionItem> dimensions;
    protected long lastClicked;
    public boolean doubleClicked;

    private static final Component APPLIES = Component.translatable("minimap.waypoints.dimension.applies");
    private static final Component NOT_APPLIES = Component.translatable("minimap.waypoints.dimension.notApplies");

    GuiListDimensions(GuiAddWaypoint parentGui) {
        super(VoxelConstants.getMinecraft(), 101, 64, parentGui.getHeight() / 6 + 90, 18);
        this.parentGui = parentGui;
        setX(parentGui.getWidth() / 2);

        DimensionManager dimensionManager = VoxelConstants.getVoxelMapInstance().getDimensionManager();
        dimensions = new ArrayList<>();
        DimensionItem first = null;
        for (DimensionContainer dim : dimensionManager.getDimensions()) {
            DimensionItem item = new DimensionItem(parentGui, dim);
            dimensions.add(item);
            if (dim.equals(parentGui.dimensions.first())) {
                first = item;
            }
        }

        dimensions.forEach(this::addEntry);
        if (first != null) {
            scrollToEntry(first);
        }
    }

    @Override
    public int getRowWidth() {
        return 100;
    }

    @Override
    public void setSelected(DimensionItem entry) {
        super.setSelected(entry);
        if (getSelected() != null) {
            GameNarrator narratorManager = new GameNarrator(VoxelConstants.getMinecraft());
            narratorManager.sayChatQueued(Component.translatable("narrator.select", getSelected().dim.name));
        }

        parentGui.setSelectedDimension(entry.dim);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        doubleClicked = System.currentTimeMillis() - lastClicked < 250L;
        lastClicked = System.currentTimeMillis();

        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    public class DimensionItem extends AbstractSelectionList.Entry<DimensionItem> {
        private final GuiAddWaypoint parentGui;
        private final DimensionContainer dim;
        private final GuiIconElement dimToggle;

        protected DimensionItem(GuiAddWaypoint waypointScreen, DimensionContainer dim) {
            parentGui = waypointScreen;
            this.dim = dim;
            dimToggle = new GuiIconElement(getX() + getWidth() - 20, getY(), 18, 18, element -> parentGui.toggleDimensionSelected());
        }

        @Override
        public void renderContent(GuiGraphics drawContext, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            drawContext.drawCenteredString(parentGui.getFont(), dim.getDisplayName(), (parentGui.getWidth() + getWidth()) / 2, getY() + 5, 0xFFFFFFFF);

            dimToggle.setPosition(getX() + getWidth() - 20, getY());
            dimToggle.setIcon(parentGui.dimensions.contains(dim) ? VoxelConstants.getCheckMarkerTexture() : VoxelConstants.getCrossMarkerTexture(), 0xFFFFFFFF);
            dimToggle.render(drawContext, mouseX, mouseY, tickDelta);

            if (dimToggle.isMouseOver(mouseX, mouseY)) {
                parentGui.setTooltip(parentGui.dimensions.contains(dim) ? APPLIES : NOT_APPLIES);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
            double mouseX = mouseButtonEvent.x();
            double mouseY = mouseButtonEvent.y();
            if (mouseY < getY() || mouseY > getBottom()) {
                return false;
            }

            setSelected(this);

            dimToggle.mouseClicked(mouseButtonEvent, doubleClick);

            return true;
        }
    }
}
