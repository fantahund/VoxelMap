package com.mamiyaotaru.voxelmap.gui.overridden;

public enum EnumOptionsMinimap {
    // Misc / Internal
    WELCOME_SCREEN("Welcome Screen", Type.BOOLEAN),
    ZOOM("Minimap Zoom", Type.FLOAT),
    OLD_NORTH("options.minimap.oldNorth", Type.BOOLEAN),
    COLOR_PICKER_MODE("options.minimap.colorPickerMode", Type.LIST),

    // Minimap - General
    HIDE_MINIMAP("options.minimap.hideMinimap", Type.BOOLEAN),
    UPDATE_NOTIFIER("options.minimap.updateNotifier", Type.BOOLEAN),
    SHOW_BIOME("options.minimap.showBiome", Type.BOOLEAN),
    SHOW_COORDS("options.minimap.showCoordinates", Type.LIST),
    LOCATION("options.minimap.location", Type.LIST),
    SIZE("options.minimap.size", Type.LIST),
    SQUARE_MAP("options.minimap.squareMap", Type.BOOLEAN),
    ROTATES("options.minimap.rotation", Type.BOOLEAN),
    IN_GAME_WAYPOINTS("options.minimap.inGameWaypoints", Type.LIST),
    CAVE_MODE("options.minimap.caveMode", Type.BOOLEAN),
    MOVE_MAP_BELOW_STATUS_EFFECT_ICONS("options.minimap.moveMapBelowStatusEffectIcons", Type.BOOLEAN),
    MOVE_SCOREBOARD_BELOW_MAP("options.minimap.moveScoreboardBelowMap", Type.BOOLEAN),

    // Minimap - Details / Performance
    DYNAMIC_LIGHTING("options.minimap.dynamicLighting", Type.BOOLEAN),
    TERRAIN_DEPTH("options.minimap.terrainDepth", Type.LIST),
    WATER_TRANSPARENCY("options.minimap.waterTransparency", Type.BOOLEAN),
    BLOCK_TRANSPARENCY("options.minimap.blockTransparency", Type.BOOLEAN),
    BIOMES("options.minimap.biomes", Type.BOOLEAN),
    BIOME_OVERLAY("options.minimap.biomeOverlay", Type.LIST),
    CHUNK_GRID("options.minimap.chunkGrid", Type.BOOLEAN),
    SLIME_CHUNKS("options.minimap.slimeChunks", Type.BOOLEAN),
    WORLD_BORDER("options.minimap.worldBorder", Type.BOOLEAN),
    FILTERING("options.minimap.filtering", Type.BOOLEAN),
    TELEPORT_COMMAND("Teleport Command", Type.NONE),

    // Waypoint
    WAYPOINT_DISTANCE("options.minimap.waypoints.distance", Type.FLOAT),
    WAYPOINT_SIGN_SCALE("options.minimap.waypoints.waypointSignScale", Type.FLOAT),
    DEATHPOINTS("options.minimap.waypoints.deathpoints", Type.LIST),
    WAYPOINT_DISTANCE_UNIT_CONVERSION("options.minimap.waypoints.distanceUnitConversion", Type.LIST),
    WAYPOINT_LABEL_STYLE("options.minimap.waypoints.waypointLabelStyle", Type.LIST),
    HIGHLIGHT_FOCUSED_WAYPOINT("options.minimap.waypoints.highlightFocusedWaypoint", Type.BOOLEAN),

    // Radar
    SHOW_RADAR("options.minimap.radar.showRadar", Type.BOOLEAN),
    RADAR_MODE("options.minimap.radar.radarMode", Type.LIST),
    SHOW_MOBS("options.minimap.radar.showMobs", Type.LIST),
    SHOW_MOB_NAMES("options.minimap.radar.showMobNames", Type.BOOLEAN),
    SHOW_MOB_HELMETS("options.minimap.radar.showMobHelmets", Type.BOOLEAN),
    SHOW_PLAYERS("options.minimap.radar.showPlayers", Type.BOOLEAN),
    SHOW_PLAYER_NAMES("options.minimap.radar.showPlayerNames", Type.BOOLEAN),
    SHOW_PLAYER_HELMETS("options.minimap.radar.showPlayerHelmets", Type.BOOLEAN),
    RADAR_FILTERING("options.minimap.radar.iconFiltering", Type.BOOLEAN),
    RADAR_OUTLINES("options.minimap.radar.iconOutlines", Type.BOOLEAN),
    SHOW_FACING("options.minimap.radar.showFacing", Type.BOOLEAN),
    RADAR_CPU_RENDERING("options.minimap.radar.cpuRendering", Type.BOOLEAN),
    SHOW_FULL_ENTITY_NAMES("options.minimap.radar.showFullEntityNames", Type.BOOLEAN),
    SHOW_ENTITY_ELEVATION("options.minimap.radar.showEntityElevation", Type.BOOLEAN),
    HIDE_SNEAKING_PLAYERS("options.minimap.radar.hideSneakingPlayers", Type.BOOLEAN),
    HIDE_INVISIBLE_ENTITIES("options.minimap.radar.hideInvisibleEntities", Type.BOOLEAN),

    // World Map
    SHOW_WORLDMAP_COORDS("options.worldmap.showCoordinates", Type.BOOLEAN),
    SHOW_WAYPOINTS("options.worldmap.showWaypoints", Type.BOOLEAN),
    SHOW_WAYPOINT_NAMES("options.worldmap.showWaypointNames", Type.BOOLEAN),
    SHOW_DISTANT_WAYPOINTS("options.worldmap.showDistantWaypoints", Type.BOOLEAN),
    MIN_ZOOM("options.worldmap.minZoom", Type.FLOAT),
    MAX_ZOOM("options.worldmap.maxZoom", Type.FLOAT),
    CACHE_SIZE("options.worldmap.cacheSize", Type.FLOAT);

    public enum Type { NONE, BOOLEAN, LIST, FLOAT }

    private final String name;
    private final Type type;

    EnumOptionsMinimap(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public Type getType() {
        return this.type;
    }
}
