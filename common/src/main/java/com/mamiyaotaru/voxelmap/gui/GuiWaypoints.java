package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.widgets.GuiIconButton;
import com.mamiyaotaru.voxelmap.gui.widgets.GuiListMinimap;
import com.mamiyaotaru.voxelmap.options.containers.WaypointOptions;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;

public class GuiWaypoints extends GuiScreenMinimap implements IGuiWaypoints {
    private final Random generator = new Random();
    private final WaypointOptions options;
    private final WaypointManager waypointManager;
    private final TextureAtlas waypointAtlas;

    private WaypointList waypointList;
    private EditBox filter;
    private Button buttonEdit;
    private Button buttonDelete;
    private Button buttonHighlight;
    private Button buttonShare;
    private Button buttonTeleport;
    private Button buttonSortName;
    private Button buttonSortCreated;
    private Button buttonSortDistance;
    private Button buttonSortColor;

    private Waypoint selectedWaypoint;
    private Waypoint newWaypoint;

    private boolean changedSort;
    private boolean addClicked;
    private boolean editClicked;
    private boolean deleteClicked;

    public GuiWaypoints(Screen parentGui) {
        super(parentGui, Component.translatable("minimap.waypoints.title"));
        options = VoxelConstants.getVoxelMapInstance().getWaypointOptions();
        waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        waypointAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        waypointList = new WaypointList(0, 54, getWidth(), getHeight() - 140);

        addRenderableWidget(waypointList);
        addRenderableWidget(buttonSortName = new Button.Builder(Component.translatable("minimap.waypoints.sortByName"), button -> sortClicked(2)).bounds(getWidth() / 2 - 154, 34, 77, 20).build());
        addRenderableWidget(buttonSortDistance = new Button.Builder(Component.translatable("minimap.waypoints.sortByDistance"), button -> sortClicked(3)).bounds(getWidth() / 2 - 77, 34, 77, 20).build());
        addRenderableWidget(buttonSortCreated = new Button.Builder(Component.translatable("minimap.waypoints.sortByCreated"), button -> sortClicked(1)).bounds(getWidth() / 2, 34, 77, 20).build());
        addRenderableWidget(buttonSortColor = new Button.Builder(Component.translatable("minimap.waypoints.sortByColor"), button -> sortClicked(4)).bounds(getWidth() / 2 + 77, 34, 77, 20).build());

        filter = new EditBox(getFont(), getWidth() / 2 - 150, getHeight() - 78, 300, 20, Component.empty());
        filter.setHint(Component.translatable("gui.selectWorld.search").withStyle(ChatFormatting.GRAY));
        filter.setMaxLength(35);
        filter.setResponder(this::filterUpdated);
        setFocused(filter);
        addRenderableWidget(filter);

        addRenderableWidget(new Button.Builder(Component.translatable("minimap.waypoints.add"), button -> addWaypoint()).bounds(getWidth() / 2 - 154, getHeight() - 50, 74, 20).build());
        addRenderableWidget(buttonEdit = new Button.Builder(Component.translatable("selectServer.edit"), button -> editWaypoint(selectedWaypoint)).bounds(getWidth() / 2 - 76, getHeight() - 50, 74, 20).build());
        addRenderableWidget(buttonDelete = new Button.Builder(Component.translatable("selectServer.delete"), button -> deleteClicked()).bounds(getWidth() / 2 + 2, getHeight() - 50, 74, 20).build());
        addRenderableWidget(buttonHighlight = new Button.Builder(Component.translatable("minimap.waypoints.highlight"), button -> setHighlightedWaypoint()).bounds(getWidth() / 2 + 80, getHeight() - 50, 74, 20).build());
        addRenderableWidget(buttonTeleport = new Button.Builder(Component.translatable("minimap.waypoints.teleportTo"), button -> teleportClicked()).bounds(getWidth() / 2 - 154, getHeight() - 26, 74, 20).build());
        addRenderableWidget(buttonShare = new Button.Builder(Component.translatable("minimap.waypoints.share"), button -> CommandUtils.sendWaypoint(selectedWaypoint)).bounds(getWidth() / 2 - 76, getHeight() - 26, 74, 20).build());
        addRenderableWidget(new Button.Builder(Component.translatable("menu.options"), button -> VoxelConstants.getMinecraft().setScreen(new GuiWaypointsOptions(this))).bounds(getWidth() / 2 + 2, getHeight() - 26, 74, 20).build());
        addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> onClose()).bounds(getWidth() / 2 + 80, getHeight() - 26, 74, 20).build());

        boolean isSomethingSelected = selectedWaypoint != null;
        buttonEdit.active = isSomethingSelected;
        buttonDelete.active = isSomethingSelected;
        buttonHighlight.active = isSomethingSelected;
        buttonShare.active = isSomethingSelected;
        buttonTeleport.active = isSomethingSelected && canTeleport();

        sort();
    }

    private void sort() {
        int sortKey = Math.abs(options.waypointSort.get());
        boolean ascending = options.waypointSort.get() > 0;
        waypointList.sortBy(sortKey, ascending);
        String arrow = ascending ? "↑" : "↓";

        if (sortKey == 1) {
            buttonSortCreated.setMessage(Component.literal(arrow + " " + I18n.get("minimap.waypoints.sortByCreated") + " " + arrow));
        } else {
            buttonSortCreated.setMessage(Component.translatable("minimap.waypoints.sortByCreated"));
        }

        if (sortKey == 2) {
            buttonSortName.setMessage(Component.literal(arrow + " " + I18n.get("minimap.waypoints.sortByName") + " " + arrow));
        } else {
            buttonSortName.setMessage(Component.translatable("minimap.waypoints.sortByName"));
        }

        if (sortKey == 3) {
            buttonSortDistance.setMessage(Component.literal(arrow + " " + I18n.get("minimap.waypoints.sortByDistance") + " " + arrow));
        } else {
            buttonSortDistance.setMessage(Component.translatable("minimap.waypoints.sortByDistance"));
        }

        if (sortKey == 4) {
            buttonSortColor.setMessage(Component.literal(arrow + " " + I18n.get("minimap.waypoints.sortByColor") + " " + arrow));
        } else {
            buttonSortColor.setMessage(Component.translatable("minimap.waypoints.sortByColor"));
        }
    }

    private void deleteClicked() {
        String waypointName = selectedWaypoint.name;
        if (waypointName != null) {
            deleteClicked = true;

            Component title = Component.translatable("minimap.waypoints.deleteConfirm");
            Component explanation = Component.translatable("selectServer.deleteWarning", waypointName);
            Component affirm = Component.translatable("selectServer.deleteButton");
            Component deny = Component.translatable("gui.cancel");

            VoxelConstants.getMinecraft().setScreen(new ConfirmScreen(this, title, explanation, affirm, deny));
        }
    }

    private void teleportClicked() {
        boolean hasCeiling = VoxelConstants.getPlayer().level().dimensionType().hasCeiling();
        int minY = VoxelConstants.getPlayer().level().getMinY();
        int maxY = VoxelConstants.getPlayer().level().getMaxY();
        int targetY = selectedWaypoint.getY() > minY ? selectedWaypoint.getY() : (!hasCeiling ? maxY : 64);

        VoxelConstants.playerRunTeleportCommand(selectedWaypoint.getX(), targetY, selectedWaypoint.getZ());
        VoxelConstants.getMinecraft().setScreen(null);
    }

    private void sortClicked(int id) {
        options.setSort(id);
        changedSort = true;
        sort();
    }

    private void filterUpdated(String string) {
        waypointList.updateFilter(string.toLowerCase());
    }

    @Override
    public boolean isEditing() {
        return editClicked;
    }

    @Override
    public void accept(boolean b) {
        if (deleteClicked) {
            deleteClicked = false;
            if (b) {
                waypointManager.deleteWaypoint(selectedWaypoint);
                selectedWaypoint = null;
            }

            VoxelConstants.getMinecraft().setScreen(this);
        }

        if (editClicked) {
            editClicked = false;
            if (b) {
                waypointManager.saveWaypoints();
            }

            VoxelConstants.getMinecraft().setScreen(this);
        }

        if (addClicked) {
            addClicked = false;
            if (b) {
                waypointManager.addWaypoint(newWaypoint);
                setSelectedWaypoint(newWaypoint);
            }

            VoxelConstants.getMinecraft().setScreen(this);
        }

    }

    private void setSelectedWaypoint(Waypoint waypoint) {
        selectedWaypoint = waypoint;
        boolean isSomethingSelected = selectedWaypoint != null;

        buttonEdit.active = isSomethingSelected;
        buttonDelete.active = isSomethingSelected;
        buttonHighlight.active = isSomethingSelected;
        buttonHighlight.setMessage(Component.translatable(isSomethingSelected && waypointManager.isHighlightedWaypoint(selectedWaypoint) ? "minimap.waypoints.removeHighlight" : "minimap.waypoints.highlight"));
        buttonShare.active = isSomethingSelected;
        buttonTeleport.active = isSomethingSelected && canTeleport();
    }

    private void setHighlightedWaypoint() {
        waypointManager.setHighlightedWaypoint(selectedWaypoint, true);

        boolean isSomethingSelected = selectedWaypoint != null;
        buttonHighlight.setMessage(Component.translatable(isSomethingSelected && waypointManager.isHighlightedWaypoint(selectedWaypoint) ? "minimap.waypoints.removeHighlight" : "minimap.waypoints.highlight"));
    }

    private void editWaypoint(Waypoint waypoint) {
        editClicked = true;
        VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(this, waypoint, true));
    }

    private void addWaypoint() {
        addClicked = true;
        float r;
        float g;
        float b;
        if (waypointManager.getWaypoints().isEmpty()) {
            r = 0.0F;
            g = 1.0F;
            b = 0.0F;
        } else {
            r = generator.nextFloat();
            g = generator.nextFloat();
            b = generator.nextFloat();
        }

        TreeSet<DimensionContainer> dimensions = new TreeSet<>();
        dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));

        double dimensionScale = VoxelConstants.getPlayer().level().dimensionType().coordinateScale();
        int scaledX = (int) (GameVariableAccessShim.xCoord() * dimensionScale);
        int scaledZ = (int) (GameVariableAccessShim.zCoord() * dimensionScale);
        int scaledY = (int) (GameVariableAccessShim.yCoord() * 1.0F);
        newWaypoint = new Waypoint("", scaledX, scaledZ, scaledY, true, r, g, b, "", VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false), dimensions);

        VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(this, newWaypoint, false));
    }

    private void toggleWaypointVisibility() {
        selectedWaypoint.enabled = !selectedWaypoint.enabled;
        waypointManager.saveWaypoints();
    }

    public boolean canTeleport() {
        Optional<IntegratedServer> integratedServer = VoxelConstants.getIntegratedServer();

        if (integratedServer.isEmpty()) {
            return true;
        }

        try {
            return integratedServer.get().getPlayerList().isOp(VoxelConstants.getPlayer().nameAndId());
        } catch (RuntimeException exception) {
            return integratedServer.get().getWorldData().isAllowCommands();
        }
    }

    @Override
    public void removed() {
        if (changedSort) {
            super.removed();
        }
    }

    class WaypointList extends GuiListMinimap<WaypointList.Entry> {
        private static final Tooltip TOOLTIP_CLICK_TO_ENABLE = Tooltip.create(Component.translatable("minimap.waypoints.enableTooltip"));
        private static final Tooltip TOOLTIP_CLICK_TO_DISABLE = Tooltip.create(Component.translatable("minimap.waypoints.disableTooltip"));
        private static final Tooltip TOOLTIP_CLICK_TO_HIGHLIGHT = Tooltip.create(Component.translatable("minimap.waypoints.highlightTooltip"));
        private static final Tooltip TOOLTIP_CLICK_TO_UNHIGHLIGHT = Tooltip.create(Component.translatable("minimap.waypoints.removeHighlightTooltip"));

        private final ArrayList<Entry> waypoints;
        private String filterString = "";

        public WaypointList(int x, int y, int width, int height) {
            super(x, y, width, height, 18);

            waypoints = new ArrayList<>();
            for (Waypoint pt : waypointManager.getWaypoints()) {
                if (pt.inWorld && pt.inDimension) {
                    waypoints.add(new Entry(pt));
                }
            }

            waypoints.forEach(this::addEntry);
        }

        @Override
        public void setSelected(Entry entry) {
            super.setSelected(entry);
            if (getSelected() != null) {
                GameNarrator narratorManager = new GameNarrator(minecraft);
                narratorManager.sayChatQueued(Component.translatable("narrator.select", getSelected().waypoint.name));
            }

            setSelectedWaypoint(entry.waypoint);
        }

        private void sortBy(int sortKey, boolean ascending) {
            final int order = ascending ? 1 : -1;
            if (sortKey == 1) {
                final ArrayList<?> masterWaypointsList = waypointManager.getWaypoints();
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

        private void updateFilter(String filterString) {
            setScrollAmount(0.0);
            clearEntries();

            this.filterString = filterString;

            for (Entry entry : waypoints) {
                if (TextUtils.scrubCodes(entry.waypoint.name).toLowerCase().contains(filterString)) {
                    if (entry.waypoint == selectedWaypoint) {
                        setSelectedWaypoint(null);
                    }

                    addEntry(entry);
                }
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }

        public class Entry extends GuiListMinimap.Entry<Entry> implements Comparable<Entry> {
            private final Waypoint waypoint;
            private final GuiIconButton waypointIcon;
            private final GuiIconButton waypointToggle;

            public Entry(Waypoint waypoint) {
                super(WaypointList.this);

                this.waypoint = waypoint;

                setTooltip(Tooltip.create(Component.literal("X: " + waypoint.getX() + ", Y: " + waypoint.getY() + ", Z: " + waypoint.getZ())));
                addWidget(waypointIcon = new GuiIconButton(getX() + 2, getY(), 18, 18, button -> setHighlightedWaypoint()));
                addWidget(waypointToggle = new GuiIconButton(getX() + getWidth() - 20, getY(), 18, 18, button -> toggleWaypointVisibility()));
            }

            @Override
            public int compareTo(Entry o) {
                return waypoint.compareTo(o.waypoint);
            }

            @Override
            protected boolean canDisplayTooltip(int mouseX, int mouseY) {
                return mouseX > getRowLeft() + 18 && mouseX < getRowRight() - 18 && super.canDisplayTooltip(mouseX, mouseY);
            }

            @Override
            public void onClick(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
                if (doubleClicked()) {
                    editWaypoint(selectedWaypoint);
                }
            }

            @Override
            public void renderContent(GuiGraphics drawContext, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                super.renderContent(drawContext, mouseX, mouseY, hovered, tickDelta);

                int color = waypoint.getUnifiedColor();
                drawContext.drawCenteredString(getFont(), waypoint.name, GuiWaypoints.this.getWidth() / 2, getY() + 5, color);

                boolean isHighlighted = waypointManager.isHighlightedWaypoint(waypoint);

                Sprite icon = waypointAtlas.getAtlasSprite("selectable/" + waypoint.imageSuffix);
                if (icon == waypointAtlas.getMissingImage()) {
                    icon = waypointAtlas.getAtlasSprite(WaypointManager.FALLBACK_ICON_NAME);
                }
                waypointIcon.setPosition(getX() + 2, getY());
                waypointIcon.setIcon(icon, color);
                waypointIcon.setTooltip(isHighlighted ? TOOLTIP_CLICK_TO_UNHIGHLIGHT : TOOLTIP_CLICK_TO_HIGHLIGHT);

                if (isHighlighted) {
                    waypointAtlas.getAtlasSprite("marker/target").blit(drawContext, RenderPipelines.GUI_TEXTURED, waypointIcon.getX(), waypointIcon.getY(), waypointIcon.getWidth(), waypointIcon.getHeight(), 0xFFFF0000);
                }

                waypointToggle.setPosition(getX() + getWidth() - 20, getY());
                waypointToggle.setIcon(waypoint.enabled ? VoxelConstants.getCheckMarkerTexture() : VoxelConstants.getCrossMarkerTexture(), 0xFFFFFFFF);
                waypointToggle.setTooltip(waypoint.enabled ? TOOLTIP_CLICK_TO_DISABLE : TOOLTIP_CLICK_TO_ENABLE);
            }
        }
    }
}
