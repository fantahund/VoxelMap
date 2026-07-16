package com.mamiyaotaru.voxelmap.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VoxelmapServerConfig {
    private static final String DEFAULT_CONFIG = "defaultConfig";
    private static final String WORLD_OVERRIDES = "worldOverrides";
    private static final String WORLDS = "worlds";
    private static final String SETTINGS = "settings";

    private final VoxelmapServerSettings defaultConfig;
    private final List<WorldOverride> worldOverrides;
    private final Map<String, VoxelmapServerSettings> overridesByWorld;

    public VoxelmapServerConfig(VoxelmapServerSettings defaultConfig, List<WorldOverride> worldOverrides) {
        this.defaultConfig = defaultConfig;
        this.worldOverrides = List.copyOf(worldOverrides);
        this.overridesByWorld = buildOverrideLookup(worldOverrides);
    }

    public static VoxelmapServerConfig defaults() {
        return new VoxelmapServerConfig(VoxelmapServerSettings.defaults(), List.of());
    }

    public static VoxelmapServerConfig fromJson(JsonObject root) {
        validateRootKeys(root);

        JsonObject defaultJson = getRequiredObject(root, DEFAULT_CONFIG, "root config");
        VoxelmapServerSettings defaultConfig = VoxelmapServerSettings.defaults()
                .merge(VoxelmapServerSettings.fromJson(defaultJson, DEFAULT_CONFIG));

        List<WorldOverride> worldOverrides = new ArrayList<>();
        JsonElement overridesElement = root.get(WORLD_OVERRIDES);
        if (overridesElement != null) {
            if (!overridesElement.isJsonArray()) {
                throw new IllegalArgumentException("'" + WORLD_OVERRIDES + "' must be an array");
            }

            JsonArray overridesArray = overridesElement.getAsJsonArray();
            for (int i = 0; i < overridesArray.size(); i++) {
                worldOverrides.add(parseWorldOverride(overridesArray.get(i), i));
            }
        }

        return new VoxelmapServerConfig(defaultConfig, worldOverrides);
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.add(DEFAULT_CONFIG, defaultConfig.toConfigJson());

        JsonArray overridesJson = new JsonArray();
        for (WorldOverride override : worldOverrides) {
            overridesJson.add(override.toJson());
        }
        root.add(WORLD_OVERRIDES, overridesJson);

        return root;
    }

    public JsonObject createClientSettingsJson(String worldId) {
        VoxelmapServerSettings override = overridesByWorld.get(worldId);
        VoxelmapServerSettings effectiveSettings = override == null ? defaultConfig : defaultConfig.merge(override);
        return effectiveSettings.toClientSettingsJson(worldId);
    }

    private static WorldOverride parseWorldOverride(JsonElement element, int index) {
        String context = WORLD_OVERRIDES + "[" + index + "]";
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException(context + " must be an object");
        }

        JsonObject overrideJson = element.getAsJsonObject();
        validateOverrideKeys(overrideJson, context);

        JsonArray worldsJson = getRequiredArray(overrideJson, WORLDS, context);
        if (worldsJson.isEmpty()) {
            throw new IllegalArgumentException("'" + WORLDS + "' in " + context + " must not be empty");
        }

        List<String> worlds = new ArrayList<>();
        Set<String> worldsInThisOverride = new HashSet<>();
        for (int i = 0; i < worldsJson.size(); i++) {
            String world = readWorldId(worldsJson.get(i), context + "." + WORLDS + "[" + i + "]");
            if (!worldsInThisOverride.add(world)) {
                throw new IllegalArgumentException("Duplicate world '" + world + "' in " + context);
            }
            worlds.add(world);
        }

        JsonObject settingsJson = getRequiredObject(overrideJson, SETTINGS, context);
        VoxelmapServerSettings settings = VoxelmapServerSettings.fromJson(settingsJson, context + "." + SETTINGS);
        return new WorldOverride(worlds, settings);
    }

    private static Map<String, VoxelmapServerSettings> buildOverrideLookup(List<WorldOverride> worldOverrides) {
        Map<String, VoxelmapServerSettings> overridesByWorld = new LinkedHashMap<>();
        for (WorldOverride override : worldOverrides) {
            for (String world : override.worlds()) {
                if (overridesByWorld.putIfAbsent(world, override.settings()) != null) {
                    throw new IllegalArgumentException("Duplicate world override for '" + world + "'");
                }
            }
        }
        return Map.copyOf(overridesByWorld);
    }

    private static void validateRootKeys(JsonObject root) {
        for (String key : root.keySet()) {
            if (!DEFAULT_CONFIG.equals(key) && !WORLD_OVERRIDES.equals(key)) {
                throw new IllegalArgumentException("Unknown root config key '" + key + "'");
            }
        }
    }

    private static void validateOverrideKeys(JsonObject overrideJson, String context) {
        for (String key : overrideJson.keySet()) {
            if (!WORLDS.equals(key) && !SETTINGS.equals(key)) {
                throw new IllegalArgumentException("Unknown key '" + key + "' in " + context);
            }
        }
    }

    private static JsonObject getRequiredObject(JsonObject parent, String key, String context) {
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("'" + key + "' in " + context + " must be an object");
        }
        return element.getAsJsonObject();
    }

    private static JsonArray getRequiredArray(JsonObject parent, String key, String context) {
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("'" + key + "' in " + context + " must be an array");
        }
        return element.getAsJsonArray();
    }

    private static String readWorldId(JsonElement element, String context) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(context + " must be a string");
        }

        String world = element.getAsString();
        if (world.isBlank()) {
            throw new IllegalArgumentException(context + " must not be blank");
        }

        return world;
    }

    public record WorldOverride(List<String> worlds, VoxelmapServerSettings settings) {
        public WorldOverride {
            worlds = List.copyOf(worlds);
        }

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            JsonArray worldsJson = new JsonArray();
            for (String world : worlds) {
                worldsJson.add(world);
            }
            json.add(WORLDS, worldsJson);
            json.add(SETTINGS, settings.toConfigJson());
            return json;
        }
    }
}
