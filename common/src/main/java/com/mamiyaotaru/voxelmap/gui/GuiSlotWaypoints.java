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
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

class GuiSlotWaypoints extends AbstractSelectionList<GuiSlotWaypoints.WaypointItem> {
    private final ArrayList<WaypointItem> waypoints;
    private ArrayList<?> waypointsFiltered;
    final GuiWaypoints parentGui;
    private String filterString = "";
    static final Component TOOLTIP_ENABLE = Component.translatable("minimap.waypoints.enableTooltip");
    static final Component TOOLTIP_DISABLE = Component.translatable("minimap.waypoints.disableTooltip");
    static final Component TOOLTIP_HIGHLIGHT = Component.translatable("minimap.waypoints.highlightTooltip");
    static final Component TOOLTIP_UNHIGHLIGHT = Component.translatable("minimap.waypoints.removeHighlightTooltip");
    protected long lastClicked;
    public boolean doubleClicked;
    private final TextureAtlas textureAtlas;

    GuiSlotWaypoints(GuiWaypoints par1GuiWaypoints) {
        super(VoxelConstants.getMinecraft(), par1GuiWaypoints.getWidth(), par1GuiWaypoints.getHeight() - 140, 54, 18);
        this.parentGui = par1GuiWaypoints;
        this.waypoints = new ArrayList<>();

        for (Waypoint pt : this.parentGui.waypointManager.getWaypoints()) {
            if (pt.inWorld && pt.inDimension) {
                this.waypoints.add(new WaypointItem(this.parentGui, pt));
            }
        }

        this.waypointsFiltered = new ArrayList<>(this.waypoints);
        this.waypointsFiltered.forEach(x -> this.addEntry((WaypointItem) x));

        this.textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
    }

    @Override
    public void setSelected(WaypointItem entry) {
        super.setSelected(entry);
        if (this.getSelected() instanceof WaypointItem) {
            GameNarrator narratorManager = new GameNarrator(VoxelConstants.getMinecraft());
            narratorManager.sayChatQueued(Component.translatable("narrator.select", this.getSelected().waypoint.name)); // FIXME 1.21.6 narrator?
        }

        this.parentGui.setSelectedWaypoint(entry.waypoint);
    }

    // FIXME 1.21.9
    // @Override
    // protected boolean isSelectedItem(int index) {
    // return ((WaypointItem) this.waypointsFiltered.get(index)).waypoint.equals(this.parentGui.selectedWaypoint);
    // }

    protected void sortBy(int sortKey, boolean ascending) {
        final int order = ascending ? 1 : -1;
        if (sortKey == 1) {
            final ArrayList<?> masterWaypointsList = this.parentGui.waypointManager.getWaypoints();
            this.waypoints.sort((waypointEntry1, waypointEntry2) -> Double.compare(masterWaypointsList.indexOf(waypointEntry1.waypoint), masterWaypointsList.indexOf(waypointEntry2.waypoint)) * order);
        } else if (sortKey == 3) {
            if (ascending) {
                Collections.sort(this.waypoints);
            } else {
                this.waypoints.sort(Collections.reverseOrder());
            }
        } else if (sortKey == 2) {
            this.waypoints.sort((waypointEntry1, waypointEntry2) -> String.CASE_INSENSITIVE_ORDER.compare(waypointEntry1.waypoint.name, waypointEntry2.waypoint.name) * order);
        } else if (sortKey == 4) {
            this.waypoints.sort((waypointEntry1, waypointEntry2) -> {
                Waypoint waypoint1 = waypointEntry1.waypoint;
                Waypoint waypoint2 = waypointEntry2.waypoint;
                float hue1 = Color.RGBtoHSB((int) (waypoint1.red * 255.0F), (int) (waypoint1.green * 255.0F), (int) (waypoint1.blue * 255.0F), null)[0];
                float hue2 = Color.RGBtoHSB((int) (waypoint2.red * 255.0F), (int) (waypoint2.green * 255.0F), (int) (waypoint2.blue * 255.0F), null)[0];
                return Double.compare(hue1, hue2) * order;
            });
        }

        this.updateFilter(this.filterString);
    }

    protected void updateFilter(String filterString) {
        this.clearEntries();
        this.filterString = filterString;
        this.waypointsFiltered = new ArrayList<>(this.waypoints);
        Iterator<?> iterator = this.waypointsFiltered.iterator();

        while (iterator.hasNext()) {
            Waypoint waypoint = ((WaypointItem) iterator.next()).waypoint;
            if (!TextUtils.scrubCodes(waypoint.name).toLowerCase().contains(filterString)) {
                if (waypoint == this.parentGui.selectedWaypoint) {
                    this.parentGui.setSelectedWaypoint(null);
                }

                iterator.remove();
            }
        }

        this.waypointsFiltered.forEach(x -> this.addEntry((WaypointItem) x));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        this.doubleClicked = System.currentTimeMillis() - this.lastClicked < 250L;
        this.lastClicked = System.currentTimeMillis();
        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    public class WaypointItem extends AbstractSelectionList.Entry<WaypointItem> implements Comparable<WaypointItem> {
        private final GuiWaypoints parentGui;
        private final Waypoint waypoint;
        private final GuiIconElement waypointIcon;
        private final GuiIconElement waypointToggle;

        protected WaypointItem(GuiWaypoints waypointScreen, Waypoint waypoint) {
            this.parentGui = waypointScreen;
            this.waypoint = waypoint;
            this.waypointIcon = new GuiIconElement(this.getX() + 2, this.getY(), 18, 18, true, (element) -> this.parentGui.setHighlightedWaypoint());
            this.waypointToggle = new GuiIconElement(this.getX() + this.getWidth() - 20, this.getY(), 18, 18, true, (element) -> this.parentGui.toggleWaypointVisibility());
        }

        @Override
        public void renderContent(GuiGraphics drawContext, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            drawContext.drawCenteredString(this.parentGui.getFont(), this.waypoint.name, this.parentGui.getWidth() / 2, this.getY() + 5, this.waypoint.getUnifiedColor());

            Sprite icon = textureAtlas.getAtlasSprite("selectable/" + waypoint.imageSuffix);
            if (icon == textureAtlas.getMissingImage()) {
                icon = textureAtlas.getAtlasSprite(WaypointManager.fallbackIconLocation);
            }
            this.waypointIcon.setPosition(this.getX() + 2, this.getY());
            this.waypointIcon.render(drawContext, mouseX, mouseY, icon, this.waypoint.getUnifiedColor());
            if (this.waypoint == this.parentGui.highlightedWaypoint) {
                this.waypointIcon.render(drawContext, mouseX, mouseY, textureAtlas.getAtlasSprite("marker/target"), 0xFFFF0000);
            }

            this.waypointToggle.setPosition(this.getX() + this.getWidth() - 20, this.getY());
            this.waypointToggle.render(drawContext, mouseX, mouseY, this.waypoint.enabled ? VoxelConstants.getCheckMarkerTexture() : VoxelConstants.getCrossMarkerTexture(), 0xFFFFFFFF);

            if (this.waypointIcon.getHovered(mouseX, mouseY)) {
                GuiWaypoints.setTooltip(this.parentGui, this.waypoint == this.parentGui.highlightedWaypoint ? TOOLTIP_UNHIGHLIGHT : TOOLTIP_HIGHLIGHT);
            } else if (this.waypointToggle.getHovered(mouseX, mouseY)) {
                GuiWaypoints.setTooltip(this.parentGui, this.waypoint.enabled ? GuiSlotWaypoints.TOOLTIP_DISABLE : GuiSlotWaypoints.TOOLTIP_ENABLE);
            } else if (mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight()) {
                GuiWaypoints.setTooltip(this.parentGui, Component.literal("X: " + this.waypoint.getX() + ", Y: " + this.waypoint.getY() + ", Z: " + this.waypoint.getZ()));
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
            double mouseX = mouseButtonEvent.x();
            double mouseY = mouseButtonEvent.y();
            if (mouseY < GuiSlotWaypoints.this.getY() || mouseY > GuiSlotWaypoints.this.getBottom()) {
                return false;
            }

            GuiSlotWaypoints.this.setSelected(this);

            boolean clicked = this.waypointIcon.mouseClicked(mouseButtonEvent, doubleClick) || this.waypointToggle.mouseClicked(mouseButtonEvent, doubleClick);

            if (!clicked && GuiSlotWaypoints.this.doubleClicked) {
                this.parentGui.editWaypoint(this.parentGui.selectedWaypoint);
            }

            return true;
        }

        @Override
        public int compareTo(WaypointItem o) {
            return this.waypoint.compareTo(o.waypoint);
        }
    }
}
