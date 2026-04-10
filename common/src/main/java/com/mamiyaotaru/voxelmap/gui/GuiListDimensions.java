package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiIconButton;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiListMinimap;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class GuiListDimensions extends GuiListMinimap<GuiListDimensions.DimensionItem> {
    private final GuiAddWaypoint parentGui;
    private final ArrayList<DimensionItem> dimensions;
    protected long lastClicked;
    public boolean doubleClicked;

    private static final Tooltip TOOLTIP_APPLIES = Tooltip.create(Component.translatable("minimap.waypoints.dimension.applies"));
    private static final Tooltip TOOLTIP_NOT_APPLIES = Tooltip.create(Component.translatable("minimap.waypoints.dimension.notApplies"));

    GuiListDimensions(GuiAddWaypoint parentGui, int x, int y, int width, int height) {
        super(x, y, width, height, 18);
        this.parentGui = parentGui;

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

    public class DimensionItem extends GuiListMinimap.Entry<DimensionItem> {
        private final GuiAddWaypoint parentGui;
        private final DimensionContainer dim;
        private final GuiIconButton dimToggle;

        protected DimensionItem(GuiAddWaypoint parentGui, DimensionContainer dim) {
            super(GuiListDimensions.this);
            this.parentGui = parentGui;
            this.dim = dim;
            dimToggle = new GuiIconButton(getX() + getWidth() - 20, getY(), 18, 18, element -> parentGui.toggleDimensionSelected());
        }

        @Override
        public void renderContent(GuiGraphics drawContext, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            drawContext.drawCenteredString(parentGui.getFont(), dim.getDisplayName(), (parentGui.getWidth() + getWidth()) / 2, getY() + 5, 0xFFFFFFFF);

            dimToggle.setPosition(getX() + getWidth() - 20, getY());
            dimToggle.setIcon(parentGui.dimensions.contains(dim) ? VoxelConstants.getCheckMarkerTexture() : VoxelConstants.getCrossMarkerTexture(), 0xFFFFFFFF);
            dimToggle.setTooltip(parentGui.dimensions.contains(dim) ? TOOLTIP_APPLIES : TOOLTIP_NOT_APPLIES);
            dimToggle.render(drawContext, mouseX, mouseY, tickDelta);
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
