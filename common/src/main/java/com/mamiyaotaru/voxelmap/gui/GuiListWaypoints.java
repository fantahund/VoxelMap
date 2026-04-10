package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiIconButton;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiListMinimap;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

class GuiListWaypoints extends GuiListMinimap<GuiListWaypoints.WaypointItem> {
    private final ArrayList<WaypointItem> waypoints;
    private ArrayList<?> waypointsFiltered;
    private final GuiWaypoints parentGui;
    private String filterString = "";
    private final TextureAtlas textureAtlas;

    private static final Tooltip TOOLTIP_CLICK_TO_ENABLE = Tooltip.create(Component.translatable("minimap.waypoints.enableTooltip"));
    private static final Tooltip TOOLTIP_CLICK_TO_DISABLE = Tooltip.create(Component.translatable("minimap.waypoints.disableTooltip"));
    private static final Tooltip TOOLTIP_CLICK_TO_HIGHLIGHT = Tooltip.create(Component.translatable("minimap.waypoints.highlightTooltip"));
    private static final Tooltip TOOLTIP_CLICK_TO_UNHIGHLIGHT = Tooltip.create(Component.translatable("minimap.waypoints.removeHighlightTooltip"));

    GuiListWaypoints(GuiWaypoints parentGui, int x, int y, int width, int height) {
        super(x, y, width, height, 18);
        this.parentGui = parentGui;

        waypoints = new ArrayList<>();
        for (Waypoint pt : parentGui.waypointManager.getWaypoints()) {
            if (pt.inWorld && pt.inDimension) {
                waypoints.add(new WaypointItem(parentGui, pt));
            }
        }

        waypointsFiltered = new ArrayList<>(waypoints);
        waypointsFiltered.forEach(entry -> addEntry((WaypointItem) entry));

        textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
    }

    @Override
    public void setSelected(WaypointItem entry) {
        super.setSelected(entry);
        if (getSelected() != null) {
            GameNarrator narratorManager = new GameNarrator(minecraft);
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

    public class WaypointItem extends GuiListMinimap.Entry<WaypointItem> implements Comparable<WaypointItem> {
        private final GuiWaypoints parentGui;
        private final Waypoint waypoint;
        private final GuiIconButton waypointIcon;
        private final GuiIconButton waypointToggle;

        protected WaypointItem(GuiWaypoints parentGui, Waypoint waypoint) {
            super(GuiListWaypoints.this);

            this.parentGui = parentGui;
            this.waypoint = waypoint;

            setTooltip(Tooltip.create(Component.literal("X: " + waypoint.getX() + ", Y: " + waypoint.getY() + ", Z: " + waypoint.getZ())));
            addWidget(waypointIcon = new GuiIconButton(getX() + 2, getY(), 18, 18, button -> parentGui.setHighlightedWaypoint()));
            addWidget(waypointToggle = new GuiIconButton(getX() + getWidth() - 20, getY(), 18, 18, button -> parentGui.toggleWaypointVisibility()));
        }

        @Override
        public void renderContent(GuiGraphics drawContext, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            super.renderContent(drawContext, mouseX, mouseY, hovered, tickDelta);

            int color = waypoint.getUnifiedColor();
            drawContext.drawCenteredString(parentGui.getFont(), waypoint.name, parentGui.getWidth() / 2, getY() + 5, color);

            boolean isHighlighted = parentGui.waypointManager.isHighlightedWaypoint(waypoint);

            Sprite icon = textureAtlas.getAtlasSprite("selectable/" + waypoint.imageSuffix);
            if (icon == textureAtlas.getMissingImage()) {
                icon = textureAtlas.getAtlasSprite(WaypointManager.fallbackIconLocation);
            }
            waypointIcon.setPosition(getX() + 2, getY());
            waypointIcon.setIcon(icon, color);
            waypointIcon.setTooltip(isHighlighted ? TOOLTIP_CLICK_TO_UNHIGHLIGHT : TOOLTIP_CLICK_TO_HIGHLIGHT);

            if (isHighlighted) {
                textureAtlas.getAtlasSprite("marker/target").blit(drawContext, RenderPipelines.GUI_TEXTURED, waypointIcon.getX(), waypointIcon.getY(), waypointIcon.getWidth(), waypointIcon.getHeight(), 0xFFFF0000);
            }

            waypointToggle.setPosition(getX() + getWidth() - 20, getY());
            waypointToggle.setIcon(waypoint.enabled ? VoxelConstants.getCheckMarkerTexture() : VoxelConstants.getCrossMarkerTexture(), 0xFFFFFFFF);
            waypointToggle.setTooltip(waypoint.enabled ? GuiListWaypoints.TOOLTIP_CLICK_TO_DISABLE : GuiListWaypoints.TOOLTIP_CLICK_TO_ENABLE);
        }

        @Override
        protected boolean canDisplayTooltip(int mouseX, int mouseY) {
            return mouseX > getRowLeft() + 18 && mouseX < getRowRight() - 18 && super.canDisplayTooltip(mouseX, mouseY);
        }

        @Override
        public void onClick(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
            if (doubleClicked()) {
                parentGui.editWaypoint(parentGui.selectedWaypoint);
            }
        }

        @Override
        public int compareTo(WaypointItem o) {
            return waypoint.compareTo(o.waypoint);
        }
    }
}
