package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISettingsManager;
import com.mamiyaotaru.voxelmap.interfaces.ISubSettingsManager;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import com.mojang.blaze3d.platform.InputConstants;
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
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MapSettingsManager implements ISettingsManager {
    private File settingsFile;
    public boolean showUnderMenus;
    private final int availableProcessors = Runtime.getRuntime().availableProcessors();
    public final boolean multicore = this.availableProcessors > 1;
    public boolean hide;
    public boolean coords = true;
    protected boolean showCaves = true;
    public boolean lightmap = true;
    public boolean heightmap = this.multicore;
    public boolean slopemap = true;
    public boolean filtering;
    public boolean waterTransparency = this.multicore;
    public boolean blockTransparency = this.multicore;
    public boolean biomes = this.multicore;
    public int biomeOverlay;
    public boolean chunkGrid;
    public boolean slimeChunks;
    public boolean worldborder = true;
    public boolean squareMap = true;
    public boolean rotates = true;
    public boolean oldNorth;
    public boolean showBeacons;
    public boolean showWaypoints = true;
    private boolean preToggleBeacons;
    private boolean preToggleSigns = true;
    public int deathpoints = 1;
    public int maxWaypointDisplayDistance = 1000;
    protected boolean welcome = true;
    public int zoom = 2;
    public int sizeModifier = 1;
    public int mapCorner = 1;

    public Boolean cavesAllowed = true;
    public boolean worldmapAllowed = true;
    public boolean minimapAllowed = true;
    public boolean waypointsAllowed = true;
    public boolean deathWaypointAllowed = true;

    public boolean moveMapDownWhileStatusEffect = true;
    public boolean moveScoreBoardDown = true;
    public boolean distanceUnitConversion = true;
    public boolean waypointNameBelowIcon = true;
    public boolean waypointDistanceBelowName = true;
    public int sort = 1;
    protected boolean realTimeTorches;
    public KeyMapping keyBindZoom;
    public KeyMapping keyBindFullscreen;
    public KeyMapping keyBindMenu;
    public KeyMapping keyBindWaypointMenu;
    public KeyMapping keyBindWaypoint;
    public KeyMapping keyBindMobToggle;
    public KeyMapping keyBindWaypointToggle;
    public KeyMapping keyBindMinimapToggle;
    public final KeyMapping[] keyBindings;
    private boolean somethingChanged;
    public static MapSettingsManager instance;
    private final List<ISubSettingsManager> subSettingsManagers = new ArrayList<>();

    public String teleportCommand = "tp %p %x %y %z";
    public String serverTeleportCommand;

    public MapSettingsManager() {
        instance = this;
        KeyMapping.Category category = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("voxelmap", "controls.title"));

        keyBindZoom = new KeyMapping("key.minimap.zoom", InputConstants.getKey("key.keyboard.z").getValue(), category);
        keyBindFullscreen = new KeyMapping("key.minimap.togglefullscreen", InputConstants.getKey("key.keyboard.x").getValue(), category);
        keyBindMenu = new KeyMapping("key.minimap.voxelmapmenu", InputConstants.getKey("key.keyboard.m").getValue(), category);
        keyBindWaypointMenu = new KeyMapping("key.minimap.waypointmenu", InputConstants.getKey("key.keyboard.u").getValue(), category);
        keyBindWaypoint = new KeyMapping("key.minimap.waypointhotkey", InputConstants.getKey("key.keyboard.n").getValue(), category);
        keyBindMobToggle = new KeyMapping("key.minimap.togglemobs", -1, category);
        keyBindWaypointToggle = new KeyMapping("key.minimap.toggleingamewaypoints", -1, category);
        keyBindMinimapToggle = new KeyMapping("key.minimap.toggleminimap", InputConstants.getKey("key.keyboard.o").getValue(), category);

        this.keyBindings = new KeyMapping[]{this.keyBindMenu, this.keyBindWaypointMenu, this.keyBindZoom, this.keyBindFullscreen, this.keyBindWaypoint, this.keyBindMobToggle, this.keyBindWaypointToggle, this.keyBindMinimapToggle};
    }

    public void addSecondaryOptionsManager(ISubSettingsManager secondarySettingsManager) {
        this.subSettingsManagers.add(secondarySettingsManager);
    }

    public void loadAll() {
        this.settingsFile = new File(VoxelConstants.getMinecraft().gameDirectory, "config/voxelmap.properties");

        try {
            if (this.settingsFile.exists()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(this.settingsFile), StandardCharsets.UTF_8.newDecoder()));
                String sCurrentLine;
                while ((sCurrentLine = in.readLine()) != null) {
                    String[] curLine = sCurrentLine.split(":");
                    switch (curLine[0]) {
                        case "Zoom Level" -> this.zoom = Math.max(0, Math.min(4, Integer.parseInt(curLine[1])));
                        case "Hide Minimap" -> this.hide = Boolean.parseBoolean(curLine[1]);
                        case "Show Coordinates" -> this.coords = Boolean.parseBoolean(curLine[1]);
                        case "Enable Cave Mode" -> this.showCaves = Boolean.parseBoolean(curLine[1]);
                        case "Dynamic Lighting" -> this.lightmap = Boolean.parseBoolean(curLine[1]);
                        case "Height Map" -> this.heightmap = Boolean.parseBoolean(curLine[1]);
                        case "Slope Map" -> this.slopemap = Boolean.parseBoolean(curLine[1]);
                        case "Blur" -> this.filtering = Boolean.parseBoolean(curLine[1]);
                        case "Water Transparency" -> this.waterTransparency = Boolean.parseBoolean(curLine[1]);
                        case "Block Transparency" -> this.blockTransparency = Boolean.parseBoolean(curLine[1]);
                        case "Biomes" -> this.biomes = Boolean.parseBoolean(curLine[1]);
                        case "Biome Overlay" -> this.biomeOverlay = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                        case "Chunk Grid" -> this.chunkGrid = Boolean.parseBoolean(curLine[1]);
                        case "Slime Chunks" -> this.slimeChunks = Boolean.parseBoolean(curLine[1]);
                        case "World Border" -> this.worldborder = Boolean.parseBoolean(curLine[1]);
                        case "Square Map" -> this.squareMap = Boolean.parseBoolean(curLine[1]);
                        case "Rotation" -> this.rotates = Boolean.parseBoolean(curLine[1]);
                        case "Old North" -> this.oldNorth = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Beacons" -> this.showBeacons = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Signs" -> this.showWaypoints = Boolean.parseBoolean(curLine[1]);
                        case "Deathpoints" -> this.deathpoints = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                        case "Waypoint Max Distance" -> this.maxWaypointDisplayDistance = Math.max(-1, Math.min(10000, Integer.parseInt(curLine[1])));
                        case "Waypoint Sort By" -> this.sort = Math.max(1, Math.min(4, Integer.parseInt(curLine[1])));
                        case "Welcome Message" -> this.welcome = Boolean.parseBoolean(curLine[1]);
                        case "Real Time Torch Flicker" -> this.realTimeTorches = Boolean.parseBoolean(curLine[1]);
                        case "Map Corner" -> this.mapCorner = Math.max(0, Math.min(3, Integer.parseInt(curLine[1])));
                        case "Map Size" -> this.sizeModifier = Math.max(-1, Math.min(4, Integer.parseInt(curLine[1])));
                        case "Zoom Key" -> this.bindKey(this.keyBindZoom, curLine[1]);
                        case "Fullscreen Key" -> this.bindKey(this.keyBindFullscreen, curLine[1]);
                        case "Menu Key" -> this.bindKey(this.keyBindMenu, curLine[1]);
                        case "Waypoint Menu Key" -> this.bindKey(this.keyBindWaypointMenu, curLine[1]);
                        case "Waypoint Key" -> this.bindKey(this.keyBindWaypoint, curLine[1]);
                        case "Mob Key" -> this.bindKey(this.keyBindMobToggle, curLine[1]);
                        case "In-game Waypoint Key" -> this.bindKey(this.keyBindWaypointToggle, curLine[1]);
                        case "Toggle Minimap Key" -> this.bindKey(this.keyBindMinimapToggle, curLine[1]);
                        case "Teleport Command" -> this.teleportCommand = curLine[1];
                        case "Move Map Down While Status Effect" -> this.moveMapDownWhileStatusEffect = Boolean.parseBoolean(curLine[1]);
                        case "Move ScoreBoard Down" -> this.moveScoreBoardDown = Boolean.parseBoolean(curLine[1]);
                        case "Distance Unit Conversion" -> this.distanceUnitConversion = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Name Below Icon" -> this.waypointNameBelowIcon = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Distance Below Name" -> this.waypointDistanceBelowName  = Boolean.parseBoolean(curLine[1]);
                    }
                }
                KeyMapping.resetMapping();
                for (ISubSettingsManager subSettingsManager : this.subSettingsManagers) {
                    subSettingsManager.loadSettings(this.settingsFile);
                }

                in.close();
            }

            this.saveAll();
        } catch (IOException exception) {
            VoxelConstants.getLogger().error(exception);
        }

    }

    private void bindKey(KeyMapping keyBinding, String id) {
        try {
            keyBinding.setKey(InputConstants.getKey(id));
        } catch (RuntimeException var4) {
            VoxelConstants.getLogger().warn(id + " is not a valid keybinding");
        }

    }

    public void saveAll() {
        File settingsFileDir = new File(VoxelConstants.getMinecraft().gameDirectory, "/config/");
        if (!settingsFileDir.exists()) {
            settingsFileDir.mkdirs();
        }

        this.settingsFile = new File(settingsFileDir, "voxelmap.properties");

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.settingsFile), StandardCharsets.UTF_8.newEncoder())));
            out.println("Zoom Level:" + this.zoom);
            out.println("Hide Minimap:" + this.hide);
            out.println("Show Coordinates:" + this.coords);
            out.println("Enable Cave Mode:" + this.showCaves);
            out.println("Dynamic Lighting:" + this.lightmap);
            out.println("Height Map:" + this.heightmap);
            out.println("Slope Map:" + this.slopemap);
            out.println("Blur:" + this.filtering);
            out.println("Water Transparency:" + this.waterTransparency);
            out.println("Block Transparency:" + this.blockTransparency);
            out.println("Biomes:" + this.biomes);
            out.println("Biome Overlay:" + this.biomeOverlay);
            out.println("Chunk Grid:" + this.chunkGrid);
            out.println("Slime Chunks:" + this.slimeChunks);
            out.println("World Boarder:" + this.worldborder);
            out.println("Square Map:" + this.squareMap);
            out.println("Rotation:" + this.rotates);
            out.println("Old North:" + this.oldNorth);
            out.println("Waypoint Beacons:" + this.showBeacons);
            out.println("Waypoint Signs:" + this.showWaypoints);
            out.println("Deathpoints:" + this.deathpoints);
            out.println("Waypoint Max Distance:" + this.maxWaypointDisplayDistance);
            out.println("Waypoint Sort By:" + this.sort);
            out.println("Welcome Message:" + this.welcome);
            out.println("Map Corner:" + this.mapCorner);
            out.println("Map Size:" + this.sizeModifier);
            out.println("Zoom Key:" + this.keyBindZoom.saveString());
            out.println("Fullscreen Key:" + this.keyBindFullscreen.saveString());
            out.println("Menu Key:" + this.keyBindMenu.saveString());
            out.println("Waypoint Menu Key:" + this.keyBindWaypointMenu.saveString());
            out.println("Waypoint Key:" + this.keyBindWaypoint.saveString());
            out.println("Mob Key:" + this.keyBindMobToggle.saveString());
            out.println("In-game Waypoint Key:" + this.keyBindWaypointToggle.saveString());
            out.println("Toggle Minimap Key:" + this.keyBindMinimapToggle.saveString());
            out.println("Teleport Command:" + this.teleportCommand);
            out.println("Move Map Down While Status Effect:" + this.moveMapDownWhileStatusEffect);
            out.println("Move ScoreBoard Down:" + this.moveScoreBoardDown);
            out.println("Distance Unit Conversion:" + this.distanceUnitConversion);
            out.println("Waypoint Name Below Icon:" + this.waypointNameBelowIcon);
            out.println("Waypoint Distance Below Name:" + this.waypointDistanceBelowName);

            for (ISubSettingsManager subSettingsManager : this.subSettingsManagers) {
                subSettingsManager.saveAll(out);
            }

            out.close();
        } catch (FileNotFoundException var5) {
            MessageUtils.chatInfo("§EError Saving Settings " + var5.getLocalizedMessage());
        }
    }

    @Override
    public String getKeyText(EnumOptionsMinimap options) {
        String s = I18n.get(options.getName()) + ": ";
        if (options.isFloat()) {
            float f = this.getOptionFloatValue(options);
            if (options == EnumOptionsMinimap.ZOOM) {
                return s + (int) f;
            } else if (options == EnumOptionsMinimap.WAYPOINTDISTANCE) {
                return f < 0.0F ? s + I18n.get("options.minimap.waypoints.infinite") : s + (int) f;
            } else {
                return f == 0.0F ? s + I18n.get("options.off") : s + (int) f + "%";
            }
        } else if (options.isBoolean()) {
            boolean flag = this.getOptionBooleanValue(options);
            return flag ? s + I18n.get("options.on") : s + I18n.get("options.off");
        } else if (options.isList()) {
            String state = this.getOptionListValue(options);
            return s + state;
        } else {
            return s;
        }
    }

    @Override
    public float getOptionFloatValue(EnumOptionsMinimap options) {
        if (options == EnumOptionsMinimap.ZOOM) {
            return this.zoom;
        } else {
            return options == EnumOptionsMinimap.WAYPOINTDISTANCE ? this.maxWaypointDisplayDistance : 0.0F;
        }
    }

    public boolean getOptionBooleanValue(EnumOptionsMinimap par1EnumOptions) {
        return switch (par1EnumOptions) {
            case COORDS -> this.coords;
            case HIDE -> this.hide || !this.minimapAllowed;
            case CAVEMODE -> this.cavesAllowed && this.showCaves;
            case LIGHTING -> this.lightmap;
            case SQUARE -> this.squareMap;
            case ROTATES -> this.rotates;
            case OLDNORTH -> this.oldNorth;
            case WELCOME -> this.welcome;
            case FILTERING -> this.filtering;
            case WATERTRANSPARENCY -> this.waterTransparency;
            case BLOCKTRANSPARENCY -> this.blockTransparency;
            case BIOMES -> this.biomes;
            case CHUNKGRID -> this.chunkGrid;
            case SLIMECHUNKS -> this.slimeChunks;
            case WORLDBORDER -> this.worldborder;
            case MOVEMAPDOWNWHILESTATSUEFFECT -> this.moveMapDownWhileStatusEffect;
            case MOVESCOREBOARDDOWN -> this.moveScoreBoardDown;
            case DISTANCEUNITCONVERSION -> this.distanceUnitConversion;
            case WAYPOINTNAMEBELOWICON -> this.waypointNameBelowIcon;
            case WAYPOINTDISTANCEBELOWNAME -> this.waypointDistanceBelowName;
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName() + ". (possibly not a boolean applicable to minimap)");
        };
    }

    public String getOptionListValue(EnumOptionsMinimap par1EnumOptions) {
        switch (par1EnumOptions) {
            case TERRAIN -> {
                if (this.slopemap && this.heightmap) {
                    return I18n.get("options.minimap.terrain.both");
                } else if (this.heightmap) {
                    return I18n.get("options.minimap.terrain.height");
                } else if (this.slopemap) {
                    return I18n.get("options.minimap.terrain.slope");
                }
                return I18n.get("options.off");
            }
            case BEACONS -> {
                if (this.waypointsAllowed && this.showBeacons && this.showWaypoints) {
                    return I18n.get("options.minimap.ingamewaypoints.both");
                } else if (this.waypointsAllowed && this.showBeacons) {
                    return I18n.get("options.minimap.ingamewaypoints.beacons");
                } else if (this.waypointsAllowed && this.showWaypoints) {
                    return I18n.get("options.minimap.ingamewaypoints.signs");
                }
                return I18n.get("options.off");
            }
            case LOCATION -> {
                if (this.mapCorner == 0) {
                    return I18n.get("options.minimap.location.topleft");
                } else if (this.mapCorner == 1) {
                    return I18n.get("options.minimap.location.topright");
                } else if (this.mapCorner == 2) {
                    return I18n.get("options.minimap.location.bottomright");
                } else {
                    if (this.mapCorner == 3) {
                        return I18n.get("options.minimap.location.bottomleft");
                    }

                    return "Error";
                }
            }
            case SIZE -> {
                if (this.sizeModifier == -1) {
                    return I18n.get("options.minimap.size.small");
                } else if (this.sizeModifier == 0) {
                    return I18n.get("options.minimap.size.medium");
                } else if (this.sizeModifier == 1) {
                    return I18n.get("options.minimap.size.large");
                } else if (this.sizeModifier == 2) {
                    return I18n.get("options.minimap.size.xl");
                } else if (this.sizeModifier == 3) {
                    return I18n.get("options.minimap.size.xxl");
                } else {
                    if (this.sizeModifier == 4) {
                        return I18n.get("options.minimap.size.xxxl");
                    }

                    return "error";
                }
            }
            case BIOMEOVERLAY -> {
                if (this.biomeOverlay == 0) {
                    return I18n.get("options.off");
                } else if (this.biomeOverlay == 1) {
                    return I18n.get("options.minimap.biomeoverlay.solid");
                } else {
                    if (this.biomeOverlay == 2) {
                        return I18n.get("options.minimap.biomeoverlay.transparent");
                    }

                    return "error";
                }
            }
            case DEATHPOINTS -> {
                if (this.deathpoints == 0) {
                    return I18n.get("options.off");
                } else if (this.deathpoints == 1) {
                    return I18n.get("options.minimap.waypoints.deathpoints.mostrecent");
                } else {
                    if (this.deathpoints == 2) {
                        return I18n.get("options.minimap.waypoints.deathpoints.all");
                    }

                    return "error";
                }
            }
            default ->
                    throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName() + ". (possibly not a list value applicable to minimap)");
        }
    }

    @Override
    public void setOptionFloatValue(EnumOptionsMinimap options, float value) {
        if (options == EnumOptionsMinimap.WAYPOINTDISTANCE) {
            float distance = value * 9951.0F + 50.0F;
            if (distance > 10000.0F) {
                distance = -1.0F;
            }

            this.maxWaypointDisplayDistance = (int) distance;
        }

        this.somethingChanged = true;
    }

    public void setOptionValue(EnumOptionsMinimap par1EnumOptions) {
        switch (par1EnumOptions) {
            case COORDS -> this.coords = !this.coords;
            case HIDE -> this.hide = !this.hide;
            case CAVEMODE -> this.showCaves = !this.showCaves;
            case LIGHTING -> this.lightmap = !this.lightmap;
            case SQUARE -> this.squareMap = !this.squareMap;
            case ROTATES -> this.rotates = !this.rotates;
            case OLDNORTH -> this.oldNorth = !this.oldNorth;
            case WELCOME -> this.welcome = !this.welcome;
            case FILTERING -> this.filtering = !this.filtering;
            case WATERTRANSPARENCY -> this.waterTransparency = !this.waterTransparency;
            case BLOCKTRANSPARENCY -> this.blockTransparency = !this.blockTransparency;
            case BIOMES -> this.biomes = !this.biomes;
            case CHUNKGRID -> this.chunkGrid = !this.chunkGrid;
            case SLIMECHUNKS -> this.slimeChunks = !this.slimeChunks;
            case WORLDBORDER -> this.worldborder = !this.worldborder;
            case MOVEMAPDOWNWHILESTATSUEFFECT -> this.moveMapDownWhileStatusEffect = !this.moveMapDownWhileStatusEffect;
            case MOVESCOREBOARDDOWN -> this.moveScoreBoardDown = !this.moveScoreBoardDown;
            case DISTANCEUNITCONVERSION -> this.distanceUnitConversion = !this.distanceUnitConversion;
            case WAYPOINTNAMEBELOWICON -> this.waypointNameBelowIcon = !this.waypointNameBelowIcon;
            case WAYPOINTDISTANCEBELOWNAME -> this.waypointDistanceBelowName = !this.waypointDistanceBelowName;
            case TERRAIN -> {
                if (this.slopemap && this.heightmap) {
                    this.slopemap = false;
                    this.heightmap = false;
                } else if (this.slopemap) {
                    this.slopemap = false;
                    this.heightmap = true;
                } else if (this.heightmap) {
                    this.slopemap = true;
                } else {
                    this.slopemap = true;
                }
            }
            case BEACONS -> {
                if (this.showBeacons && this.showWaypoints) {
                    this.showBeacons = false;
                    this.showWaypoints = false;
                } else if (this.showBeacons) {
                    this.showBeacons = false;
                    this.showWaypoints = true;
                } else if (this.showWaypoints) {
                    this.showBeacons = true;
                } else {
                    this.showBeacons = true;
                }
            }
            case LOCATION -> this.mapCorner = this.mapCorner >= 3 ? 0 : this.mapCorner + 1;
            case SIZE -> this.sizeModifier = this.sizeModifier >= 4 ? -1 : this.sizeModifier + 1;
            case BIOMEOVERLAY -> {
                ++this.biomeOverlay;
                if (this.biomeOverlay > 2) {
                    this.biomeOverlay = 0;
                }
            }
            case DEATHPOINTS -> {
                ++this.deathpoints;
                if (this.deathpoints > 2) {
                    this.deathpoints = 0;
                }
            }
            default ->
                    throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName());
        }

        this.somethingChanged = true;
    }

    public void toggleIngameWaypoints() {
        if (!this.showBeacons && !this.showWaypoints) {
            this.showBeacons = this.preToggleBeacons;
            this.showWaypoints = this.preToggleSigns;
        } else {
            this.preToggleBeacons = this.showBeacons;
            this.preToggleSigns = this.showWaypoints;
            this.showBeacons = false;
            this.showWaypoints = false;
        }
    }

    public String getKeyBindingDescription(int keybindIndex) {
        return this.keyBindings[keybindIndex].getName().equals("key.minimap.voxelmapmenu") ? I18n.get("key.minimap.menu") : I18n.get(this.keyBindings[keybindIndex].getName());
    }

    public Component getKeybindDisplayString(int keybindIndex) {
        KeyMapping keyBinding = this.keyBindings[keybindIndex];
        return this.getKeybindDisplayString(keyBinding);
    }

    public Component getKeybindDisplayString(KeyMapping keyBinding) {
        return keyBinding.getTranslatedKeyMessage();
    }

    public void setKeyBinding(KeyMapping keyBinding, InputConstants.Key input) {
        keyBinding.setKey(input);
        this.saveAll();
    }

    public void setSort(int sort) {
        if (sort != this.sort && sort != -this.sort) {
            this.sort = sort;
        } else {
            this.sort = -this.sort;
        }

    }

    public boolean isChanged() {
        if (this.somethingChanged) {
            this.somethingChanged = false;
            return true;
        } else {
            return false;
        }
    }
}
