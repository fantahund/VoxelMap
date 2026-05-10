package com.mamiyaotaru.voxelmap.options;

import com.mamiyaotaru.voxelmap.VoxelConstants;

import java.util.HashMap;

public class MapPermissionsManager {
    public static final String SERVER_TELEPORT_COMMAND = "teleportCommand";
    public static final String MINIMAP_ALLOWED = "minimapAllowed";
    public static final String WORLDMAP_ALLOWED = "worldmapAllowed";
    public static final String CAVES_ALLOWED = "cavesAllowed";
    public static final String WAYPOINTS_ALLOWED = "waypointsAllowed";
    public static final String DEATH_WAYPOINT_ALLOWED = "deathWaypointAllowed";
    public static final String RADAR_ALLOWED = "radarAllowed";
    public static final String RADAR_PLAYERS_ALLOWED = "radarPlayersAllowed";
    public static final String RADAR_MOBS_ALLOWED = "radarMobsAllowed";

    private final HashMap<String, Object> permissions = new HashMap<>();

    public Object get(String name) {
        return permissions.get(name);
    }

    public void set(String name, Object value) {
        permissions.put(name, value);
    }

    public String getString(String name) {
        Object value = get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof String string) {
            return string;
        }
        return value.toString();
    }

    public boolean anyAllowed(String... names) {
        for (String key : names) {
            if (getBoolean(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean allAllowed(String... names) {
        for (String key : names) {
            if (!getBoolean(key)) {
                return false;
            }
        }
        return true;
    }

    public Boolean getBoolean(String name) {
        Object value = get(name);
        if (value instanceof Boolean booleanVal) {
            return booleanVal;
        }
        if (value instanceof String string) {
            try {
                boolean parsed = Boolean.parseBoolean(string);
                set(name, parsed);
                return parsed;
            } catch (Exception e) {
                VoxelConstants.getLogger().warn("Failed to parse Boolean permission! (Name: {}, Value: {})", name, value);
            }
        }
        return null;
    }
}
