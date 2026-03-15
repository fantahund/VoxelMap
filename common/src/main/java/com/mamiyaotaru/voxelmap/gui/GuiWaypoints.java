package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;

public class GuiWaypoints extends GuiScreenMinimap implements IGuiWaypoints {
    protected final MapSettingsManager options;
    protected final WaypointManager waypointManager;
    protected Component screenTitle;
    private GuiListWaypoints waypointList;
    private Button buttonEdit;
    private boolean editClicked;
    private Button buttonDelete;
    private boolean deleteClicked;
    private Button buttonHighlight;
    private Button buttonShare;
    private Button buttonTeleport;
    private Button buttonSortName;
    private Button buttonSortCreated;
    private Button buttonSortDistance;
    private Button buttonSortColor;
    protected EditBox filter;
    private boolean addClicked;
    private Component tooltip;
    protected Waypoint selectedWaypoint;
    protected Waypoint highlightedWaypoint;
    protected Waypoint newWaypoint;
    private final Random generator = new Random();
    private boolean changedSort;

    public GuiWaypoints(Screen parentScreen) {
        lastScreen = parentScreen;

        options = VoxelConstants.getVoxelMapInstance().getMapOptions();
        waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        highlightedWaypoint = waypointManager.getHighlightedWaypoint();
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        screenTitle = Component.translatable("minimap.waypoints.title");
        waypointList = new GuiListWaypoints(this);

        addRenderableWidget(waypointList);
        addRenderableWidget(buttonSortName = new Button.Builder(Component.translatable("minimap.waypoints.sortByName"), button -> sortClicked(2)).bounds(getWidth() / 2 - 154, 34, 77, 20).build());
        addRenderableWidget(buttonSortDistance = new Button.Builder(Component.translatable("minimap.waypoints.sortByDistance"), button -> sortClicked(3)).bounds(getWidth() / 2 - 77, 34, 77, 20).build());
        addRenderableWidget(buttonSortCreated = new Button.Builder(Component.translatable("minimap.waypoints.sortByCreated"), button -> sortClicked(1)).bounds(getWidth() / 2, 34, 77, 20).build());
        addRenderableWidget(buttonSortColor = new Button.Builder(Component.translatable("minimap.waypoints.sortByColor"), button -> sortClicked(4)).bounds(getWidth() / 2 + 77, 34, 77, 20).build());

        int filterStringWidth = getFont().width(I18n.get("minimap.waypoints.filter") + ":");
        filter = new EditBox(getFont(), getWidth() / 2 - 153 + filterStringWidth + 5, getHeight() - 78, 305 - filterStringWidth - 5, 20, Component.empty());
        filter.setMaxLength(35);
        filter.setResponder(this::filterUpdated);

        addRenderableWidget(filter);
        setFocused(filter);
        addRenderableWidget(new Button.Builder(Component.translatable("minimap.waypoints.add"), button -> addWaypoint()).bounds(getWidth() / 2 - 154, getHeight() - 50, 74, 20).build());
        addRenderableWidget(buttonEdit = new Button.Builder(Component.translatable("selectServer.edit"), button -> editWaypoint(selectedWaypoint)).bounds(getWidth() / 2 - 76, getHeight() - 50, 74, 20).build());
        addRenderableWidget(buttonDelete = new Button.Builder(Component.translatable("selectServer.delete"), button -> deleteClicked()).bounds(getWidth() / 2 + 2, getHeight() - 50, 74, 20).build());
        addRenderableWidget(buttonHighlight = new Button.Builder(Component.translatable("minimap.waypoints.highlight"), button -> setHighlightedWaypoint()).bounds(getWidth() / 2 + 80, getHeight() - 50, 74, 20).build());
        addRenderableWidget(buttonTeleport = new Button.Builder(Component.translatable("minimap.waypoints.teleportTo"), button -> teleportClicked()).bounds(getWidth() / 2 - 154, getHeight() - 26, 74, 20).build());
        addRenderableWidget(buttonShare = new Button.Builder(Component.translatable("minimap.waypoints.share"), button -> CommandUtils.sendWaypoint(selectedWaypoint)).bounds(getWidth() / 2 - 76, getHeight() - 26, 74, 20).build());
        addRenderableWidget(new Button.Builder(Component.translatable("menu.options"), button -> VoxelConstants.getMinecraft().setScreen(new GuiWaypointsOptions(this, options))).bounds(getWidth() / 2 + 2, getHeight() - 26, 74, 20).build());
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
        int sortKey = Math.abs(options.waypointSort);
        boolean ascending = options.waypointSort > 0;
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

    protected void sortClicked(int id) {
        options.setWaypointSort(id);
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

    protected void setSelectedWaypoint(Waypoint waypoint) {
        selectedWaypoint = waypoint;
        boolean isSomethingSelected = selectedWaypoint != null;

        buttonEdit.active = isSomethingSelected;
        buttonDelete.active = isSomethingSelected;
        buttonHighlight.active = isSomethingSelected;
        buttonHighlight.setMessage(Component.translatable(isSomethingSelected && selectedWaypoint == highlightedWaypoint ? "minimap.waypoints.removeHighlight" : "minimap.waypoints.highlight"));
        buttonShare.active = isSomethingSelected;
        buttonTeleport.active = isSomethingSelected && canTeleport();
    }

    protected void setHighlightedWaypoint() {
        waypointManager.setHighlightedWaypoint(selectedWaypoint, true);
        highlightedWaypoint = waypointManager.getHighlightedWaypoint();

        boolean isSomethingSelected = selectedWaypoint != null;
        buttonHighlight.setMessage(Component.translatable(isSomethingSelected && selectedWaypoint == highlightedWaypoint ? "minimap.waypoints.removeHighlight" : "minimap.waypoints.highlight"));
    }

    protected void editWaypoint(Waypoint waypoint) {
        editClicked = true;
        VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(this, waypoint, true));
    }

    protected void addWaypoint() {
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

    protected void toggleWaypointVisibility() {
        selectedWaypoint.enabled = !selectedWaypoint.enabled;
        waypointManager.saveWaypoints();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        tooltip = null;

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(getFont(), screenTitle, getWidth() / 2, 20, 0xFFFFFFFF);
        graphics.text(getFont(), I18n.get("minimap.waypoints.filter") + ":", getWidth() / 2 - 153, getHeight() - 73, 0xFFA0A0A0);

        if (tooltip != null) {
            renderTooltip(graphics, tooltip, mouseX, mouseY);
        }
    }

    protected void setTooltip(Component tooltip) {
        this.tooltip = tooltip;
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
}
