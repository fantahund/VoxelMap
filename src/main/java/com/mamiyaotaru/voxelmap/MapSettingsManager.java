package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISettingsManager;
import com.mamiyaotaru.voxelmap.interfaces.ISubSettingsManager;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import net.minecraft.client.MinecraftClient;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MapSettingsManager implements ISettingsManager {
    public final int SORT_DATE = 1;
    public final int SORT_NAME = 2;
    public final int SORT_DISTANCE = 3;
    public final int SORT_COLOR = 4;
    public final int TOP_LEFT = 0;
    public final int TOP_RIGHT = 1;
    public final int BOTTOM_RIGHT = 2;
    public final int BOTTOM_LEFT = 3;
    public final int SMALL = -1;
    public final int MEDIUM = 0;
    public final int LARGE = 1;
    public final int XL = 2;
    public final int XXL = 3;
    public final int XXXL = 4;
    public final int OFF = 0;
    public final int SOLID = 1;
    public final int TRANSPARENT = 2;
    public final int MOST_RECENT = 1;
    public final int ALL = 2;
    private File settingsFile;
    public boolean showUnderMenus;
    private int availableProcessors = Runtime.getRuntime().availableProcessors();
    public boolean multicore = this.availableProcessors > 1;
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
    protected int regularZoom = 2;
    public int sizeModifier = 1;
    public int mapCorner = 1;
    public Boolean cavesAllowed = true;
    public int sort = 1;
    protected boolean realTimeTorches = false;
    public KeyBinding keyBindZoom = new KeyBinding("key.minimap.zoom", InputUtil.fromTranslationKey("key.keyboard.z").getCode(), "controls.minimap.title");
    public KeyBinding keyBindFullscreen = new KeyBinding("key.minimap.togglefullscreen", InputUtil.fromTranslationKey("key.keyboard.x").getCode(), "controls.minimap.title");
    public KeyBinding keyBindMenu = new KeyBinding("key.minimap.voxelmapmenu", InputUtil.fromTranslationKey("key.keyboard.m").getCode(), "controls.minimap.title");
    public KeyBinding keyBindWaypointMenu = new KeyBinding("key.minimap.waypointmenu", -1, "controls.minimap.title");
    public KeyBinding keyBindWaypoint = new KeyBinding("key.minimap.waypointhotkey", InputUtil.fromTranslationKey("key.keyboard.n").getCode(), "controls.minimap.title");
    public KeyBinding keyBindMobToggle = new KeyBinding("key.minimap.togglemobs", -1, "controls.minimap.title");
    public KeyBinding keyBindWaypointToggle = new KeyBinding("key.minimap.toggleingamewaypoints", -1, "controls.minimap.title");
    public KeyBinding[] keyBindings;
    public MinecraftClient game = null;
    private boolean somethingChanged;
    public static MapSettingsManager instance;
    private List<ISubSettingsManager> subSettingsManagers = new ArrayList();

    public MapSettingsManager() {
        instance = this;
        this.game = MinecraftClient.getInstance();
        this.keyBindings = new KeyBinding[]{this.keyBindMenu, this.keyBindWaypointMenu, this.keyBindZoom, this.keyBindFullscreen, this.keyBindWaypoint, this.keyBindMobToggle, this.keyBindWaypointToggle};
    }

    public void addSecondaryOptionsManager(ISubSettingsManager secondarySettingsManager) {
        this.subSettingsManagers.add(secondarySettingsManager);
    }

    public void loadAll() {
        this.settingsFile = new File(this.game.runDirectory, "config/voxelmap.properties");

        try {
            if (this.settingsFile.exists()) {
                BufferedReader in;
                String sCurrentLine;
                for (in = new BufferedReader(new InputStreamReader(new FileInputStream(this.settingsFile), Charset.forName("UTF-8").newDecoder())); (sCurrentLine = in.readLine()) != null; KeyBinding.updateKeysByCode()) {
                    String[] curLine = sCurrentLine.split(":");
                    if (curLine[0].equals("Zoom Level")) {
                        this.zoom = Math.max(0, Math.min(4, Integer.parseInt(curLine[1])));
                    } else if (curLine[0].equals("Hide Minimap")) {
                        this.hide = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Show Coordinates")) {
                        this.coords = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Enable Cave Mode")) {
                        this.showCaves = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Dynamic Lighting")) {
                        this.lightmap = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Height Map")) {
                        this.heightmap = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Slope Map")) {
                        this.slopemap = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Blur")) {
                        this.filtering = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Water Transparency")) {
                        this.waterTransparency = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Block Transparency")) {
                        this.blockTransparency = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Biomes")) {
                        this.biomes = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Biome Overlay")) {
                        this.biomeOverlay = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                    } else if (curLine[0].equals("Chunk Grid")) {
                        this.chunkGrid = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Slime Chunks")) {
                        this.slimeChunks = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Square Map")) {
                        this.squareMap = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Rotation")) {
                        this.rotates = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Old North")) {
                        this.oldNorth = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Waypoint Beacons")) {
                        this.showBeacons = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Waypoint Signs")) {
                        this.showWaypoints = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Deathpoints")) {
                        this.deathpoints = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                    } else if (curLine[0].equals("Waypoint Max Distance")) {
                        this.maxWaypointDisplayDistance = Math.max(-1, Math.min(10000, Integer.parseInt(curLine[1])));
                    } else if (curLine[0].equals("Waypoint Sort By")) {
                        this.sort = Math.max(1, Math.min(4, Integer.parseInt(curLine[1])));
                    } else if (curLine[0].equals("Welcome Message")) {
                        this.welcome = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Real Time Torch Flicker")) {
                        this.realTimeTorches = Boolean.parseBoolean(curLine[1]);
                    } else if (curLine[0].equals("Map Corner")) {
                        this.mapCorner = Math.max(0, Math.min(3, Integer.parseInt(curLine[1])));
                    } else if (curLine[0].equals("Map Size")) {
                        this.sizeModifier = Math.max(-1, Math.min(4, Integer.parseInt(curLine[1])));
                    } else if (curLine[0].equals("Zoom Key")) {
                        this.bindKey(this.keyBindZoom, curLine[1]);
                    } else if (curLine[0].equals("Fullscreen Key")) {
                        this.bindKey(this.keyBindFullscreen, curLine[1]);
                    } else if (curLine[0].equals("Menu Key")) {
                        this.bindKey(this.keyBindMenu, curLine[1]);
                    } else if (curLine[0].equals("Waypoint Menu Key")) {
                        this.bindKey(this.keyBindWaypointMenu, curLine[1]);
                    } else if (curLine[0].equals("Waypoint Key")) {
                        this.bindKey(this.keyBindWaypoint, curLine[1]);
                    } else if (curLine[0].equals("Mob Key")) {
                        this.bindKey(this.keyBindMobToggle, curLine[1]);
                    } else if (curLine[0].equals("In-game Waypoint Key")) {
                        this.bindKey(this.keyBindWaypointToggle, curLine[1]);
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
            System.err.println(id + " is not a valid keybinding");
        }

    }

    public void saveAll() {
        File settingsFileDir = new File(this.game.runDirectory, "/config/");
        if (!settingsFileDir.exists()) {
            settingsFileDir.mkdirs();
        }

        this.settingsFile = new File(settingsFileDir, "voxelmap.properties");

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.settingsFile), Charset.forName("UTF-8").newEncoder())));
            out.println("Zoom Level:" + Integer.toString(this.zoom));
            out.println("Hide Minimap:" + Boolean.toString(this.hide));
            out.println("Show Coordinates:" + Boolean.toString(this.coords));
            out.println("Enable Cave Mode:" + Boolean.toString(this.showCaves));
            out.println("Dynamic Lighting:" + Boolean.toString(this.lightmap));
            out.println("Height Map:" + Boolean.toString(this.heightmap));
            out.println("Slope Map:" + Boolean.toString(this.slopemap));
            out.println("Blur:" + Boolean.toString(this.filtering));
            out.println("Water Transparency:" + Boolean.toString(this.waterTransparency));
            out.println("Block Transparency:" + Boolean.toString(this.blockTransparency));
            out.println("Biomes:" + Boolean.toString(this.biomes));
            out.println("Biome Overlay:" + Integer.toString(this.biomeOverlay));
            out.println("Chunk Grid:" + Boolean.toString(this.chunkGrid));
            out.println("Slime Chunks:" + Boolean.toString(this.slimeChunks));
            out.println("Square Map:" + Boolean.toString(this.squareMap));
            out.println("Rotation:" + Boolean.toString(this.rotates));
            out.println("Old North:" + Boolean.toString(this.oldNorth));
            out.println("Waypoint Beacons:" + Boolean.toString(this.showBeacons));
            out.println("Waypoint Signs:" + Boolean.toString(this.showWaypoints));
            out.println("Deathpoints:" + Integer.toString(this.deathpoints));
            out.println("Waypoint Max Distance:" + Integer.toString(this.maxWaypointDisplayDistance));
            out.println("Waypoint Sort By:" + Integer.toString(this.sort));
            out.println("Welcome Message:" + Boolean.toString(this.welcome));
            out.println("Map Corner:" + Integer.toString(this.mapCorner));
            out.println("Map Size:" + Integer.toString(this.sizeModifier));
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
        switch (par1EnumOptions) {
            case COORDS:
                return this.coords;
            case HIDE:
                return this.hide;
            case CAVEMODE:
                return this.cavesAllowed && this.showCaves;
            case LIGHTING:
                return this.lightmap;
            case SQUARE:
                return this.squareMap;
            case ROTATES:
                return this.rotates;
            case OLDNORTH:
                return this.oldNorth;
            case WELCOME:
                return this.welcome;
            case FILTERING:
                return this.filtering;
            case WATERTRANSPARENCY:
                return this.waterTransparency;
            case BLOCKTRANSPARENCY:
                return this.blockTransparency;
            case BIOMES:
                return this.biomes;
            case CHUNKGRID:
                return this.chunkGrid;
            case SLIMECHUNKS:
                return this.slimeChunks;
            default:
                throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName() + ". (possibly not a boolean applicable to minimap)");
        }
    }

    public String getOptionListValue(EnumOptionsMinimap par1EnumOptions) {
        switch (par1EnumOptions) {
            case TERRAIN:
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
            case BEACONS:
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
            case LOCATION:
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
            case SIZE:
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
            case BIOMEOVERLAY:
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
            case DEATHPOINTS:
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
            default:
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
            case COORDS:
                this.coords = !this.coords;
                break;
            case HIDE:
                this.hide = !this.hide;
                break;
            case CAVEMODE:
                this.showCaves = !this.showCaves;
                break;
            case LIGHTING:
                this.lightmap = !this.lightmap;
                break;
            case SQUARE:
                this.squareMap = !this.squareMap;
                break;
            case ROTATES:
                this.rotates = !this.rotates;
                break;
            case OLDNORTH:
                this.oldNorth = !this.oldNorth;
                break;
            case WELCOME:
                this.welcome = !this.welcome;
                break;
            case FILTERING:
                this.filtering = !this.filtering;
                break;
            case WATERTRANSPARENCY:
                this.waterTransparency = !this.waterTransparency;
                break;
            case BLOCKTRANSPARENCY:
                this.blockTransparency = !this.blockTransparency;
                break;
            case BIOMES:
                this.biomes = !this.biomes;
                break;
            case CHUNKGRID:
                this.chunkGrid = !this.chunkGrid;
                break;
            case SLIMECHUNKS:
                this.slimeChunks = !this.slimeChunks;
                break;
            case TERRAIN:
                if (this.slopemap && this.heightmap) {
                    this.slopemap = false;
                    this.heightmap = false;
                } else if (this.slopemap) {
                    this.slopemap = false;
                    this.heightmap = true;
                } else if (this.heightmap) {
                    this.slopemap = true;
                    this.heightmap = true;
                } else {
                    this.slopemap = true;
                    this.heightmap = false;
                }
                break;
            case BEACONS:
                if (this.showBeacons && this.showWaypoints) {
                    this.showBeacons = false;
                    this.showWaypoints = false;
                } else if (this.showBeacons) {
                    this.showBeacons = false;
                    this.showWaypoints = true;
                } else if (this.showWaypoints) {
                    this.showWaypoints = true;
                    this.showBeacons = true;
                } else {
                    this.showBeacons = true;
                    this.showWaypoints = false;
                }
                break;
            case LOCATION:
                this.mapCorner = this.mapCorner >= 3 ? 0 : this.mapCorner + 1;
                break;
            case SIZE:
                this.sizeModifier = this.sizeModifier >= 4 ? -1 : this.sizeModifier + 1;
                break;
            case BIOMEOVERLAY:
                ++this.biomeOverlay;
                if (this.biomeOverlay > 2) {
                    this.biomeOverlay = 0;
                }
                break;
            case DEATHPOINTS:
                ++this.deathpoints;
                if (this.deathpoints > 2) {
                    this.deathpoints = 0;
                }
                break;
            default:
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
