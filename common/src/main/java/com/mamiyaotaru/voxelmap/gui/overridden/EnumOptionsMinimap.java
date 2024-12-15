package com.mamiyaotaru.voxelmap.gui.overridden;

public enum EnumOptionsMinimap {
    COORDS("options.minimap.showcoordinates", false, true, false),
    HIDE("options.minimap.hideminimap", false, true, false),
    CAVEMODE("options.minimap.cavemode", false, true, false),
    LIGHTING("options.minimap.dynamiclighting", false, true, false),
    TERRAIN("options.minimap.terraindepth", false, false, true),
    SQUARE("options.minimap.squaremap", false, true, false),
    ROTATES("options.minimap.rotation", false, true, false),
    OLDNORTH("options.minimap.oldnorth", false, true, false),
    BEACONS("options.minimap.ingamewaypoints", false, false, true),
    WELCOME("Welcome Screen", false, true, false),
    ZOOM("option.minimapZoom", false, true, false),
    LOCATION("options.minimap.location", false, false, true),
    SIZE("options.minimap.size", false, false, true),
    FILTERING("options.minimap.filtering", false, true, false),
    WATERTRANSPARENCY("options.minimap.watertransparency", false, true, false),
    BLOCKTRANSPARENCY("options.minimap.blocktransparency", false, true, false),
    BIOMES("options.minimap.biomes", false, true, false),
    BIOMEOVERLAY("options.minimap.biomeoverlay", false, false, true),
    CHUNKGRID("options.minimap.chunkgrid", false, true, false),
    SLIMECHUNKS("options.minimap.slimechunks", false, true, false),
    WORLDBORDER("options.minimap.worldborder", false, true, false),
    RADARMODE("options.minimap.radar.radarmode", false, false, true),
    SHOWRADAR("options.minimap.radar.showradar", false, true, false),
    SHOWHOSTILES("options.minimap.radar.showhostiles", false, true, false),
    SHOWPLAYERS("options.minimap.radar.showplayers", false, true, false),
    SHOWNEUTRALS("options.minimap.radar.showneutrals", false, true, false),
    SHOWPLAYERHELMETS("options.minimap.radar.showplayerhelmets", false, true, false),
    SHOWMOBHELMETS("options.minimap.radar.showmobhelmets", false, true, false),
    SHOWPLAYERNAMES("options.minimap.radar.showplayernames", false, true, false),
    SHOWMOBNAMES("options.minimap.radar.showmobnames", false, true, false),
    RADAROUTLINES("options.minimap.radar.iconoutlines", false, true, false),
    RADARFILTERING("options.minimap.radar.iconfiltering", false, true, false),
    SHOWFACING("options.minimap.radar.showfacing", false, true, false),
    WAYPOINTDISTANCE("options.minimap.waypoints.distance", true, false, false),
    DEATHPOINTS("options.minimap.waypoints.deathpoints", false, false, true),
    SHOWWAYPOINTS("options.worldmap.showwaypoints", false, true, false),
    SHOWWAYPOINTNAMES("options.worldmap.showwaypointnames", false, true, false),
    MINZOOM("options.worldmap.minzoom", true, false, false),
    MAXZOOM("options.worldmap.maxzoom", true, false, false),
    CACHESIZE("options.worldmap.cachesize", true, false, false),
    MOVEMAPDOWNWHILESTATSUEFFECT("options.minimap.movemapdownwhilestatuseffect", false, true, false),
    MOVESCOREBOARDDOWN("options.minimap.movescoreboarddown", false, true, false),
    DISTANCEUNITCONVERSION("options.minimap.waypoints.distanceunitconversion", false, true, false),
    WAYPOINTNAMEBELOWICON("options.minimap.waypoints.waypointnamebelowicon", false, true, false);

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
