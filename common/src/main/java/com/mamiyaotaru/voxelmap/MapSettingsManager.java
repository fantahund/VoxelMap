package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISettingsManager;
import com.mamiyaotaru.voxelmap.interfaces.ISubSettingsManager;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MapSettingsManager implements ISettingsManager {
    public static final String ERROR_STRING = "§c???";
    public static MapSettingsManager instance;

    private File settingsFile;
    private final List<ISubSettingsManager> subSettingsManagers = new ArrayList<>();
    private boolean somethingChanged;

    private final int availableProcessors = Runtime.getRuntime().availableProcessors();
    public final boolean multicore = availableProcessors > 1;

    protected boolean welcome = true;
    public int zoom = 2;
    public boolean oldNorth;
    public int colorPickerMode = 0;
    protected boolean realTimeTorches;

    public boolean hide = false;
    public boolean updateNotifier = true;
    public boolean showBiome = false;
    public int coordsMode = 1;
    public int mapCorner = 1;
    public int sizeModifier = 1;
    public boolean squareMap = false;
    public boolean rotates = true;
    public boolean showWaypointBeacons = false;
    public boolean showWaypointSigns = true;
    private boolean lastWaypointBeacons = false;
    private boolean lastWaypointSigns = true;
    protected boolean showCaves = true;
    public boolean moveMapBelowStatusEffectIcons = true;
    public boolean moveScoreboardBelowMap = true;

    public boolean dynamicLighting = true;
    public boolean heightmap = multicore;
    public boolean slopemap = true;
    public boolean waterTransparency = multicore;
    public boolean blockTransparency = multicore;
    public boolean biomes = multicore;
    public int biomeOverlay = 0;
    public boolean chunkGrid = false;
    public boolean slimeChunks = false;
    public boolean worldBorder = true;
    public boolean filtering = false;
    public String teleportCommand = "tp %p %x %y %z";
    public String serverTeleportCommand;

    public int waypointSort = 1;
    public int maxWaypointDisplayDistance = -1;
    public float waypointSignScale = 1.0F;
    public int deathpoints = 1;
    public int waypointDistanceConversion = 1;
    public int waypointNamesLocation = 2;
    public int waypointDistancesLocation = 2;

    public boolean showUnderMenus;
    public Boolean cavesAllowed = true;
    public boolean worldmapAllowed = true;
    public boolean minimapAllowed = true;
    public boolean waypointsAllowed = true;
    public boolean deathWaypointAllowed = true;

    public KeyMapping keyBindZoom;
    public KeyMapping keyBindFullscreen;
    public KeyMapping keyBindMenu;
    public KeyMapping keyBindWaypointMenu;
    public KeyMapping keyBindWaypoint;
    public KeyMapping keyBindMobToggle;
    public KeyMapping keyBindWaypointToggle;
    public KeyMapping keyBindMinimapToggle;
    public final KeyMapping[] keyBindings;

    public MapSettingsManager() {
        instance = this;
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "controls.title"));

        keyBindings = new KeyMapping[] {
                keyBindZoom = new KeyMapping("key.minimap.zoom", InputConstants.getKey("key.keyboard.z").getValue(), category),
                keyBindFullscreen = new KeyMapping("key.minimap.toggleFullscreen", InputConstants.getKey("key.keyboard.x").getValue(), category),
                keyBindMenu = new KeyMapping("key.minimap.voxelmapMenu", InputConstants.getKey("key.keyboard.m").getValue(), category),
                keyBindWaypointMenu = new KeyMapping("key.minimap.waypointMenu", InputConstants.getKey("key.keyboard.u").getValue(), category),
                keyBindWaypoint = new KeyMapping("key.minimap.waypointHotkey", InputConstants.getKey("key.keyboard.n").getValue(), category),
                keyBindMobToggle = new KeyMapping("key.minimap.toggleMobs", -1, category),
                keyBindWaypointToggle = new KeyMapping("key.minimap.toggleInGameWaypoints", -1, category),
                keyBindMinimapToggle = new KeyMapping("key.minimap.toggleMinimap", InputConstants.getKey("key.keyboard.h").getValue(), category)
        };
    }

    public void addSubSettingsManager(ISubSettingsManager subSettingsManager) {
        subSettingsManagers.add(subSettingsManager);
    }

    @Override
    public void loadAll() {
        settingsFile = new File(VoxelConstants.getMinecraft().gameDirectory, "config/voxelmap.properties");

        try {
            if (settingsFile.exists()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(settingsFile), StandardCharsets.UTF_8.newDecoder()));
                String sCurrentLine;
                while ((sCurrentLine = in.readLine()) != null) {
                    String[] curLine = sCurrentLine.split(":");
                    switch (curLine[0]) {
                        case "Welcome Message" -> welcome = Boolean.parseBoolean(curLine[1]);
                        case "Zoom Level" -> zoom = Mth.clamp(Integer.parseInt(curLine[1]), 0, 4);
                        case "Old North" -> oldNorth = Boolean.parseBoolean(curLine[1]);
                        case "Color Picker Mode" -> colorPickerMode = Mth.clamp(Integer.parseInt(curLine[1]), 0, 1);
                        case "Real Time Torch Flicker" -> realTimeTorches = Boolean.parseBoolean(curLine[1]);

                        case "Hide Minimap" -> hide = Boolean.parseBoolean(curLine[1]);
                        case "Update Notifier" -> updateNotifier = Boolean.parseBoolean(curLine[1]);
                        case "Display Biome" -> showBiome = Boolean.parseBoolean(curLine[1]);
                        case "Display Coordinates" -> coordsMode = Mth.clamp(Integer.parseInt(curLine[1]), 0, 2);
                        case "Map Corner" -> mapCorner = Mth.clamp(Integer.parseInt(curLine[1]), 0, 3);
                        case "Map Size" -> sizeModifier = Mth.clamp(Integer.parseInt(curLine[1]), -1, 4);
                        case "Square Map" -> squareMap = Boolean.parseBoolean(curLine[1]);
                        case "Rotation" -> rotates = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Beacons" -> showWaypointBeacons = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Signs" -> showWaypointSigns = Boolean.parseBoolean(curLine[1]);
                        case "Enable Cave Mode" -> showCaves = Boolean.parseBoolean(curLine[1]);
                        case "Move Map Below Status Effect Icons" -> moveMapBelowStatusEffectIcons = Boolean.parseBoolean(curLine[1]);
                        case "Move Scoreboard Below Map" -> moveScoreboardBelowMap = Boolean.parseBoolean(curLine[1]);

                        case "Dynamic Lighting" -> dynamicLighting = Boolean.parseBoolean(curLine[1]);
                        case "Height Map" -> heightmap = Boolean.parseBoolean(curLine[1]);
                        case "Slope Map" -> slopemap = Boolean.parseBoolean(curLine[1]);
                        case "Water Transparency" -> waterTransparency = Boolean.parseBoolean(curLine[1]);
                        case "Block Transparency" -> blockTransparency = Boolean.parseBoolean(curLine[1]);
                        case "Biomes" -> biomes = Boolean.parseBoolean(curLine[1]);
                        case "Biome Overlay" -> biomeOverlay = Mth.clamp(Integer.parseInt(curLine[1]), 0, 2);
                        case "Chunk Grid" -> chunkGrid = Boolean.parseBoolean(curLine[1]);
                        case "Slime Chunks" -> slimeChunks = Boolean.parseBoolean(curLine[1]);
                        case "World Border" -> worldBorder = Boolean.parseBoolean(curLine[1]);
                        case "Filtering" -> filtering = Boolean.parseBoolean(curLine[1]);
                        case "Teleport Command" -> teleportCommand = curLine[1];

                        case "Waypoint Sort By" -> waypointSort = Mth.clamp(Integer.parseInt(curLine[1]), 1, 4);
                        case "Waypoint Max Distance" -> maxWaypointDisplayDistance = Mth.clamp(Integer.parseInt(curLine[1]), -1, 10000);
                        case "Waypoint Sign Scale" -> waypointSignScale = Mth.clamp(Float.parseFloat(curLine[1]), 0.5F, 1.5F);
                        case "Deathpoints" -> deathpoints = Mth.clamp(Integer.parseInt(curLine[1]), 0, 2);
                        case "Waypoint Distance Unit Conversion" -> waypointDistanceConversion = Mth.clamp(Integer.parseInt(curLine[1]), 0, 2);
                        case "Show In-game Waypoint Names" -> waypointNamesLocation = Mth.clamp(Integer.parseInt(curLine[1]), 0, 2);
                        case "Show In-game Waypoint Distances" -> waypointDistancesLocation = Mth.clamp(Integer.parseInt(curLine[1]), 0, 2);

                        case "Zoom Key" -> bindKey(keyBindZoom, curLine[1]);
                        case "Fullscreen Key" -> bindKey(keyBindFullscreen, curLine[1]);
                        case "Menu Key" -> bindKey(keyBindMenu, curLine[1]);
                        case "Waypoint Menu Key" -> bindKey(keyBindWaypointMenu, curLine[1]);
                        case "Waypoint Key" -> bindKey(keyBindWaypoint, curLine[1]);
                        case "Mob Key" -> bindKey(keyBindMobToggle, curLine[1]);
                        case "In-game Waypoint Key" -> bindKey(keyBindWaypointToggle, curLine[1]);
                        case "Toggle Minimap Key" -> bindKey(keyBindMinimapToggle, curLine[1]);

                    }
                }
                KeyMapping.resetMapping();
                for (ISubSettingsManager subSettingsManager : subSettingsManagers) {
                    subSettingsManager.loadAll(settingsFile);
                }

                in.close();
            }

            saveAll();
        } catch (IOException exception) {
            VoxelConstants.getLogger().error(exception);
        }

    }

    @Override
    public void saveAll() {
        File settingsFileDir = new File(VoxelConstants.getMinecraft().gameDirectory, "/config/");
        if (!settingsFileDir.exists()) {
            settingsFileDir.mkdirs();
        }

        settingsFile = new File(settingsFileDir, "voxelmap.properties");

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(settingsFile), StandardCharsets.UTF_8.newEncoder())));

            out.println("Welcome Message:" + welcome);
            out.println("Zoom Level:" + zoom);
            out.println("Old North:" + oldNorth);
            out.println("Color Picker Mode:" + colorPickerMode);

            out.println("Hide Minimap:" + hide);
            out.println("Update Notifier:" + updateNotifier);
            out.println("Display Biome:" + showBiome);
            out.println("Display Coordinates:" + coordsMode);
            out.println("Map Corner:" + mapCorner);
            out.println("Map Size:" + sizeModifier);
            out.println("Square Map:" + squareMap);
            out.println("Rotation:" + rotates);
            out.println("Waypoint Beacons:" + showWaypointBeacons);
            out.println("Waypoint Signs:" + showWaypointSigns);
            out.println("Enable Cave Mode:" + showCaves);
            out.println("Move Map Below Status Effect Icons:" + moveMapBelowStatusEffectIcons);
            out.println("Move Scoreboard Below Map:" + moveScoreboardBelowMap);

            out.println("Dynamic Lighting:" + dynamicLighting);
            out.println("Height Map:" + heightmap);
            out.println("Slope Map:" + slopemap);
            out.println("Water Transparency:" + waterTransparency);
            out.println("Block Transparency:" + blockTransparency);
            out.println("Biomes:" + biomes);
            out.println("Biome Overlay:" + biomeOverlay);
            out.println("Chunk Grid:" + chunkGrid);
            out.println("Slime Chunks:" + slimeChunks);
            out.println("World Border:" + worldBorder);
            out.println("Filtering:" + filtering);
            out.println("Teleport Command:" + teleportCommand);

            out.println("Waypoint Sort By:" + waypointSort);
            out.println("Waypoint Max Distance:" + maxWaypointDisplayDistance);
            out.println("Waypoint Sign Scale:" + waypointSignScale);
            out.println("Deathpoints:" + deathpoints);
            out.println("Waypoint Distance Unit Conversion:" + waypointDistanceConversion);
            out.println("Show In-game Waypoint Names:" + waypointNamesLocation);
            out.println("Show In-game Waypoint Distances:" + waypointDistancesLocation);

            out.println("Zoom Key:" + keyBindZoom.saveString());
            out.println("Fullscreen Key:" + keyBindFullscreen.saveString());
            out.println("Menu Key:" + keyBindMenu.saveString());
            out.println("Waypoint Menu Key:" + keyBindWaypointMenu.saveString());
            out.println("Waypoint Key:" + keyBindWaypoint.saveString());
            out.println("Mob Key:" + keyBindMobToggle.saveString());
            out.println("In-game Waypoint Key:" + keyBindWaypointToggle.saveString());
            out.println("Toggle Minimap Key:" + keyBindMinimapToggle.saveString());

            for (ISubSettingsManager subSettingsManager : subSettingsManagers) {
                subSettingsManager.saveAll(out);
            }

            out.close();
        } catch (FileNotFoundException exception) {
            VoxelConstants.getLogger().error(exception);
            MessageUtils.chatInfo("§EError Saving Settings " + exception.getLocalizedMessage());
        }
    }

    @Override
    public String getKeyText(EnumOptionsMinimap option) {
        String s = I18n.get(option.getName()) + ": ";
        if (option.isBoolean()) {
            boolean flag = getBooleanValue(option);
            return s + (flag ? I18n.get("options.on") : I18n.get("options.off"));
        } else if (option.isList()) {
            String state = getListValue(option);
            return s + state;
        } else if (option.isFloat()) {
            float value = getFloatValue(option);
            return switch (option) {
                case ZOOM -> s + (int) value;

                case WAYPOINT_DISTANCE -> s + (value < 0.0F ? I18n.get("options.minimap.waypoints.infinite") : (int) value);
                case WAYPOINT_SIGN_SCALE -> s + String.format("%.2fx", value);

                default -> s + (value <= 0.0F ? I18n.get("options.off") : (int) value + "%");
            };
        } else {
            return s + ERROR_STRING;
        }
    }

    @Override
    public boolean getBooleanValue(EnumOptionsMinimap option) {
        return switch (option) {
            case WELCOME_SCREEN -> welcome;
            case OLD_NORTH -> oldNorth;

            case HIDE_MINIMAP -> hide || !minimapAllowed;
            case UPDATE_NOTIFIER -> updateNotifier;
            case SHOW_BIOME -> showBiome;
            case SQUARE_MAP -> squareMap;
            case ROTATES -> rotates;
            case CAVE_MODE -> cavesAllowed && showCaves;
            case MOVE_MAP_BELOW_STATUS_EFFECT_ICONS -> moveMapBelowStatusEffectIcons;
            case MOVE_SCOREBOARD_BELOW_MAP -> moveScoreboardBelowMap;

            case DYNAMIC_LIGHTING -> dynamicLighting;
            case WATER_TRANSPARENCY -> waterTransparency;
            case BLOCK_TRANSPARENCY -> blockTransparency;
            case BIOMES -> biomes;
            case CHUNK_GRID -> chunkGrid;
            case SLIME_CHUNKS -> slimeChunks;
            case WORLD_BORDER -> worldBorder;
            case FILTERING -> filtering;

            default -> throw new IllegalArgumentException("Invalid boolean value! Add code to handle EnumOptionMinimap: " + option.getName());
        };
    }

    @Override
    public void toggleBooleanValue(EnumOptionsMinimap option) {
        switch (option) {
            case WELCOME_SCREEN -> welcome = !welcome;
            case OLD_NORTH -> oldNorth = !oldNorth;

            case HIDE_MINIMAP -> hide = !hide;
            case UPDATE_NOTIFIER -> updateNotifier = !updateNotifier;
            case SHOW_BIOME -> showBiome = !showBiome;
            case SQUARE_MAP -> squareMap = !squareMap;
            case ROTATES -> rotates = !rotates;
            case CAVE_MODE -> showCaves = !showCaves;
            case MOVE_MAP_BELOW_STATUS_EFFECT_ICONS -> moveMapBelowStatusEffectIcons = !moveMapBelowStatusEffectIcons;
            case MOVE_SCOREBOARD_BELOW_MAP -> moveScoreboardBelowMap = !moveScoreboardBelowMap;

            case DYNAMIC_LIGHTING -> dynamicLighting = !dynamicLighting;
            case WATER_TRANSPARENCY -> waterTransparency = !waterTransparency;
            case BLOCK_TRANSPARENCY -> blockTransparency = !blockTransparency;
            case BIOMES -> biomes = !biomes;
            case CHUNK_GRID -> chunkGrid = !chunkGrid;
            case SLIME_CHUNKS -> slimeChunks = !slimeChunks;
            case WORLD_BORDER -> worldBorder = !worldBorder;
            case FILTERING -> filtering = !filtering;

            default -> throw new IllegalArgumentException("Invalid boolean value! Add code to handle EnumOptionMinimap: " + option.getName());
        }

        somethingChanged = true;
    }

    @Override
    public String getListValue(EnumOptionsMinimap option) {
        switch (option) {
            case COLOR_PICKER_MODE -> {
                return parseListValue(0, colorPickerMode,
                        I18n.get("options.minimap.colorPickerMode.simple"),
                        I18n.get("options.minimap.colorPickerMode.full"));
            }

            case SHOW_COORDS -> {
                return parseListValue(0, coordsMode,
                        I18n.get("options.off"),
                        I18n.get("options.minimap.showCoordinates.classic"),
                        I18n.get("options.minimap.showCoordinates.horizontal"));
            }
            case LOCATION -> {
                return parseListValue(0, mapCorner,
                        I18n.get("options.minimap.location.topLeft"),
                        I18n.get("options.minimap.location.topRight"),
                        I18n.get("options.minimap.location.bottomRight"),
                        I18n.get("options.minimap.location.bottomLeft"));
            }
            case SIZE -> {
                return parseListValue(-1, sizeModifier,
                        I18n.get("options.minimap.size.small"),
                        I18n.get("options.minimap.size.medium"),
                        I18n.get("options.minimap.size.large"),
                        I18n.get("options.minimap.size.xl"),
                        I18n.get("options.minimap.size.xxl"),
                        I18n.get("options.minimap.size.xxxl"));
            }
            case IN_GAME_WAYPOINTS -> {
                if (waypointsAllowed && showWaypointBeacons && showWaypointSigns) {
                    return I18n.get("options.minimap.inGameWaypoints.both");
                } else if (waypointsAllowed && showWaypointBeacons) {
                    return I18n.get("options.minimap.inGameWaypoints.beacons");
                } else if (waypointsAllowed && showWaypointSigns) {
                    return I18n.get("options.minimap.inGameWaypoints.signs");
                }
                return I18n.get("options.off");
            }

            case TERRAIN_DEPTH -> {
                if (slopemap && heightmap) {
                    return I18n.get("options.minimap.terrain.both");
                } else if (heightmap) {
                    return I18n.get("options.minimap.terrain.height");
                } else if (slopemap) {
                    return I18n.get("options.minimap.terrain.slope");
                }
                return I18n.get("options.off");
            }
            case BIOME_OVERLAY -> {
                return parseListValue(0, biomeOverlay,
                        I18n.get("options.off"),
                        I18n.get("options.minimap.biomeOverlay.solid"),
                        I18n.get("options.minimap.biomeOverlay.transparent"));
            }

            case DEATHPOINTS -> {
                return parseListValue(0, deathpoints,
                        I18n.get("options.off"),
                        I18n.get("options.minimap.waypoints.deathpoints.mostRecent"),
                        I18n.get("options.minimap.waypoints.deathpoints.all"));
            }
            case WAYPOINT_DISTANCE_UNIT_CONVERSION -> {
                return parseListValue(0, waypointDistanceConversion,
                        I18n.get("options.off"),
                        I18n.get("options.minimap.waypoints.distanceUnitConversion.from1000m"),
                        I18n.get("options.minimap.waypoints.distanceUnitConversion.from10000m"));
            }
            case SHOW_IN_GAME_WAYPOINT_NAMES -> {
                return parseListValue(0, waypointNamesLocation,
                        I18n.get("options.off"),
                        I18n.get("options.minimap.waypoints.showWaypointNames.aboveIcon"),
                        I18n.get("options.minimap.waypoints.showWaypointNames.belowIcon"));
            }
            case SHOW_IN_GAME_WAYPOINT_DISTANCES -> {
                String str = waypointNamesLocation == 0 ? I18n.get("options.minimap.waypoints.showWaypointDistances.aboveIcon") : I18n.get("options.minimap.waypoints.showWaypointDistances.besideName");
                String str2 = waypointNamesLocation == 0 ? I18n.get("options.minimap.waypoints.showWaypointDistances.belowIcon") : I18n.get("options.minimap.waypoints.showWaypointDistances.belowName");
                return parseListValue(0, waypointDistancesLocation, I18n.get("options.off"), str, str2);
            }

            default -> throw new IllegalArgumentException("Invalid list value! Add code to handle EnumOptionMinimap: " + option.getName());
        }
    }

    @Override
    public void cycleListValue(EnumOptionsMinimap option) {
        switch (option) {
            case COLOR_PICKER_MODE -> colorPickerMode = cycleInRange(colorPickerMode, 0, 1);

            case SHOW_COORDS -> coordsMode = cycleInRange(coordsMode, 0, 2);
            case LOCATION -> mapCorner = cycleInRange(mapCorner, 0, 3);
            case SIZE -> sizeModifier = cycleInRange(sizeModifier, -1, 4);
            case IN_GAME_WAYPOINTS -> {
                if (showWaypointBeacons && showWaypointSigns) {
                    showWaypointBeacons = false;
                    showWaypointSigns = false;
                } else if (showWaypointBeacons) {
                    showWaypointBeacons = false;
                    showWaypointSigns = true;
                } else if (showWaypointSigns) {
                    showWaypointBeacons = true;
                } else {
                    showWaypointBeacons = true;
                }
            }

            case TERRAIN_DEPTH -> {
                if (slopemap && heightmap) {
                    slopemap = false;
                    heightmap = false;
                } else if (slopemap) {
                    slopemap = false;
                    heightmap = true;
                } else if (heightmap) {
                    slopemap = true;
                } else {
                    slopemap = true;
                }
            }
            case BIOME_OVERLAY -> biomeOverlay = cycleInRange(biomeOverlay, 0, 2);

            case DEATHPOINTS -> deathpoints = cycleInRange(deathpoints, 0, 2);
            case WAYPOINT_DISTANCE_UNIT_CONVERSION -> waypointDistanceConversion = cycleInRange(waypointDistanceConversion, 0, 2);
            case SHOW_IN_GAME_WAYPOINT_NAMES -> waypointNamesLocation = cycleInRange(waypointNamesLocation, 0, 2);
            case SHOW_IN_GAME_WAYPOINT_DISTANCES -> waypointDistancesLocation = cycleInRange(waypointDistancesLocation, 0, 2);

            default -> throw new IllegalArgumentException("Invalid list value! Add code to handle EnumOptionMinimap: " + option.getName());
        }

        somethingChanged = true;
    }

    @Override
    public float getFloatValue(EnumOptionsMinimap option) {
        return switch (option) {
            case ZOOM -> zoom;

            case WAYPOINT_DISTANCE -> maxWaypointDisplayDistance;
            case WAYPOINT_SIGN_SCALE -> waypointSignScale;

            default -> throw new IllegalArgumentException("Invalid float value! Add code to handle EnumOptionMinimap: " + option.getName());
        };
    }

    @Override
    public void setFloatValue(EnumOptionsMinimap option, float value) {
        switch (option) {
            case WAYPOINT_DISTANCE -> {
                float distance = Mth.lerp(value, 50.0F, 10001.0F);
                if (distance > 10000.0F) {
                    distance = -1.0F;
                }

                maxWaypointDisplayDistance = (int) distance;
            }
            case WAYPOINT_SIGN_SCALE -> {
                int stepCount = 100;
                float value2 = Math.round(value * stepCount) / (float) stepCount;

                waypointSignScale = Mth.lerp(value2, 0.5F, 1.5F);
            }

            default -> throw new IllegalArgumentException("Invalid float value! Add code to handle EnumOptionMinimap: " + option.getName());
        }

        somethingChanged = true;
    }

    public static void updateBooleanOrListValue(ISettingsManager settingsManager, EnumOptionsMinimap option) {
        if (option.isBoolean()) {
            settingsManager.toggleBooleanValue(option);
        } else if (option.isList()) {
            settingsManager.cycleListValue(option);
        }
    }

    public static int cycleInRange(int current, int min, int max) {
        return current >= max ? min : current + 1;
    }

    public static String parseListValue(int base, int value, String... names) {
        int i = value - base;
        if (i >= 0 && i < names.length) {
            return names[i];
        }
        return ERROR_STRING;
    }

    public void toggleIngameWaypoints() {
        if (!showWaypointBeacons && !showWaypointSigns) {
            showWaypointBeacons = lastWaypointBeacons;
            showWaypointSigns = lastWaypointSigns;
        } else {
            lastWaypointBeacons = showWaypointBeacons;
            lastWaypointSigns = showWaypointSigns;
            showWaypointBeacons = false;
            showWaypointSigns = false;
        }
    }

    private void bindKey(KeyMapping keyBinding, String id) {
        try {
            keyBinding.setKey(InputConstants.getKey(id));
        } catch (RuntimeException var4) {
            VoxelConstants.getLogger().warn(id + " is not a valid keybinding");
        }

    }

    public void setKeyBinding(KeyMapping keyBinding, InputConstants.Key input) {
        keyBinding.setKey(input);
        saveAll();
    }

    public String getKeyBindingDescription(int keybindIndex) {
        String rawName = keyBindings[keybindIndex].getName();
        if (rawName.equals("key.minimap.voxelmapMenu")) {
            return I18n.get("key.minimap.menu");
        }

        return I18n.get(rawName);
    }

    public Component getKeybindDisplayString(int keybindIndex) {
        KeyMapping keyBinding = keyBindings[keybindIndex];
        return getKeybindDisplayString(keyBinding);
    }

    public Component getKeybindDisplayString(KeyMapping keyBinding) {
        return keyBinding.getTranslatedKeyMessage();
    }

    public void setWaypointSort(int sort) {
        if (sort != waypointSort && sort != -waypointSort) {
            waypointSort = sort;
        } else {
            waypointSort = -waypointSort;
        }

    }

    public boolean isChanged() {
        if (somethingChanged) {
            somethingChanged = false;
            return true;
        } else {
            return false;
        }
    }
}
