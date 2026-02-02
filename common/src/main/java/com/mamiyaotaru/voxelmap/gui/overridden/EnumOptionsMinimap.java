package com.mamiyaotaru.voxelmap.gui.overridden;

public enum EnumOptionsMinimap {
    SHOW_COORDS("options.minimap.showCoordinates", false, false, true),
    HIDE_MINIMAP("options.minimap.hideMinimap", false, true, false),
    CAVE_MODE("options.minimap.caveMode", false, true, false),
    DYNAMIC_LIGHTING("options.minimap.dynamicLighting", false, true, false),
    TERRAIN_DEPTH("options.minimap.terrainDepth", false, false, true),
    SQUARE_MAP("options.minimap.squareMap", false, true, false),
    ROTATES("options.minimap.rotation", false, true, false),
    OLD_NORTH("options.minimap.oldNorth", false, true, false),
    IN_GAME_WAYPOINTS("options.minimap.inGameWaypoints", false, false, true),
    WELCOME_SCREEN("Welcome Screen", false, true, false),
    ZOOM("option.minimapZoom", false, true, false),
    LOCATION("options.minimap.location", false, false, true),
    SIZE("options.minimap.size", false, false, true),
    FILTERING("options.minimap.filtering", false, true, false),
    WATER_TRANSPARENCY("options.minimap.waterTransparency", false, true, false),
    BLOCK_TRANSPARENCY("options.minimap.blockTransparency", false, true, false),
    BIOMES("options.minimap.biomes", false, true, false),
    BIOME_OVERLAY("options.minimap.biomeOverlay", false, false, true),
    CHUNK_GRID("options.minimap.chunkGrid", false, true, false),
    SLIME_CHUNKS("options.minimap.slimeChunks", false, true, false),
    WORLD_BORDER("options.minimap.worldBorder", false, true, false),
    MOVE_MAP_DOWN_WHILE_STATUS_EFFECT("options.minimap.moveMapBelowStatusEffectIcons", false, true, false),
    MOVE_SCOREBOARD_DOWN("options.minimap.moveScoreboardBelowMap", false, true, false),
    SHOW_BIOME("options.minimap.showBiome", false, true, false),
    UPDATE_NOTIFIER("options.minimap.updateNotifier", false, true, false),
    COLOR_PICKER_MODE("options.minimap.colorPickerMode", false, false, true),
    RADAR_MODE("options.minimap.radar.radarMode", false, false, true),
    SHOW_RADAR("options.minimap.radar.showRadar", false, true, false),
    SHOW_MOBS("options.minimap.radar.showMobs", false, false, true),
    SHOW_PLAYERS("options.minimap.radar.showPlayers", false, true, false),
    SHOW_PLAYER_HELMETS("options.minimap.radar.showPlayerHelmets", false, true, false),
    SHOW_MOB_HELMETS("options.minimap.radar.showMobHelmets", false, true, false),
    SHOW_PLAYER_NAMES("options.minimap.radar.showPlayerNames", false, true, false),
    SHOW_MOB_NAMES("options.minimap.radar.showMobNames", false, true, false),
    RADAR_OUTLINES("options.minimap.radar.iconOutlines", false, true, false),
    RADAR_FILTERING("options.minimap.radar.iconFiltering", false, true, false),
    SHOW_FACING("options.minimap.radar.showFacing", false, true, false),
    WAYPOINT_DISTANCE("options.minimap.waypoints.distance", true, false, false),
    DEATHPOINTS("options.minimap.waypoints.deathpoints", false, false, true),
    SHOW_WAYPOINTS("options.worldmap.showWaypoints", false, true, false),
    SHOW_WAYPOINT_NAMES("options.worldmap.showWaypointNames", false, true, false),
    MIN_ZOOM("options.worldmap.minZoom", true, false, false),
    MAX_ZOOM("options.worldmap.maxZoom", true, false, false),
    CACHE_SIZE("options.worldmap.cacheSize", true, false, false),
    TELEPORT_COMMAND("Teleport Command", false, false, false),
    DISTANCE_UNIT_CONVERSION("options.minimap.waypoints.distanceUnitConversion", false, false, true),
    WAYPOINT_SIGN_SCALE("options.minimap.waypoints.waypointSignScale", true, false, false),
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
