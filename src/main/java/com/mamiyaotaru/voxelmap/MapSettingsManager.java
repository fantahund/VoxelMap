package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISettingsManager;
import com.mamiyaotaru.voxelmap.interfaces.ISubSettingsManager;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MapSettingsManager implements ISettingsManager {
    private File settingsFile;
    public boolean showUnderMenus;
    private final int availableProcessors = Runtime.getRuntime().availableProcessors();
    public final boolean multicore = this.availableProcessors > 1;
    public boolean hide = false;
    public boolean coords = true;
    protected boolean showCaves = true;
    public boolean lightmap = true;
    public boolean heightmap = this.multicore;
    public boolean slopemap = true;
    public boolean filtering = false;
    public boolean waterTransparency = this.multicore;
    public boolean blockTransparency = this.multicore;
    public boolean biomes = this.multicore;
    public int biomeOverlay = 0;
    public boolean chunkGrid = false;
    public boolean slimeChunks = false;
    public boolean squareMap = true;
    public boolean rotates = true;
    public boolean oldNorth = false;
    public boolean showBeacons = false;
    public boolean showWaypoints = true;
    private boolean preToggleBeacons = false;
    private boolean preToggleSigns = true;
    public int deathpoints = 1;
    public int maxWaypointDisplayDistance = 1000;
    protected boolean welcome = true;
    public int zoom = 2;
    public int sizeModifier = 1;
    public int mapCorner = 1;
    public Boolean cavesAllowed = true;
    public int sort = 1;
    protected boolean realTimeTorches = false;
    public final KeyBinding keyBindZoom = new KeyBinding("key.minimap.zoom", InputUtil.fromTranslationKey("key.keyboard.z").getCode(), "controls.minimap.title");
    public final KeyBinding keyBindFullscreen = new KeyBinding("key.minimap.togglefullscreen", InputUtil.fromTranslationKey("key.keyboard.x").getCode(), "controls.minimap.title");
    public final KeyBinding keyBindMenu = new KeyBinding("key.minimap.voxelmapmenu", InputUtil.fromTranslationKey("key.keyboard.m").getCode(), "controls.minimap.title");
    public final KeyBinding keyBindWaypointMenu = new KeyBinding("key.minimap.waypointmenu", -1, "controls.minimap.title");
    public final KeyBinding keyBindWaypoint = new KeyBinding("key.minimap.waypointhotkey", InputUtil.fromTranslationKey("key.keyboard.n").getCode(), "controls.minimap.title");
    public final KeyBinding keyBindMobToggle = new KeyBinding("key.minimap.togglemobs", -1, "controls.minimap.title");
    public final KeyBinding keyBindWaypointToggle = new KeyBinding("key.minimap.toggleingamewaypoints", -1, "controls.minimap.title");
    public final KeyBinding[] keyBindings;
    private boolean somethingChanged;
    public static MapSettingsManager instance;
    private final List<ISubSettingsManager> subSettingsManagers = new ArrayList<>();

    public MapSettingsManager() {
        instance = this;
        this.keyBindings = new KeyBinding[]{this.keyBindMenu, this.keyBindWaypointMenu, this.keyBindZoom, this.keyBindFullscreen, this.keyBindWaypoint, this.keyBindMobToggle, this.keyBindWaypointToggle};
    }

    public void addSecondaryOptionsManager(ISubSettingsManager secondarySettingsManager) {
        this.subSettingsManagers.add(secondarySettingsManager);
    }

    public void loadAll() {
        this.settingsFile = new File(VoxelConstants.getMinecraft().runDirectory, "config/voxelmap.properties");

        try {
            if (this.settingsFile.exists()) {
                BufferedReader in;
                String sCurrentLine;
                for (in = new BufferedReader(new InputStreamReader(new FileInputStream(this.settingsFile), StandardCharsets.UTF_8.newDecoder())); (sCurrentLine = in.readLine()) != null; KeyBinding.updateKeysByCode()) {
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
                    }
                }

                for (ISubSettingsManager subSettingsManager : this.subSettingsManagers) {
                    subSettingsManager.loadSettings(this.settingsFile);
                }

                in.close();
            }

            this.saveAll();
        } catch (Exception ignored) {
        }

    }

    private void bindKey(KeyBinding keyBinding, String id) {
        try {
            keyBinding.setBoundKey(InputUtil.fromTranslationKey(id));
        } catch (Exception var4) {
            VoxelMap.getLogger().warn(id + " is not a valid keybinding");
        }

    }

    public void saveAll() {
        File settingsFileDir = new File(VoxelConstants.getMinecraft().runDirectory, "/config/");
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
            out.println("Zoom Key:" + this.keyBindZoom.getBoundKeyTranslationKey());
            out.println("Fullscreen Key:" + this.keyBindFullscreen.getBoundKeyTranslationKey());
            out.println("Menu Key:" + this.keyBindMenu.getBoundKeyTranslationKey());
            out.println("Waypoint Menu Key:" + this.keyBindWaypointMenu.getBoundKeyTranslationKey());
            out.println("Waypoint Key:" + this.keyBindWaypoint.getBoundKeyTranslationKey());
            out.println("Mob Key:" + this.keyBindMobToggle.getBoundKeyTranslationKey());
            out.println("In-game Waypoint Key:" + this.keyBindWaypointToggle.getBoundKeyTranslationKey());

            for (ISubSettingsManager subSettingsManager : this.subSettingsManagers) {
                subSettingsManager.saveAll(out);
            }

            out.close();
        } catch (Exception var5) {
            MessageUtils.chatInfo("§EError Saving Settings " + var5.getLocalizedMessage());
        }

    }

    @Override
    public String getKeyText(EnumOptionsMinimap par1EnumOptions) {
        String s = I18nUtils.getString(par1EnumOptions.getName()) + ": ";
        if (par1EnumOptions.isFloat()) {
            float f = this.getOptionFloatValue(par1EnumOptions);
            if (par1EnumOptions == EnumOptionsMinimap.ZOOM) {
                return s + (int) f;
            } else if (par1EnumOptions == EnumOptionsMinimap.WAYPOINTDISTANCE) {
                return f < 0.0F ? s + I18nUtils.getString("options.minimap.waypoints.infinite") : s + (int) f;
            } else {
                return f == 0.0F ? s + I18nUtils.getString("options.off") : s + (int) f + "%";
            }
        } else if (par1EnumOptions.isBoolean()) {
            boolean flag = this.getOptionBooleanValue(par1EnumOptions);
            return flag ? s + I18nUtils.getString("options.on") : s + I18nUtils.getString("options.off");
        } else if (par1EnumOptions.isList()) {
            String state = this.getOptionListValue(par1EnumOptions);
            return s + state;
        } else {
            return s;
        }
    }

    @Override
    public float getOptionFloatValue(EnumOptionsMinimap par1EnumOptions) {
        if (par1EnumOptions == EnumOptionsMinimap.ZOOM) {
            return (float) this.zoom;
        } else {
            return par1EnumOptions == EnumOptionsMinimap.WAYPOINTDISTANCE ? (float) this.maxWaypointDisplayDistance : 0.0F;
        }
    }

    public boolean getOptionBooleanValue(EnumOptionsMinimap par1EnumOptions) {
        return switch (par1EnumOptions) {
            case COORDS -> this.coords;
            case HIDE -> this.hide;
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
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName() + ". (possibly not a boolean applicable to minimap)");
        };
    }

    public String getOptionListValue(EnumOptionsMinimap par1EnumOptions) {
        switch (par1EnumOptions) {
            case TERRAIN -> {
                if (this.slopemap && this.heightmap) {
                    return I18nUtils.getString("options.minimap.terrain.both");
                } else if (this.heightmap) {
                    return I18nUtils.getString("options.minimap.terrain.height");
                } else {
                    if (this.slopemap) {
                        return I18nUtils.getString("options.minimap.terrain.slope");
                    }

                    return I18nUtils.getString("options.off");
                }
            }
            case BEACONS -> {
                if (this.showBeacons && this.showWaypoints) {
                    return I18nUtils.getString("options.minimap.ingamewaypoints.both");
                } else if (this.showBeacons) {
                    return I18nUtils.getString("options.minimap.ingamewaypoints.beacons");
                } else {
                    if (this.showWaypoints) {
                        return I18nUtils.getString("options.minimap.ingamewaypoints.signs");
                    }

                    return I18nUtils.getString("options.off");
                }
            }
            case LOCATION -> {
                if (this.mapCorner == 0) {
                    return I18nUtils.getString("options.minimap.location.topleft");
                } else if (this.mapCorner == 1) {
                    return I18nUtils.getString("options.minimap.location.topright");
                } else if (this.mapCorner == 2) {
                    return I18nUtils.getString("options.minimap.location.bottomright");
                } else {
                    if (this.mapCorner == 3) {
                        return I18nUtils.getString("options.minimap.location.bottomleft");
                    }

                    return "Error";
                }
            }
            case SIZE -> {
                if (this.sizeModifier == -1) {
                    return I18nUtils.getString("options.minimap.size.small");
                } else if (this.sizeModifier == 0) {
                    return I18nUtils.getString("options.minimap.size.medium");
                } else if (this.sizeModifier == 1) {
                    return I18nUtils.getString("options.minimap.size.large");
                } else if (this.sizeModifier == 2) {
                    return I18nUtils.getString("options.minimap.size.xl");
                } else if (this.sizeModifier == 3) {
                    return I18nUtils.getString("options.minimap.size.xxl");
                } else {
                    if (this.sizeModifier == 4) {
                        return I18nUtils.getString("options.minimap.size.xxxl");
                    }

                    return "error";
                }
            }
            case BIOMEOVERLAY -> {
                if (this.biomeOverlay == 0) {
                    return I18nUtils.getString("options.off");
                } else if (this.biomeOverlay == 1) {
                    return I18nUtils.getString("options.minimap.biomeoverlay.solid");
                } else {
                    if (this.biomeOverlay == 2) {
                        return I18nUtils.getString("options.minimap.biomeoverlay.transparent");
                    }

                    return "error";
                }
            }
            case DEATHPOINTS -> {
                if (this.deathpoints == 0) {
                    return I18nUtils.getString("options.off");
                } else if (this.deathpoints == 1) {
                    return I18nUtils.getString("options.minimap.waypoints.deathpoints.mostrecent");
                } else {
                    if (this.deathpoints == 2) {
                        return I18nUtils.getString("options.minimap.waypoints.deathpoints.all");
                    }

                    return "error";
                }
            }
            default ->
                    throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName() + ". (possibly not a list value applicable to minimap)");
        }
    }

    @Override
    public void setOptionFloatValue(EnumOptionsMinimap par1EnumOptions, float par2) {
        if (par1EnumOptions == EnumOptionsMinimap.WAYPOINTDISTANCE) {
            float distance = par2 * 9951.0F + 50.0F;
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
        return this.keyBindings[keybindIndex].getTranslationKey().equals("key.minimap.voxelmapmenu") ? I18nUtils.getString("key.minimap.menu") : I18nUtils.getString(this.keyBindings[keybindIndex].getTranslationKey());
    }

    public Text getKeybindDisplayString(int keybindIndex) {
        KeyBinding keyBinding = this.keyBindings[keybindIndex];
        return this.getKeybindDisplayString(keyBinding);
    }

    public Text getKeybindDisplayString(KeyBinding keyBinding) {
        return keyBinding.getBoundKeyLocalizedText();
    }

    public void setKeyBinding(KeyBinding keyBinding, InputUtil.Key input) {
        keyBinding.setBoundKey(input);
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
