package com.mamiyaotaru.voxelmap.options;

import com.mamiyaotaru.voxelmap.options.fields.BooleanField;
import com.mamiyaotaru.voxelmap.options.fields.StringField;

public class ServerSettingsManager {
    public final BooleanField minimapAllowed;
    public final BooleanField worldmapAllowed;
    public final BooleanField cavesAllowed;
    public final BooleanField waypointsAllowed;
    public final BooleanField deathpointsAllowd;
    public final BooleanField radarAllowed;
    public final BooleanField radarMobsAllowed;
    public final BooleanField radarPlayersAllowed;
    public final StringField serverTeleportCommand;

    public ServerSettingsManager() {
        minimapAllowed = new BooleanField("Minimap Allowed", "", true);
        worldmapAllowed = new BooleanField("WorldMap Allowed", "", true);
        cavesAllowed = new BooleanField("Caves Allowed", "", true);
        waypointsAllowed = new BooleanField("Waypoints Allowed", "", true);
        deathpointsAllowd = new BooleanField("Deathpoints Allowed", "", true);
        radarAllowed = new BooleanField("Radar Allowed", "", true);
        radarMobsAllowed = new BooleanField("Radar Mobs Allowed", "", true);
        radarPlayersAllowed = new BooleanField("Radar Players Allowed", "", true);
        serverTeleportCommand = new StringField("Server Teleport Command", "", "", StringField.PATTERN_NONE);
        reset();
    }

    public void reset() {
        minimapAllowed.set(true);
        worldmapAllowed.set(true);
        cavesAllowed.set(true);
        waypointsAllowed.set(true);
        deathpointsAllowd.set(true);
        radarAllowed.set(true);
        radarMobsAllowed.set(true);
        radarPlayersAllowed.set(true);
        serverTeleportCommand.set("");
    }
}
