package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISubSettingsManager;
import net.minecraft.client.resources.language.I18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class PersistentMapSettingsManager implements ISubSettingsManager {
    protected int mapX;
    protected int mapZ;
    protected float zoom = 4.0F;
    private float minZoomPower = -1.0F;
    private float maxZoomPower = 4.0F;
    protected float minZoom = 0.5F;
    protected float maxZoom = 16.0F;
    protected int cacheSize = 500;
    protected boolean outputImages;
    public boolean showWaypoints = true;
    public boolean showWaypointNames = true;
    public boolean showCoordinates = true;

    @Override
    public void loadAll(File settingsFile) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(settingsFile));

            String sCurrentLine;
            while ((sCurrentLine = in.readLine()) != null) {
                String[] curLine = sCurrentLine.split(":");
                switch (curLine[0]) {
                    case "Worldmap Zoom" -> zoom = Float.parseFloat(curLine[1]);
                    case "Worldmap Minimum Zoom" -> minZoom = Float.parseFloat(curLine[1]);
                    case "Worldmap Maximum Zoom" -> maxZoom = Float.parseFloat(curLine[1]);
                    case "Worldmap Cache Size" -> cacheSize = Integer.parseInt(curLine[1]);
                    case "Show Worldmap Waypoints" -> showWaypoints = Boolean.parseBoolean(curLine[1]);
                    case "Show Worldmap Waypoint Names" -> showWaypointNames = Boolean.parseBoolean(curLine[1]);
                    case "Show Worldmap Coordinates" -> showCoordinates = Boolean.parseBoolean(curLine[1]);
                    case "Output Images" -> outputImages = Boolean.parseBoolean(curLine[1]);
                }
            }

            in.close();
        } catch (IOException ignored) {}

        for (int power = -3; power <= 5; ++power) {
            if (Math.pow(2.0, power) == minZoom) {
                minZoomPower = power;
            }

            if (Math.pow(2.0, power) == maxZoom) {
                maxZoomPower = power;
            }
        }

        bindCacheSize();
        bindZoom();
    }

    @Override
    public void saveAll(PrintWriter out) {
        out.println("Worldmap Zoom:" + zoom);
        out.println("Worldmap Minimum Zoom:" + minZoom);
        out.println("Worldmap Maximum Zoom:" + maxZoom);
        out.println("Worldmap Cache Size:" + cacheSize);
        out.println("Show Worldmap Waypoints:" + showWaypoints);
        out.println("Show Worldmap Waypoint Names:" + showWaypointNames);
        out.println("Show Worldmap Coordinates:" + showCoordinates);
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
                case MIN_ZOOM, MAX_ZOOM -> s + (float) Math.pow(2.0, value) + "x";
                case CACHE_SIZE -> s + (int) value;

                default -> s + (value <= 0.0F ? I18n.get("options.off") : (int) value + "%");
            };
        } else {
            return s + MapSettingsManager.ERROR_STRING;
        }
    }

    @Override
    public boolean getBooleanValue(EnumOptionsMinimap option) {
        return switch (option) {
            case SHOW_WAYPOINTS -> showWaypoints && VoxelConstants.getVoxelMapInstance().getMapOptions().waypointsAllowed;
            case SHOW_WAYPOINT_NAMES -> showWaypointNames && VoxelConstants.getVoxelMapInstance().getMapOptions().waypointsAllowed;
            case SHOW_WORLDMAP_COORDS -> showCoordinates;

            default -> throw new IllegalArgumentException("Invalid boolean value! Add code to handle EnumOptionMinimap: " + option.getName());
        };
    }

    @Override
    public void toggleBooleanValue(EnumOptionsMinimap option) {
        switch (option) {
            case SHOW_WAYPOINTS -> showWaypoints = !showWaypoints;
            case SHOW_WAYPOINT_NAMES -> showWaypointNames = !showWaypointNames;
            case SHOW_WORLDMAP_COORDS -> showCoordinates = !showCoordinates;

            default -> throw new IllegalArgumentException("Invalid boolean value! Add code to handle EnumOptionMinimap: " + option.getName());
        }
    }

    @Override
    public String getListValue(EnumOptionsMinimap option) {
        throw new IllegalArgumentException("Invalid list value! Add code to handle EnumOptionMinimap: " + option.getName());
    }

    @Override
    public void cycleListValue(EnumOptionsMinimap option) {
        throw new IllegalArgumentException("Invalid list value! Add code to handle EnumOptionMinimap: " + option.getName());
    }

    @Override
    public float getFloatValue(EnumOptionsMinimap option) {
        return switch (option) {
            case MIN_ZOOM -> minZoomPower;
            case MAX_ZOOM -> maxZoomPower;
            case CACHE_SIZE -> cacheSize;

            default -> throw new IllegalArgumentException("Invalid float value! Add code to handle EnumOptionMinimap: " + option.getName());
        };
    }

    @Override
    public void setFloatValue(EnumOptionsMinimap option, float value) {
        switch (option) {
            case MIN_ZOOM -> {
                minZoomPower = ((int) (value * 8.0F) - 3);
                minZoom = (float) Math.pow(2.0, minZoomPower);
                if (maxZoom < minZoom) {
                    maxZoom = minZoom;
                    maxZoomPower = minZoomPower;
                }
            }
            case MAX_ZOOM -> {
                maxZoomPower = ((int) (value * 8.0F) - 3);
                maxZoom = (float) Math.pow(2.0, maxZoomPower);
                if (minZoom > maxZoom) {
                    minZoom = maxZoom;
                    minZoomPower = maxZoomPower;
                }
            }
            case CACHE_SIZE -> {
                cacheSize = (int) (value * 5000.0F);
                cacheSize = Math.max(cacheSize, 30);

                while (cacheSize < calculateMinCacheSize()) {
                    ++minZoomPower;
                    minZoom = (float) Math.pow(2.0, minZoomPower);
                }

                if (maxZoom < minZoom) {
                    maxZoom = minZoom;
                    maxZoomPower = minZoomPower;
                }
            }

            default -> throw new IllegalArgumentException("Invalid float value! Add code to handle EnumOptionMinimap: " + option.getName());
        }

        bindZoom();
        bindCacheSize();
    }

    private int calculateMinCacheSize() {
        return (int) ((1600.0F / minZoom / 256.0F + 4.0F) * (1100.0F / minZoom / 256.0F + 3.0F) * 1.35F);
    }

    private void bindCacheSize() {
        int minCacheSize = calculateMinCacheSize();
        cacheSize = Math.max(cacheSize, minCacheSize);
    }

    private void bindZoom() {
        zoom = Math.max(zoom, minZoom);
        zoom = Math.min(zoom, maxZoom);
    }
}
