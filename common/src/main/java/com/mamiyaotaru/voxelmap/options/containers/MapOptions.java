package com.mamiyaotaru.voxelmap.options.containers;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionsScreenMinimap;
import com.mamiyaotaru.voxelmap.options.MapPermissionsManager;
import com.mamiyaotaru.voxelmap.options.enums.OptionEnumMinimap;
import com.mamiyaotaru.voxelmap.options.fields.BooleanField;
import com.mamiyaotaru.voxelmap.options.fields.EnumField;
import com.mamiyaotaru.voxelmap.options.fields.IntegerField;
import com.mamiyaotaru.voxelmap.options.fields.OptionField;
import com.mamiyaotaru.voxelmap.options.fields.StringField;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.io.PrintWriter;

public class MapOptions extends AbstractOptionsContainer {
    private final boolean isMulticore = Runtime.getRuntime().availableProcessors() > 1;

    // Hidden Options
    public final BooleanField showUnderMenus;
    public final BooleanField welcome;
    public final IntegerField zoom;
    public final BooleanField oldNorth;
    public final EnumField<OptionEnumMinimap.ColorPickerMode> colorPickerMode;

    // General Options
    public final BooleanField hide;
    public final BooleanField updateNotifier;
    public final BooleanField showBiomeInfo;
    public final EnumField<OptionEnumMinimap.CoordInfo> showCoordInfo;
    public final EnumField<OptionEnumMinimap.Location> mapCorner;
    public final EnumField<OptionEnumMinimap.Size> sizeModifier;
    public final BooleanField squareMap;
    public final BooleanField rotates;
    public final EnumField<OptionEnumMinimap.InGameWaypoints> inGameWaypoints;
    private final EnumField<OptionEnumMinimap.InGameWaypoints> inGameWaypointsToggle;
    public final BooleanField showCaves;
    public final BooleanField moveMapBelowStatusEffectIcons;
    public final BooleanField moveScoreboardBelowMap;

    // Performance Options
    public final BooleanField dynamicLighting;
    public final EnumField<OptionEnumMinimap.TerrainDepth> terrainDepth;
    public final BooleanField waterTransparency;
    public final BooleanField blockTransparency;
    public final BooleanField biomeShading;
    public final EnumField<OptionEnumMinimap.BiomeOverlay> biomeOverlay;
    public final BooleanField chunkGrid;
    public final BooleanField slimeChunks;
    public final BooleanField worldBorder;
    public final BooleanField filtering;
    public final StringField teleportCommand;

    // Key Options
    public KeyMapping keyBindZoom;
    public KeyMapping keyBindFullscreen;
    public KeyMapping keyBindMenu;
    public KeyMapping keyBindWaypointMenu;
    public KeyMapping keyBindWaypoint;
    public KeyMapping keyBindMobToggle;
    public KeyMapping keyBindWaypointToggle;
    public KeyMapping keyBindMinimapToggle;
    public final KeyMapping[] keyBindings;

    public MapOptions() {
        addOptionField((showUnderMenus = new BooleanField("Show Under Menus", "", true)));
        addOptionField((welcome = new BooleanField("Welcome Screen", "", true)));
        addOptionField((zoom = new IntegerField("Zoom", "", 2, 0, 4)));
        addOptionField((oldNorth = new BooleanField("Old North", "options.minimap.oldNorth", false)).withListener(this::setOldNorth));
        addOptionField((colorPickerMode = new EnumField<>("Color Picker Mode", "options.minimap.colorPickerMode", OptionEnumMinimap.ColorPickerMode.SIMPLE)));

        addOptionField((hide = new BooleanField("Hide Minimap", "options.minimap.hideMinimap", false)));
        addOptionField((updateNotifier = new BooleanField("Update Notifier", "options.minimap.updateNotifier", true)));
        addOptionField((showBiomeInfo = new BooleanField("Show Biome Info", "options.minimap.showBiome", true)));
        addOptionField((showCoordInfo = new EnumField<>("Show Coord Info", "options.minimap.showCoordinates", OptionEnumMinimap.CoordInfo.DEFAULT)));
        addOptionField((mapCorner = new EnumField<>("Map Corner", "options.minimap.location", OptionEnumMinimap.Location.TOP_LEFT)));
        addOptionField((sizeModifier = new EnumField<>("Map Size", "options.minimap.size", OptionEnumMinimap.Size.LARGE)));
        addOptionField((squareMap = new BooleanField("Square Map", "options.minimap.squareMap", false)));
        addOptionField((rotates = new BooleanField("Rotation", "options.minimap.rotation", true)));
        addOptionField((inGameWaypoints = new EnumField<>("In-game Waypoints", "options.minimap.inGameWaypoints", OptionEnumMinimap.InGameWaypoints.SIGNS)).withListener(this::updateInGameWaypoints));
        addOptionField((inGameWaypointsToggle = new EnumField<>("In-game Waypoints Toggle", "", OptionEnumMinimap.InGameWaypoints.SIGNS)));
        addOptionField((showCaves = new BooleanField("Enable Cave Mode", "options.minimap.caveMode", true)).withListener(this::updateMinimap));
        addOptionField((moveMapBelowStatusEffectIcons = new BooleanField("Move Map Below Status Effect Icons", "options.minimap.moveMapBelowStatusEffectIcons", true)));
        addOptionField((moveScoreboardBelowMap = new BooleanField("Move Scoreboard Below Map", "options.minimap.moveScoreboardBelowMap", true)));

        addOptionField((dynamicLighting = new BooleanField("Dynamic Lighting", "options.minimap.dynamicLighting", true)).withListener(this::updateMinimap));
        addOptionField((terrainDepth = new EnumField<>("Terrain Depth", "options.minimap.terrainDepth", isMulticore ? OptionEnumMinimap.TerrainDepth.BOTH : OptionEnumMinimap.TerrainDepth.SLOPE_MAP)).withListener(this::updateMinimap));
        addOptionField((waterTransparency = new BooleanField("Water Transparency", "options.minimap.waterTransparency", isMulticore)).withListener(this::updateMinimap).withFormat(this::formatMulticoreOption));
        addOptionField((blockTransparency = new BooleanField("Block Transparency", "options.minimap.blockTransparency", isMulticore)).withListener(this::updateMinimap).withFormat(this::formatMulticoreOption));
        addOptionField((biomeShading = new BooleanField("Biome Shading", "options.minimap.biomes", true)).withListener(this::updateMinimap).withFormat(this::formatMulticoreOption));
        addOptionField((biomeOverlay = new EnumField<>("Biome Overlay", "options.minimap.biomeOverlay", OptionEnumMinimap.BiomeOverlay.OFF)).withListener(this::updateMinimap));
        addOptionField((chunkGrid = new BooleanField("Chunk Grid", "options.minimap.chunkGrid", false)).withListener(this::updateMinimap));
        addOptionField((slimeChunks = new BooleanField("Slime Chunks", "options.minimap.slimeChunks", false)).withListener(this::updateMinimap));
        addOptionField((worldBorder = new BooleanField("World Border", "options.minimap.worldBorder", true)).withListener(this::updateMinimap));
        addOptionField((filtering = new BooleanField("Map Filtering", "options.minimap.filtering", false)).withListener(this::updateMinimap));
        addOptionField((teleportCommand = new StringField("Teleport Command", "options.minimap.teleportCommand", "tp %p %x %y %z", StringField.PATTERN_NOT_EMPTY)));

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "controls.title_test"));
        keyBindZoom = new KeyMapping("key.minimap.zoom", InputConstants.KEY_Z, category);
        keyBindFullscreen = new KeyMapping("key.minimap.toggleFullscreen", InputConstants.KEY_X, category);
        keyBindMenu = new KeyMapping("key.minimap.voxelmapMenu", InputConstants.KEY_M, category);
        keyBindWaypointMenu = new KeyMapping("key.minimap.waypointMenu", InputConstants.KEY_U, category);
        keyBindWaypoint = new KeyMapping("key.minimap.waypointHotkey", InputConstants.KEY_N, category);
        keyBindMobToggle = new KeyMapping("key.minimap.toggleMobs", -1, category);
        keyBindWaypointToggle = new KeyMapping("key.minimap.toggleInGameWaypoints", -1, category);
        keyBindMinimapToggle = new KeyMapping("key.minimap.toggleMinimap", InputConstants.KEY_H, category);

        keyBindings = new KeyMapping[] { keyBindZoom, keyBindFullscreen, keyBindMenu, keyBindWaypointMenu, keyBindWaypoint, keyBindMobToggle, keyBindWaypointToggle, keyBindMinimapToggle };
    }

    @Override
    public void updateOptionsActive() {
        boolean minimapEnabled = !hide.get();

        OptionField<?>[] minimapOptions = new OptionField[]{showBiomeInfo, showCoordInfo, mapCorner, sizeModifier, squareMap, rotates, moveMapBelowStatusEffectIcons, moveScoreboardBelowMap};
        for (OptionField<?> option : minimapOptions) {
            option.setActive(minimapEnabled);
        }

        slimeChunks.setActive(Minecraft.getInstance().hasSingleplayerServer() || !VoxelConstants.getVoxelMapInstance().getWorldSeed().isEmpty());
    }

    @Override
    public void updateOptionsAllowed(MapPermissionsManager permissionsManager) {
        hide.setAllowed(permissionsManager.getBoolean(MapPermissionsManager.MINIMAP_ALLOWED));
        showCaves.setAllowed(permissionsManager.getBoolean(MapPermissionsManager.CAVES_ALLOWED));
        inGameWaypoints.setAllowed(permissionsManager.getBoolean(MapPermissionsManager.WAYPOINTS_ALLOWED));
        teleportCommand.setAllowed(permissionsManager.getString(MapPermissionsManager.SERVER_TELEPORT_COMMAND) != null);
    }

    @Override
    public void loadLine(String[] keyValue) {
        super.loadLine(keyValue);

        switch (keyValue[0]) {
            case "Zoom Key" -> bindKey(keyBindZoom, keyValue[1]);
            case "Fullscreen Key" -> bindKey(keyBindFullscreen, keyValue[1]);
            case "Menu Key" -> bindKey(keyBindMenu, keyValue[1]);
            case "Waypoint Menu Key" -> bindKey(keyBindWaypointMenu, keyValue[1]);
            case "Waypoint Key" -> bindKey(keyBindWaypoint, keyValue[1]);
            case "Mob Key" -> bindKey(keyBindMobToggle, keyValue[1]);
            case "In-game Waypoint Key" -> bindKey(keyBindWaypointToggle, keyValue[1]);
            case "Toggle Minimap Key" -> bindKey(keyBindMinimapToggle, keyValue[1]);
        }
        KeyMapping.resetMapping();
    }

    @Override
    public void saveAll(PrintWriter out) {
        super.saveAll(out);

        out.println("Zoom Key:" + keyBindZoom.saveString());
        out.println("Fullscreen Key:" + keyBindFullscreen.saveString());
        out.println("Menu Key:" + keyBindMenu.saveString());
        out.println("Waypoint Menu Key:" + keyBindWaypointMenu.saveString());
        out.println("Waypoint Key:" + keyBindWaypoint.saveString());
        out.println("Mob Key:" + keyBindMobToggle.saveString());
        out.println("In-game Waypoint Key:" + keyBindWaypointToggle.saveString());
        out.println("Toggle Minimap Key:" + keyBindMinimapToggle.saveString());
    }

    private void bindKey(KeyMapping keyBinding, String id) {
        try {
            keyBinding.setKey(InputConstants.getKey(id));
        } catch (RuntimeException var4) {
            VoxelConstants.getLogger().warn("{} is not a valid keybinding", id);
        }
    }

    public void toggleInGameWaypoints() {
        if (inGameWaypoints.get() != OptionEnumMinimap.InGameWaypoints.OFF) {
            inGameWaypoints.set(OptionEnumMinimap.InGameWaypoints.OFF);
        } else {
            inGameWaypoints.set(inGameWaypointsToggle.get());
        }
    }

    // Option Attributes

    private String formatMulticoreOption(Boolean flag) {
        String string = flag ? I18n.get("options.on") : I18n.get("options.off");
        if (!isMulticore && flag) {
            string = "§c" + string;
        }
        return string;
    }

    private void updateMinimap(Object value) {
        if (VoxelConstants.getVoxelMapInstance().getMap() != null) {
            VoxelConstants.getVoxelMapInstance().getMap().optionsChanged();
        }
    }

    private void setOldNorth(Boolean value) {
        if (VoxelConstants.getVoxelMapInstance().getWaypointManager() != null) {
            VoxelConstants.getVoxelMapInstance().getWaypointManager().setOldNorth(value);
        }
    }

    private void updateInGameWaypoints(OptionEnumMinimap.InGameWaypoints value) {
        if (Minecraft.getInstance().screen instanceof GuiOptionsScreenMinimap) {
            inGameWaypointsToggle.set(value);
        }
    }
}
