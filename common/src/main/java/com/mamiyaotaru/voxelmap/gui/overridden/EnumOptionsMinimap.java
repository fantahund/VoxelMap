package com.mamiyaotaru.voxelmap.gui.overridden;

public enum EnumOptionsMinimap {
    SHOW_COORDS("options.minimap.showcoordinates", false, true, false),
    HIDE_MINIMAP("options.minimap.hideminimap", false, true, false),
    CAVE_MODE("options.minimap.cavemode", false, true, false),
    DYNAMIC_LIGHTING("options.minimap.dynamiclighting", false, true, false),
    TERRAIN_DEPTH("options.minimap.terraindepth", false, false, true),
    SQUARE_MAP("options.minimap.squaremap", false, true, false),
    ROTATES("options.minimap.rotation", false, true, false),
    OLD_NORTH("options.minimap.oldnorth", false, true, false),
    IN_GAME_WAYPOINTS("options.minimap.ingamewaypoints", false, false, true),
    WELCOME_SCREEN("Welcome Screen", false, true, false),
    ZOOM("option.minimapZoom", false, true, false),
    LOCATION("options.minimap.location", false, false, true),
    SIZE("options.minimap.size", false, false, true),
    FILTERING("options.minimap.filtering", false, true, false),
    WATER_TRANSPARENCY("options.minimap.watertransparency", false, true, false),
    BLOCK_TRANSPARENCY("options.minimap.blocktransparency", false, true, false),
    BIOMES("options.minimap.biomes", false, true, false),
    BIOME_OVERLAY("options.minimap.biomeoverlay", false, false, true),
    CHUNK_GRID("options.minimap.chunkgrid", false, true, false),
    SLIME_CHUNKS("options.minimap.slimechunks", false, true, false),
    WORLD_BORDER("options.minimap.worldborder", false, true, false),
    RADAR_MODE("options.minimap.radar.radarmode", false, false, true),
    SHOW_RADAR("options.minimap.radar.showradar", false, true, false),
    SHOW_HOSTILES("options.minimap.radar.showhostiles", false, true, false),
    SHOW_PLAYERS("options.minimap.radar.showplayers", false, true, false),
    SHOW_NEUTRALS("options.minimap.radar.showneutrals", false, true, false),
    SHOW_PLAYER_HELMETS("options.minimap.radar.showplayerhelmets", false, true, false),
    SHOW_MOB_HELMETS("options.minimap.radar.showmobhelmets", false, true, false),
    SHOW_PLAYER_NAMES("options.minimap.radar.showplayernames", false, true, false),
    SHOW_MOB_NAMES("options.minimap.radar.showmobnames", false, true, false),
    RADAR_OUTLINES("options.minimap.radar.iconoutlines", false, true, false),
    RADAR_FILTERING("options.minimap.radar.iconfiltering", false, true, false),
    SHOW_FACING("options.minimap.radar.showfacing", false, true, false),
    WAYPOINT_DISTANCE("options.minimap.waypoints.distance", true, false, false),
    DEATHPOINTS("options.minimap.waypoints.deathpoints", false, false, true),
    SHOW_WAYPOINTS("options.worldmap.showwaypoints", false, true, false),
    SHOW_WAYPOINT_NAMES("options.worldmap.showwaypointnames", false, true, false),
    MIN_ZOOM("options.worldmap.minzoom", true, false, false),
    MAX_ZOOM("options.worldmap.maxzoom", true, false, false),
    CACHE_SIZE("options.worldmap.cachesize", true, false, false),
    MOVE_MAP_DOWN_WHILE_STATUS_EFFECT("options.minimap.movemapdownwhilestatuseffect", false, true, false),
    MOVE_SCOREBOARD_DOWN("options.minimap.movescoreboarddown", false, true, false),
    DISTANCE_UNIT_CONVERSION("options.minimap.waypoints.distanceunitconversion", false, true, false),
    SHOW_IN_GAME_WAYPOINT_NAMES("options.minimap.waypoints.showWaypointNames", false, false, true),
    SHOW_IN_GAME_WAYPOINT_DISTANCES("options.minimap.waypoints.showWaypointDistances", false, false, true);

    private final boolean isFloat;
    private final boolean isBoolean;
    private final boolean isList;
    private final String name;

    EnumOptionsMinimap(String name, boolean isFloat, boolean isBoolean, boolean isList) {
        this.name = name;
        this.isFloat = isFloat;
        this.isBoolean = isBoolean;
        this.isList = isList;
    }

    public boolean isFloat() {
        return this.isFloat;
    }

    public boolean isBoolean() {
        return this.isBoolean;
    }

    public boolean isList() {
        return this.isList;
    }

    public String getName() {
        return this.name;
    }
}
