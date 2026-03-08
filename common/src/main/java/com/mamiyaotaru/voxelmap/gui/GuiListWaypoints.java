package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiIconElement;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

class GuiListWaypoints extends AbstractSelectionList<GuiListWaypoints.WaypointItem> {
    private final ArrayList<WaypointItem> waypoints;
    private ArrayList<?> waypointsFiltered;
    private final GuiWaypoints parentGui;
    protected long lastClicked;
    private boolean doubleClicked;
    private String filterString = "";
    private final TextureAtlas textureAtlas;

    private static final Component TOOLTIP_ENABLE = Component.translatable("minimap.waypoints.enableTooltip");
    private static final Component TOOLTIP_DISABLE = Component.translatable("minimap.waypoints.disableTooltip");
    private static final Component TOOLTIP_HIGHLIGHT = Component.translatable("minimap.waypoints.highlightTooltip");
    private static final Component TOOLTIP_UNHIGHLIGHT = Component.translatable("minimap.waypoints.removeHighlightTooltip");

    GuiListWaypoints(GuiWaypoints parentGui) {
        super(VoxelConstants.getMinecraft(), parentGui.getWidth(), parentGui.getHeight() - 140, 54, 18);
        this.parentGui = parentGui;

        waypoints = new ArrayList<>();
        for (Waypoint pt : parentGui.waypointManager.getWaypoints()) {
            if (pt.inWorld && pt.inDimension) {
                waypoints.add(new WaypointItem(parentGui, pt));
            }
        }

        waypointsFiltered = new ArrayList<>(waypoints);
        waypointsFiltered.forEach(x -> addEntry((WaypointItem) x));

        textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
    }

    @Override
    public void setSelected(WaypointItem entry) {
        super.setSelected(entry);
        if (getSelected() != null) {
            GameNarrator narratorManager = new GameNarrator(VoxelConstants.getMinecraft());
            narratorManager.sayChatQueued(Component.translatable("narrator.select", getSelected().waypoint.name));
        }

        parentGui.setSelectedWaypoint(entry.waypoint);
    }

    protected void sortBy(int sortKey, boolean ascending) {
        final int order = ascending ? 1 : -1;
        if (sortKey == 1) {
            final ArrayList<?> masterWaypointsList = parentGui.waypointManager.getWaypoints();
            waypoints.sort((p1, p2) -> Double.compare(masterWaypointsList.indexOf(p1.waypoint), masterWaypointsList.indexOf(p2.waypoint)) * order);
        } else if (sortKey == 2) {
            waypoints.sort((p1, p2) -> String.CASE_INSENSITIVE_ORDER.compare(p1.waypoint.name, p2.waypoint.name) * order);
        } else if (sortKey == 3) {
            waypoints.sort(ascending ? Comparator.naturalOrder() : Collections.reverseOrder());
        }  else if (sortKey == 4) {
            waypoints.sort((p1, p2) -> {
                Waypoint waypoint1 = p1.waypoint;
                Waypoint waypoint2 = p2.waypoint;
                float hue1 = Color.RGBtoHSB((int) (waypoint1.red * 255.0F), (int) (waypoint1.green * 255.0F), (int) (waypoint1.blue * 255.0F), null)[0];
                float hue2 = Color.RGBtoHSB((int) (waypoint2.red * 255.0F), (int) (waypoint2.green * 255.0F), (int) (waypoint2.blue * 255.0F), null)[0];
                return Double.compare(hue1, hue2) * order;
            });
        }

        updateFilter(filterString);
    }

    protected void updateFilter(String filterString) {
        setScrollAmount(0.0);
        clearEntries();

        this.filterString = filterString;
        waypointsFiltered = new ArrayList<>(waypoints);
        Iterator<?> iterator = waypointsFiltered.iterator();

        while (iterator.hasNext()) {
            Waypoint waypoint = ((WaypointItem) iterator.next()).waypoint;
            if (!TextUtils.scrubCodes(waypoint.name).toLowerCase().contains(filterString)) {
                if (waypoint == parentGui.selectedWaypoint) {
                    parentGui.setSelectedWaypoint(null);
                }

                iterator.remove();
            }
        }

        waypointsFiltered.forEach(x -> addEntry((WaypointItem) x));
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

    public class WaypointItem extends AbstractSelectionList.Entry<WaypointItem> implements Comparable<WaypointItem> {
        private final GuiWaypoints parentGui;
        private final Waypoint waypoint;
        private final GuiIconElement waypointIcon;
        private final GuiIconElement waypointToggle;

        protected WaypointItem(GuiWaypoints waypointScreen, Waypoint waypoint) {
            parentGui = waypointScreen;
            this.waypoint = waypoint;
            waypointIcon = new GuiIconElement(getX() + 2, getY(), 18, 18, true, (element) -> parentGui.setHighlightedWaypoint());
            waypointToggle = new GuiIconElement(getX() + getWidth() - 20, getY(), 18, 18, true, (element) -> parentGui.toggleWaypointVisibility());
        }

        @Override
        public void renderContent(GuiGraphics drawContext, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int color = waypoint.getUnifiedColor();
            drawContext.drawCenteredString(parentGui.getFont(), waypoint.name, parentGui.getWidth() / 2, getY() + 5, color);

            Sprite icon = textureAtlas.getAtlasSprite("selectable/" + waypoint.imageSuffix);
            if (icon == textureAtlas.getMissingImage()) {
                icon = textureAtlas.getAtlasSprite(WaypointManager.fallbackIconLocation);
            }
            waypointIcon.setPosition(getX() + 2, getY());
            waypointIcon.setIconForRender(RenderPipelines.GUI_TEXTURED, icon, color);
            waypointIcon.render(drawContext, mouseX, mouseY, tickDelta);
            if (waypoint == parentGui.highlightedWaypoint) {
                waypointIcon.setIconForRender(RenderPipelines.GUI_TEXTURED, textureAtlas.getAtlasSprite("marker/target"), 0xFFFF0000);
                waypointIcon.render(drawContext, mouseX, mouseY, tickDelta);
            }

            Identifier toggleIcon = waypoint.enabled ? VoxelConstants.getCheckMarkerTexture() : VoxelConstants.getCrossMarkerTexture();
            waypointToggle.setPosition(getX() + getWidth() - 20, getY());
            waypointToggle.setIconForRender(RenderPipelines.GUI_TEXTURED, toggleIcon, 0xFFFFFFFF);
            waypointToggle.render(drawContext, mouseX, mouseY, tickDelta);

            if (waypointIcon.isMouseOver(mouseX, mouseY)) {
                parentGui.setTooltip(waypoint == parentGui.highlightedWaypoint ? TOOLTIP_UNHIGHLIGHT : TOOLTIP_HIGHLIGHT);

            } else if (waypointToggle.isMouseOver(mouseX, mouseY)) {
                parentGui.setTooltip(waypoint.enabled ? GuiListWaypoints.TOOLTIP_DISABLE : GuiListWaypoints.TOOLTIP_ENABLE);

            } else if (mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= getY() && mouseY <= getY() + getHeight()) {
                parentGui.setTooltip(Component.literal("X: " + waypoint.getX() + ", Y: " + waypoint.getY() + ", Z: " + waypoint.getZ()));

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

            boolean clicked = waypointIcon.mouseClicked(mouseButtonEvent, doubleClick) || waypointToggle.mouseClicked(mouseButtonEvent, doubleClick);

            if (!clicked && doubleClicked) {
                parentGui.editWaypoint(parentGui.selectedWaypoint);
            }

            return true;
        }

        @Override
        public int compareTo(WaypointItem o) {
            return waypoint.compareTo(o.waypoint);
        }
    }
}
