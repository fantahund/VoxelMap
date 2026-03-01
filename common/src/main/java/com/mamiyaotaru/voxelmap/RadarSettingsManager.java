package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISubSettingsManager;
import com.mojang.serialization.DataResult;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;

public class RadarSettingsManager implements ISubSettingsManager {
    private boolean somethingChanged;

    public boolean showRadar = true;
    public int radarMode = 2;
    public boolean showHostiles = true;
    public boolean showNeutrals = false;
    public boolean showMobNames = true;
    public boolean showMobHelmets = true;
    public boolean showPlayers = true;
    public boolean showPlayerNames = true;
    public boolean showPlayerHelmets = true;
    public boolean filtering = true;
    public boolean outlines = true;
    public boolean showEntityElevation = true;
    public boolean hideSneakingPlayers = true;
    public boolean showFacing = true;
    float fontScale = 1.0F;
    public final HashSet<Identifier> hiddenMobs = new HashSet<>();

    public boolean radarAllowed = true;
    public boolean radarPlayersAllowed = true;
    public boolean radarMobsAllowed = true;

    @Override
    public void loadAll(File settingsFile) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(settingsFile));

            String sCurrentLine;
            while ((sCurrentLine = in.readLine()) != null) {
                String[] curLine = sCurrentLine.split(":", 2);
                switch (curLine[0]) {
                    case "Radar Mode" -> radarMode = Math.max(1, Math.min(2, Integer.parseInt(curLine[1])));
                    case "Show Radar" -> showRadar = Boolean.parseBoolean(curLine[1]);
                    case "Show Hostiles" -> showHostiles = Boolean.parseBoolean(curLine[1]);
                    case "Show Neutrals" -> showNeutrals = Boolean.parseBoolean(curLine[1]);
                    case "Show Players" -> showPlayers = Boolean.parseBoolean(curLine[1]);
                    case "Filter Mob Icons" -> filtering = Boolean.parseBoolean(curLine[1]);
                    case "Outline Mob Icons" -> outlines = Boolean.parseBoolean(curLine[1]);
                    case "Show Player Helmets" -> showPlayerHelmets = Boolean.parseBoolean(curLine[1]);
                    case "Show Mob Helmets" -> showMobHelmets = Boolean.parseBoolean(curLine[1]);
                    case "Show Player Names" -> showPlayerNames = Boolean.parseBoolean(curLine[1]);
                    case "Show Mob Names" -> showMobNames = Boolean.parseBoolean(curLine[1]);
                    case "Font Scale" -> fontScale = Float.parseFloat(curLine[1]);
                    case "Show Facing" -> showFacing = Boolean.parseBoolean(curLine[1]);
                    case "Show Entity Elevation" -> showEntityElevation = Boolean.parseBoolean(curLine[1]);
                    case "Hide Sneaking Players" -> hideSneakingPlayers = Boolean.parseBoolean(curLine[1]);
                    case "Hidden Mobs" -> applyHiddenMobSettings(curLine[1]);
                }
            }

            in.close();
        } catch (IOException | ArrayIndexOutOfBoundsException ignored) {
        }

    }

    private void applyHiddenMobSettings(String hiddenMobs) {
        String[] mobsToHide = hiddenMobs.split(",");

        this.hiddenMobs.clear();
        for (String s : mobsToHide) {
            DataResult<Identifier> location = Identifier.read(s);
            if (location.isSuccess()) {
                this.hiddenMobs.add(location.getOrThrow());
            }
        }
    }

    @Override
    public void saveAll(PrintWriter out) {
        out.println("Radar Mode:" + radarMode);
        out.println("Show Radar:" + showRadar);
        out.println("Show Hostiles:" + showHostiles);
        out.println("Show Neutrals:" + showNeutrals);
        out.println("Show Players:" + showPlayers);
        out.println("Filter Mob Icons:" + filtering);
        out.println("Outline Mob Icons:" + outlines);
        out.println("Show Player Helmets:" + showPlayerHelmets);
        out.println("Show Mob Helmets:" + showMobHelmets);
        out.println("Show Player Names:" + showPlayerNames);
        out.println("Show Mob Names:" + showMobNames);
        out.println("Font Scale:" + fontScale);
        out.println("Show Facing:" + showFacing);
        out.println("Show Entity Elevation:" + showEntityElevation);
        out.println("Hide Sneaking Players:" + hideSneakingPlayers);
        out.print("Hidden Mobs:");
        for (Identifier mob : hiddenMobs) {
            out.print(mob.toString() + ",");
        }
        out.println();
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
            return s + (value <= 0.0F ? I18n.get("options.off") : (int) value + "%");
        } else {
            return s + MapSettingsManager.ERROR_STRING;
        }
    }

    @Override
    public boolean getBooleanValue(EnumOptionsMinimap option) {
        return switch (option) {
            case SHOW_RADAR -> showRadar;
            case SHOW_MOB_NAMES -> showMobNames;
            case SHOW_MOB_HELMETS -> showMobHelmets;
            case SHOW_PLAYERS -> showPlayers;
            case SHOW_PLAYER_NAMES -> showPlayerNames;
            case SHOW_PLAYER_HELMETS -> showPlayerHelmets;
            case RADAR_FILTERING -> filtering;
            case RADAR_OUTLINES -> outlines;
            case SHOW_FACING -> showFacing;
            case SHOW_ENTITY_ELEVATION -> showEntityElevation;
            case HIDE_SNEAKING_PLAYERS -> hideSneakingPlayers;

            default -> throw new IllegalArgumentException("Invalid boolean value! Add code to handle EnumOptionMinimap: " + option.getName());
        };
    }

    @Override
    public void toggleBooleanValue(EnumOptionsMinimap option) {
        switch (option) {
            case SHOW_RADAR -> showRadar = !showRadar;
            case SHOW_MOB_NAMES -> showMobNames = !showMobNames;
            case SHOW_MOB_HELMETS -> showMobHelmets = !showMobHelmets;
            case SHOW_PLAYERS -> showPlayers = !showPlayers;
            case SHOW_PLAYER_NAMES -> showPlayerNames = !showPlayerNames;
            case SHOW_PLAYER_HELMETS -> showPlayerHelmets = !showPlayerHelmets;
            case RADAR_FILTERING -> filtering = !filtering;
            case RADAR_OUTLINES -> outlines = !outlines;
            case SHOW_FACING -> showFacing = !showFacing;
            case SHOW_ENTITY_ELEVATION -> showEntityElevation = !showEntityElevation;
            case HIDE_SNEAKING_PLAYERS -> hideSneakingPlayers = !hideSneakingPlayers;

            default -> throw new IllegalArgumentException("Invalid boolean value! Add code to handle EnumOptionMinimap: " + option.getName());
        }

        somethingChanged = true;
    }

    @Override
    public String getListValue(EnumOptionsMinimap option) {
        switch (option) {
            case RADAR_MODE -> {
                return MapSettingsManager.parseListValue(1, radarMode,
                        I18n.get("options.minimap.radar.radarMode.simple"),
                        I18n.get("options.minimap.radar.radarMode.full"));
            }
            case SHOW_MOBS -> {
                if (!showHostiles && !showNeutrals) {
                    return I18n.get("options.off");
                } else if (showHostiles && !showNeutrals) {
                    return I18n.get("options.minimap.radar.showMobs.showHostiles");
                } else if (!showHostiles) {
                    return I18n.get("options.minimap.radar.showMobs.showNeutrals");
                } else {
                    return I18n.get("options.minimap.radar.showMobs.showAll");
                }
            }

            default -> throw new IllegalArgumentException("Invalid list value! Add code to handle EnumOptionMinimap: " + option.getName());
        }
    }

    @Override
    public void cycleListValue(EnumOptionsMinimap option) {
        switch (option) {
            case RADAR_MODE -> radarMode = radarMode == 2 ? 1 : 2;
            case SHOW_MOBS -> {
                if (!showHostiles && !showNeutrals) {
                    showHostiles = true;
                } else if (showHostiles && !showNeutrals) {
                    showHostiles = false;
                    showNeutrals = true;
                } else if (!showHostiles) {
                    showHostiles = true;
                } else {
                    showHostiles = false;
                    showNeutrals = false;
                }
            }

            default -> throw new IllegalArgumentException("Invalid list value! Add code to handle EnumOptionMinimap: " + option.getName());
        }

        somethingChanged = true;
    }

    @Override
    public float getFloatValue(EnumOptionsMinimap option) {
        throw new IllegalArgumentException("Invalid float value! Add code to handle EnumOptionMinimap: " + option.getName());
    }

    @Override
    public void setFloatValue(EnumOptionsMinimap option, float value) {
        throw new IllegalArgumentException("Invalid float value! Add code to handle EnumOptionMinimap: " + option.getName());
    }

    public boolean isChanged() {
        if (somethingChanged) {
            somethingChanged = false;
            return true;
        } else {
            return false;
        }
    }

    public boolean isMobEnabled(LivingEntity entity) {
        return isMobEnabled(entity.getType());
    }

    public boolean isMobEnabled(EntityType<?> type) {
        return !hiddenMobs.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }
}
