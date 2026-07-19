package com.mamiyaotaru.voxelmap.gui.settings;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.persistent.PersistentMapSettingsManager;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

public final class VoxelMapSettings {
    private VoxelMapSettings() {
    }

    public static List<SettingsCategory> create(Runnable openEntityTypeDialog) {
        VoxelMap voxelMap = VoxelConstants.getVoxelMapInstance();
        MapSettingsManager map = voxelMap.getMapOptions();
        RadarSettingsManager radar = voxelMap.getRadarOptions();
        PersistentMapSettingsManager world = voxelMap.getPersistentMapOptions();

        return List.of(
                general(map),
                minimap(map),
                worldMap(map, world),
                waypoints(map),
                radar(radar, openEntityTypeDialog),
                // new SettingsCategory("controls", "options.voxelmap.category.controls", List.of(), SettingsCategory.SpecialView.KEY_BINDINGS),
                advanced(voxelMap, map, radar));
    }

    private static SettingsCategory general(MapSettingsManager map) {
        return new SettingsCategory("general", "options.voxelmap.category.general", List.of(
                group("options.voxelmap.group.overlays",
                        toggle("minimap.chunkGrid", "options.minimap.chunkGrid", map, () -> map.chunkGrid, value -> map.chunkGrid = value),
                        toggle("minimap.slimeChunks", "options.minimap.slimeChunks", map, () -> map.slimeChunks, value -> map.slimeChunks = value,
                                () -> VoxelConstants.getMinecraft().hasSingleplayerServer() || !VoxelConstants.getVoxelMapInstance().getWorldSeed().isEmpty(),
                                requires("options.voxelmap.requires.seed"), 0),
                        toggle("minimap.worldBorder", "options.minimap.worldBorder", map, () -> map.worldBorder, value -> map.worldBorder = value),
                        choice("advanced.biomeOverlay", "options.voxelmap.advanced.biomeOverlay", map, () -> map.biomeOverlay, value -> map.biomeOverlay = value,
                                value(0, "options.off"), value(2, "options.minimap.biomeOverlay.transparent"), value(1, "options.voxelmap.biomeOverlay.solid"))),
                group("options.voxelmap.group.mapRendering",
                        toggle("advanced.lighting", "options.minimap.dynamicLighting", map, () -> map.dynamicLighting, value -> map.dynamicLighting = value),
                        choice("advanced.terrain", "options.minimap.terrainDepth", map,
                                () -> map.heightmap && map.slopemap ? 3 : map.heightmap ? 1 : map.slopemap ? 2 : 0,
                                value -> {
                                    map.heightmap = value == 1 || value == 3;
                                    map.slopemap = value == 2 || value == 3;
                                },
                                value(0, "options.off"), value(1, "options.minimap.terrain.height"), value(2, "options.minimap.terrain.slope"), value(3, "options.minimap.terrain.both")),
                        toggle("advanced.water", "options.minimap.waterTransparency", map, () -> map.waterTransparency, value -> map.waterTransparency = value),
                        toggle("advanced.blocks", "options.minimap.blockTransparency", map, () -> map.blockTransparency, value -> map.blockTransparency = value),
                        toggle("advanced.biomes", "options.minimap.biomes", map, () -> map.biomes, value -> map.biomes = value),
                        toggle("advanced.filtering", "options.voxelmap.advanced.textureFiltering", map, () -> map.filtering, value -> map.filtering = value))));
    }

    private static SettingsCategory minimap(MapSettingsManager map) {
        return new SettingsCategory("minimap", "options.voxelmap.category.minimap", List.of(
                group("options.voxelmap.group.visibilityPosition",
                        toggle("minimap.visible", "options.voxelmap.minimap.visible", map, () -> !map.hide, value -> map.hide = !value,
                                () -> map.minimapAllowed, serverDisabled(), 0),
                        choice("minimap.location", "options.minimap.location", map, () -> map.mapCorner, value -> map.mapCorner = value,
                                value(0, "options.minimap.location.topLeft"), value(1, "options.minimap.location.topRight"),
                                value(2, "options.minimap.location.bottomRight"), value(3, "options.minimap.location.bottomLeft")),
                        choice("minimap.size", "options.minimap.size", map, () -> map.sizeModifier, value -> map.sizeModifier = value,
                                value(-1, "options.minimap.size.small"), value(0, "options.minimap.size.medium"), value(1, "options.minimap.size.large"),
                                value(2, "options.minimap.size.xl"), value(3, "options.minimap.size.xxl"), value(4, "options.minimap.size.xxxl")),
                        choice("minimap.shape", "options.voxelmap.minimap.shape", map, () -> map.squareMap, value -> map.squareMap = value,
                                value(false, "options.voxelmap.minimap.shape.round"), value(true, "options.voxelmap.minimap.shape.square")),
                        choice("minimap.orientation", "options.voxelmap.minimap.orientation", map, () -> map.rotates, value -> map.rotates = value,
                                value(false, "options.voxelmap.minimap.orientation.north"), value(true, "options.voxelmap.minimap.orientation.rotate"))),
                group("options.voxelmap.group.information",
                        choice("minimap.coordinates", "options.voxelmap.minimap.coordinates", map, () -> map.coordsMode, value -> map.coordsMode = value,
                                value(0, "options.off"), value(2, "options.minimap.showCoordinates.horizontal"), value(1, "options.minimap.showCoordinates.classic")),
                        toggle("minimap.biome", "options.minimap.showBiome", map, () -> map.showBiome, value -> map.showBiome = value),
                        toggle("minimap.caves", "options.minimap.caveMode", map, map::isCaveModeShown, map::setCaveModeShown,
                                () -> map.cavesAllowed, serverDisabled(), 0)),
                group("options.voxelmap.group.hudLayout",
                        toggle("minimap.effects", "options.minimap.moveMapBelowStatusEffectIcons", map, () -> map.moveMapBelowStatusEffectIcons, value -> map.moveMapBelowStatusEffectIcons = value),
                        toggle("minimap.scoreboard", "options.minimap.moveScoreboardBelowMap", map, () -> map.moveScoreboardBelowMap, value -> map.moveScoreboardBelowMap = value))));
    }

    private static SettingsCategory waypoints(MapSettingsManager map) {
        return new SettingsCategory("waypoints", "options.voxelmap.category.waypoints", List.of(
                group("options.voxelmap.group.waypointDisplay",
                        toggle("waypoints.visible", "options.voxelmap.waypoints.visible", map,
                                () -> SettingsValueAdapters.waypointsVisible(map.showWaypointBeacons, map.showWaypointSigns),
                                value -> {
                                    SettingsValueAdapters.WaypointStyle style = SettingsValueAdapters.waypointVisibility(value);
                                    map.showWaypointBeacons = style.beacons();
                                    map.showWaypointSigns = style.signs();
                                },
                                () -> map.waypointsAllowed, serverDisabled(), 0),
                        SettingsOption.choice("waypoints.style", "options.voxelmap.waypoints.style", tooltip("waypoints.style"),
                                () -> SettingsValueAdapters.waypointStyle(map.showWaypointBeacons, map.showWaypointSigns),
                                value -> changed(map, ignored -> {
                                    SettingsValueAdapters.WaypointStyle style = SettingsValueAdapters.waypointStyle(value);
                                    map.showWaypointBeacons = style.beacons();
                                    map.showWaypointSigns = style.signs();
                                }, value),
                                List.of(value(1, "options.minimap.inGameWaypoints.beacons"), value(2, "options.minimap.inGameWaypoints.signs"), value(3, "options.minimap.inGameWaypoints.both")),
                                () -> map.waypointsAllowed && (map.showWaypointBeacons || map.showWaypointSigns), requires("options.voxelmap.requires.waypoints"), 1),
                        slider("waypoints.distance", "options.minimap.waypoints.distance", map,
                                () -> map.maxWaypointDisplayDistance < 0 ? 10050.0 : (double) map.maxWaypointDisplayDistance,
                                value -> map.maxWaypointDisplayDistance = value > 10000 ? -1 : value.intValue(), 50, 10050, 50,
                                value -> value > 10000 ? Component.translatable("options.minimap.waypoints.infinite") : Component.literal(value.intValue() + " m"),
                                () -> map.waypointsAllowed, requires("options.voxelmap.requires.waypoints"), 1),
                        slider("waypoints.scale", "options.minimap.waypoints.waypointSignScale", map, () -> (double) map.waypointSignScale,
                                value -> map.waypointSignScale = value.floatValue(), 0.5, 1.5, 0.01,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value)),
                                () -> map.waypointsAllowed && map.showWaypointSigns, requires("options.voxelmap.requires.waypointSigns"), 1),
                        choice("waypoints.layout", "options.minimap.waypoints.signLayout", map, () -> map.waypointSignLayout,
                                value -> map.waypointSignLayout = value, value(0, "options.off"),
                                value(1, "options.minimap.waypoints.signLayout.default"), value(2, "options.minimap.waypoints.signLayout.classicTop"), value(3, "options.minimap.waypoints.signLayout.classicBottom")),
                        toggle("waypoints.highlight", "options.minimap.waypoints.highlightSignOnFocus", map, () -> map.highlightSignOnFocus, value -> map.highlightSignOnFocus = value),
                        choice("waypoints.units", "options.minimap.waypoints.distanceUnitConversion", map, () -> map.waypointDistanceConversion,
                                value -> map.waypointDistanceConversion = value, value(0, "options.off"),
                                value(1, "options.minimap.waypoints.distanceUnitConversion.from1000m"), value(2, "options.minimap.waypoints.distanceUnitConversion.from10000m"))),
                group("options.voxelmap.group.deathpoints",
                        toggle("waypoints.deathpoints", "options.voxelmap.waypoints.deathpoints.enabled", map, () -> map.deathpoints != 0,
                                value -> map.deathpoints = SettingsValueAdapters.optionalMode(value, map.deathpoints), () -> map.deathWaypointAllowed, serverDisabled(), 0),
                        SettingsOption.choice("waypoints.deathpointRetention", "options.voxelmap.waypoints.deathpoints.retention", tooltip("waypoints.deathpointRetention"),
                                () -> map.deathpoints == 2 ? 2 : 1, value -> changed(map, ignored -> map.deathpoints = value, value),
                                List.of(value(1, "options.minimap.waypoints.deathpoints.mostRecent"), value(2, "options.minimap.waypoints.deathpoints.all")),
                                () -> map.deathWaypointAllowed && map.deathpoints != 0, requires("options.voxelmap.requires.deathpoints"), 1))));
    }

    private static SettingsCategory radar(RadarSettingsManager radar, Runnable openEntityTypeDialog) {
        return new SettingsCategory("radar", "options.voxelmap.category.radar", List.of(
                group("options.voxelmap.group.radarGeneral",
                        toggle("radar.visible", "options.minimap.radar.showRadar", radar, () -> radar.showRadar, value -> radar.showRadar = value,
                                () -> radarAvailable(radar), serverDisabled(), 0),
                        choice("radar.mode", "options.minimap.radar.radarMode", radar, () -> radar.radarMode, value -> radar.radarMode = value,
                                value(1, "options.voxelmap.radar.mode.simple"), value(2, "options.voxelmap.radar.mode.full")),
                        dependentRadarToggle("radar.elevation", "options.minimap.radar.showEntityElevation", radar, () -> radar.showEntityElevation, value -> radar.showEntityElevation = value, () -> true),
                        dependentRadarToggle("radar.invisible", "options.minimap.radar.hideInvisibleEntities", radar, () -> radar.hideInvisibleEntities, value -> radar.hideInvisibleEntities = value, () -> true)),
                group("options.voxelmap.group.players",
                        toggle("radar.players", "options.minimap.radar.showPlayers", radar, () -> radar.showPlayers, value -> radar.showPlayers = value,
                                () -> radarEnabled(radar) && radar.radarPlayersAllowed, serverDisabled(), 0),
                        dependentRadarToggle("radar.playerNames", "options.minimap.radar.showPlayerNames", radar, () -> radar.showPlayerNames, value -> radar.showPlayerNames = value, () -> radar.showPlayers && radar.radarMode == 2),
                        dependentRadarToggle("radar.playerHelmets", "options.minimap.radar.showPlayerHelmets", radar, () -> radar.showPlayerHelmets, value -> radar.showPlayerHelmets = value, () -> radar.showPlayers && radar.radarMode == 2),
                        dependentRadarToggle("radar.sneaking", "options.minimap.radar.hideSneakingPlayers", radar, () -> radar.hideSneakingPlayers, value -> radar.hideSneakingPlayers = value, () -> radar.showPlayers),
                        dependentRadarToggle("radar.facing", "options.minimap.radar.showFacing", radar, () -> radar.showFacing, value -> radar.showFacing = value,
                                () -> radar.radarMode == 1 && (radar.showPlayers || radar.showHostiles || radar.showNeutrals))),
                group("options.voxelmap.group.creatures",
                        toggle("radar.hostiles", "options.voxelmap.radar.hostiles", radar, () -> radar.showHostiles, value -> radar.showHostiles = value,
                                () -> radarEnabled(radar) && radar.radarMobsAllowed, serverDisabled(), 0),
                        toggle("radar.neutrals", "options.voxelmap.radar.neutrals", radar, () -> radar.showNeutrals, value -> radar.showNeutrals = value,
                                () -> radarEnabled(radar) && radar.radarMobsAllowed, serverDisabled(), 0),
                        dependentRadarToggle("radar.mobNames", "options.minimap.radar.showMobNames", radar, () -> radar.showMobNames, value -> radar.showMobNames = value, () -> radar.radarMode == 2 && (radar.showHostiles || radar.showNeutrals)),
                        dependentRadarToggle("radar.mobHelmets", "options.minimap.radar.showMobHelmets", radar, () -> radar.showMobHelmets, value -> radar.showMobHelmets = value, () -> radar.radarMode == 2 && (radar.showHostiles || radar.showNeutrals)),
                        dependentRadarToggle("radar.fullNames", "options.minimap.radar.showFullEntityNames", radar, () -> radar.showFullEntityNames, value -> radar.showFullEntityNames = value, () -> radar.radarMode == 2 && (radar.showHostiles || radar.showNeutrals)),
                        SettingsOption.action("radar.individualEntities", "options.voxelmap.radar.individualEntities", tooltip("radar.individualEntities"),
                                Component.translatable("options.voxelmap.action.open"), openEntityTypeDialog,
                                () -> radarEnabled(radar) && radar.radarMobsAllowed && (radar.showHostiles || radar.showNeutrals),
                                () -> Component.translatable(!radar.radarAllowed || !radar.radarMobsAllowed
                                        ? "options.voxelmap.managed.serverDisabled"
                                        : "options.voxelmap.requires.entityCategory"))),
                group("options.voxelmap.group.iconDisplay",
                        dependentRadarToggle("radar.filtering", "options.minimap.radar.iconFiltering", radar, () -> radar.filtering, value -> radar.filtering = value, () -> radar.radarMode == 2),
                        dependentRadarToggle("radar.outlines", "options.minimap.radar.iconOutlines", radar, () -> radar.outlines, value -> radar.outlines = value, () -> radar.radarMode == 2))));
    }

    private static SettingsCategory worldMap(MapSettingsManager map, PersistentMapSettingsManager world) {
        return new SettingsCategory("worldmap", "options.voxelmap.category.worldmap", List.of(
                group("options.voxelmap.group.worldmapDisplay",
                        persistentToggle("worldmap.coordinates", "options.worldmap.showCoordinates", world, EnumOptionsMinimap.SHOW_WORLDMAP_COORDS, () -> true, Component::empty, 0),
                        persistentToggle("worldmap.waypoints", "options.worldmap.showWaypoints", world, EnumOptionsMinimap.SHOW_WAYPOINTS, () -> map.waypointsAllowed, serverDisabled(), 0),
                        persistentToggle("worldmap.names", "options.worldmap.showWaypointNames", world, EnumOptionsMinimap.SHOW_WAYPOINT_NAMES, () -> map.waypointsAllowed && world.showWaypoints, requires("options.voxelmap.requires.worldmapWaypoints"), 1),
                        persistentToggle("worldmap.distant", "options.worldmap.showDistantWaypoints", world, EnumOptionsMinimap.SHOW_DISTANT_WAYPOINTS, () -> map.waypointsAllowed && world.showWaypoints, requires("options.voxelmap.requires.worldmapWaypoints"), 1)),
                group("options.voxelmap.group.zoomCache",
                        SettingsOption.slider("worldmap.farthestZoom", "options.voxelmap.worldmap.farthestZoom", tooltip("worldmap.farthestZoom"),
                                () -> (double) world.getFloatValue(EnumOptionsMinimap.MIN_ZOOM), value -> world.setFloatValue(EnumOptionsMinimap.MIN_ZOOM, (float) ((value + 3.0) / 8.0)),
                                -3, 5, 1, VoxelMapSettings::zoomLabel, () -> true, Component::empty, 0),
                        SettingsOption.slider("worldmap.nearestZoom", "options.voxelmap.worldmap.nearestZoom", tooltip("worldmap.nearestZoom"),
                                () -> (double) world.getFloatValue(EnumOptionsMinimap.MAX_ZOOM), value -> world.setFloatValue(EnumOptionsMinimap.MAX_ZOOM, (float) ((value + 3.0) / 8.0)),
                                -3, 5, 1, VoxelMapSettings::zoomLabel, () -> true, Component::empty, 0),
                        SettingsOption.slider("worldmap.cache", "options.worldmap.cacheSize", tooltip("worldmap.cache"),
                                () -> (double) world.getFloatValue(EnumOptionsMinimap.CACHE_SIZE), value -> world.setFloatValue(EnumOptionsMinimap.CACHE_SIZE, (float) (value / 5000.0)),
                                30, 5000, 10, value -> Component.translatable("options.voxelmap.value.regions", value.intValue()), () -> true, Component::empty, 0))));
    }

    private static SettingsCategory advanced(VoxelMap voxelMap, MapSettingsManager map, RadarSettingsManager radar) {
        return new SettingsCategory("advanced", "options.voxelmap.category.advanced", List.of(
                group("options.voxelmap.group.worldServer",
                        SettingsOption.text("advanced.seed", "options.minimap.worldSeed", tooltip("advanced.seed"), voxelMap::getWorldSeed,
                                value -> {
                                    voxelMap.setWorldSeed(value.trim());
                                    voxelMap.getMap().forceFullRender(true);
                                },
                                () -> !VoxelConstants.getMinecraft().hasSingleplayerServer(), requires("options.voxelmap.managed.singleplayerSeed")),
                        SettingsOption.text("advanced.teleport", "options.minimap.teleportCommand", tooltip("advanced.teleport"), () -> map.teleportCommand,
                                value -> {
                                    map.teleportCommand = value.isBlank() ? "tp %p %x %y %z" : value;
                                    map.markChanged();
                                },
                                () -> map.serverTeleportCommand == null, requires("options.voxelmap.managed.server"))),
                group("options.voxelmap.group.troubleshooting",
                        toggle("advanced.compatibilityRenderer", "options.minimap.radar.cpuRendering", radar,
                                () -> radar.cpuRendering || radar.forceCpuRendering, value -> radar.cpuRendering = value || radar.forceCpuRendering,
                                () -> !radar.forceCpuRendering, requires("options.voxelmap.managed.renderer"), 0)),
                group("options.voxelmap.group.interface",
                        choice("advanced.colorPicker", "options.minimap.colorPickerMode", map, () -> map.colorPickerMode, value -> map.colorPickerMode = value,
                                value(0, "options.minimap.colorPickerMode.simple"), value(1, "options.minimap.colorPickerMode.full")),
                        toggle("advanced.updates", "options.minimap.updateNotifier", map, () -> map.updateNotifier, value -> map.updateNotifier = value))));
    }

    private static boolean radarAvailable(RadarSettingsManager radar) {
        return radar.radarAllowed && (radar.radarPlayersAllowed || radar.radarMobsAllowed);
    }

    private static boolean radarEnabled(RadarSettingsManager radar) {
        return radarAvailable(radar) && radar.showRadar;
    }

    private static SettingsOption<Boolean> dependentRadarToggle(String id, String key, RadarSettingsManager radar, java.util.function.Supplier<Boolean> getter,
            Consumer<Boolean> setter, java.util.function.BooleanSupplier extra) {
        return toggle(id, key, radar, getter, setter, () -> radarEnabled(radar) && extra.getAsBoolean(), requires("options.voxelmap.requires.radar"), 1);
    }

    private static SettingsOption<Boolean> persistentToggle(String id, String key, PersistentMapSettingsManager manager, EnumOptionsMinimap option,
            java.util.function.BooleanSupplier enabled, java.util.function.Supplier<Component> reason, int indentation) {
        return SettingsOption.toggle(id, key, tooltip(id), () -> manager.getBooleanValue(option), value -> {
            if (manager.getBooleanValue(option) != value)
                manager.toggleBooleanValue(option);
        }, enabled, reason, indentation);
    }

    private static SettingsOption<Boolean> toggle(String id, String key, MapSettingsManager manager, java.util.function.Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return toggle(id, key, manager, getter, setter, () -> true, Component::empty, 0);
    }

    private static SettingsOption<Boolean> toggle(String id, String key, MapSettingsManager manager, java.util.function.Supplier<Boolean> getter,
            Consumer<Boolean> setter, java.util.function.BooleanSupplier enabled, java.util.function.Supplier<Component> reason, int indentation) {
        return SettingsOption.toggle(id, key, tooltip(id), getter, value -> changed(manager, setter, value), enabled, reason, indentation);
    }

    private static SettingsOption<Boolean> toggle(String id, String key, RadarSettingsManager manager, java.util.function.Supplier<Boolean> getter,
            Consumer<Boolean> setter, java.util.function.BooleanSupplier enabled, java.util.function.Supplier<Component> reason, int indentation) {
        return SettingsOption.toggle(id, key, tooltip(id), getter, value -> {
            setter.accept(value);
            manager.markChanged();
        }, enabled, reason, indentation);
    }

    private static SettingsOption<Boolean> toggle(String id, String key, RadarSettingsManager manager, java.util.function.Supplier<Boolean> getter,
            Consumer<Boolean> setter) {
        return toggle(id, key, manager, getter, setter, () -> true, Component::empty, 0);
    }

    @SafeVarargs
    private static <T> SettingsOption<T> choice(String id, String key, MapSettingsManager manager, java.util.function.Supplier<T> getter,
            Consumer<T> setter, SettingsOption.Choice<T>... values) {
        return SettingsOption.choice(id, key, tooltip(id), getter, value -> changed(manager, setter, value), List.of(values));
    }

    @SafeVarargs
    private static <T> SettingsOption<T> choice(String id, String key, RadarSettingsManager manager, java.util.function.Supplier<T> getter,
            Consumer<T> setter, SettingsOption.Choice<T>... values) {
        return SettingsOption.choice(id, key, tooltip(id), getter, value -> {
            setter.accept(value);
            manager.markChanged();
        }, List.of(values),
                () -> radarEnabled(manager), requires("options.voxelmap.requires.radar"), 1);
    }

    private static SettingsOption<Double> slider(String id, String key, MapSettingsManager manager, java.util.function.Supplier<Double> getter,
            Consumer<Double> setter, double min, double max, double step, java.util.function.Function<Double, Component> formatter,
            java.util.function.BooleanSupplier enabled, java.util.function.Supplier<Component> reason, int indentation) {
        return SettingsOption.slider(id, key, tooltip(id), getter, value -> changed(manager, setter, value), min, max, step, formatter, enabled, reason, indentation);
    }

    private static SettingsGroup group(String titleKey, SettingsOption<?>... options) {
        return new SettingsGroup(titleKey, List.of(options));
    }

    private static <T> SettingsOption.Choice<T> value(T value, String key) {
        return new SettingsOption.Choice<>(value, Component.translatable(key));
    }

    private static String tooltip(String id) {
        return "options.voxelmap." + id + ".tooltip";
    }

    private static java.util.function.Supplier<Component> requires(String key) {
        return () -> Component.translatable(key);
    }

    private static java.util.function.Supplier<Component> serverDisabled() {
        return requires("options.voxelmap.managed.serverDisabled");
    }

    private static Component zoomLabel(Double power) {
        return Component.literal(String.format(Locale.ROOT, "%.3gx", Math.pow(2.0, power)));
    }

    private static <T> void changed(MapSettingsManager manager, Consumer<T> setter, T value) {
        setter.accept(value);
        manager.markChanged();
    }
}
