package com.mamiyaotaru.voxelmap.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VoxelmapServerSettings {
    public static final String RADAR_ALLOWED = "radarAllowed";
    public static final String RADAR_MOBS_ALLOWED = "radarMobsAllowed";
    public static final String RADAR_PLAYERS_ALLOWED = "radarPlayersAllowed";
    public static final String CAVES_ALLOWED = "cavesAllowed";
    public static final String MINIMAP_ALLOWED = "minimapAllowed";
    public static final String WORLDMAP_ALLOWED = "worldmapAllowed";
    public static final String WAYPOINTS_ALLOWED = "waypointsAllowed";
    public static final String DEATH_WAYPOINT_ALLOWED = "deathWaypointAllowed";
    public static final String TELEPORT_COMMAND = "teleportCommand";
    public static final String WORLD_NAME_SUFFIX = "worldNameSuffix";
    public static final String SERVER_IDENTITY = "serverIdentity";

    private static final List<String> BOOLEAN_KEYS = List.of(
            RADAR_ALLOWED,
            RADAR_MOBS_ALLOWED,
            RADAR_PLAYERS_ALLOWED,
            CAVES_ALLOWED,
            MINIMAP_ALLOWED,
            WORLDMAP_ALLOWED,
            WAYPOINTS_ALLOWED,
            DEATH_WAYPOINT_ALLOWED
    );
    private static final Set<String> KNOWN_KEYS = Set.of(
            RADAR_ALLOWED,
            RADAR_MOBS_ALLOWED,
            RADAR_PLAYERS_ALLOWED,
            CAVES_ALLOWED,
            MINIMAP_ALLOWED,
            WORLDMAP_ALLOWED,
            WAYPOINTS_ALLOWED,
            DEATH_WAYPOINT_ALLOWED,
            TELEPORT_COMMAND,
            WORLD_NAME_SUFFIX,
            SERVER_IDENTITY
    );

    private final Map<String, Boolean> booleanValues;
    private final boolean teleportCommandPresent;
    private final String teleportCommand;
    private final boolean worldNameSuffixPresent;
    private final String worldNameSuffix;
    private final boolean serverIdentityPresent;
    private final String serverIdentity;

    private VoxelmapServerSettings(
            Map<String, Boolean> booleanValues,
            boolean teleportCommandPresent,
            String teleportCommand,
            boolean worldNameSuffixPresent,
            String worldNameSuffix,
            boolean serverIdentityPresent,
            String serverIdentity
    ) {
        this.booleanValues = Collections.unmodifiableMap(new LinkedHashMap<>(booleanValues));
        this.teleportCommandPresent = teleportCommandPresent;
        this.teleportCommand = teleportCommand;
        this.worldNameSuffixPresent = worldNameSuffixPresent;
        this.worldNameSuffix = worldNameSuffix;
        this.serverIdentityPresent = serverIdentityPresent;
        this.serverIdentity = serverIdentity;
    }

    public static VoxelmapServerSettings defaults() {
        Map<String, Boolean> booleanValues = new LinkedHashMap<>();
        for (String key : BOOLEAN_KEYS) {
            booleanValues.put(key, true);
        }

        return new VoxelmapServerSettings(booleanValues, true, null, true, "", true, null);
    }

    public static VoxelmapServerSettings fromJson(JsonObject json, String context) {
        Map<String, Boolean> booleanValues = new LinkedHashMap<>();
        boolean teleportCommandPresent = false;
        String teleportCommand = null;
        boolean worldNameSuffixPresent = false;
        String worldNameSuffix = null;
        boolean serverIdentityPresent = false;
        String serverIdentity = null;

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (!KNOWN_KEYS.contains(key)) {
                throw new IllegalArgumentException("Unknown setting '" + key + "' in " + context);
            }

            if (BOOLEAN_KEYS.contains(key)) {
                booleanValues.put(key, readBoolean(value, key, context));
            } else if (TELEPORT_COMMAND.equals(key)) {
                teleportCommandPresent = true;
                teleportCommand = readNullableString(value, key, context);
            } else if (WORLD_NAME_SUFFIX.equals(key)) {
                worldNameSuffixPresent = true;
                worldNameSuffix = readString(value, key, context);
            } else if (SERVER_IDENTITY.equals(key)) {
                serverIdentityPresent = true;
                serverIdentity = readNullableString(value, key, context);
            }
        }

        return new VoxelmapServerSettings(
                booleanValues,
                teleportCommandPresent,
                teleportCommand,
                worldNameSuffixPresent,
                worldNameSuffix,
                serverIdentityPresent,
                serverIdentity
        );
    }

    public VoxelmapServerSettings merge(VoxelmapServerSettings override) {
        Map<String, Boolean> mergedBooleans = new LinkedHashMap<>(this.booleanValues);
        mergedBooleans.putAll(override.booleanValues);

        boolean mergedTeleportCommandPresent = this.teleportCommandPresent;
        String mergedTeleportCommand = this.teleportCommand;
        if (override.teleportCommandPresent) {
            mergedTeleportCommandPresent = true;
            mergedTeleportCommand = override.teleportCommand;
        }

        boolean mergedWorldNameSuffixPresent = this.worldNameSuffixPresent;
        String mergedWorldNameSuffix = this.worldNameSuffix;
        if (override.worldNameSuffixPresent) {
            mergedWorldNameSuffixPresent = true;
            mergedWorldNameSuffix = override.worldNameSuffix;
        }

        boolean mergedServerIdentityPresent = this.serverIdentityPresent;
        String mergedServerIdentity = this.serverIdentity;
        if (override.serverIdentityPresent) {
            mergedServerIdentityPresent = true;
            mergedServerIdentity = override.serverIdentity;
        }

        return new VoxelmapServerSettings(
                mergedBooleans,
                mergedTeleportCommandPresent,
                mergedTeleportCommand,
                mergedWorldNameSuffixPresent,
                mergedWorldNameSuffix,
                mergedServerIdentityPresent,
                mergedServerIdentity
        );
    }

    public JsonObject toConfigJson() {
        JsonObject json = new JsonObject();
        for (String key : BOOLEAN_KEYS) {
            if (booleanValues.containsKey(key)) {
                json.addProperty(key, booleanValues.get(key));
            }
        }

        if (teleportCommandPresent) {
            addNullableString(json, TELEPORT_COMMAND, teleportCommand);
        }

        if (worldNameSuffixPresent) {
            json.addProperty(WORLD_NAME_SUFFIX, worldNameSuffix);
        }

        if (serverIdentityPresent) {
            addNullableString(json, SERVER_IDENTITY, serverIdentity);
        }

        return json;
    }

    public JsonObject toClientSettingsJson(String worldId) {
        JsonObject json = new JsonObject();
        addNullableString(json, SERVER_IDENTITY, getServerIdentity());
        json.addProperty("worldName", worldId + getWorldNameSuffix());
        for (String key : BOOLEAN_KEYS) {
            json.addProperty(key, getRequiredBoolean(key));
        }

        addNullableString(json, TELEPORT_COMMAND, getTeleportCommand());
        return json;
    }

    private boolean getRequiredBoolean(String key) {
        Boolean value = booleanValues.get(key);
        if (value == null) {
            throw new IllegalStateException("Missing effective server setting '" + key + "'");
        }
        return value;
    }

    private String getTeleportCommand() {
        if (!teleportCommandPresent) {
            throw new IllegalStateException("Missing effective server setting '" + TELEPORT_COMMAND + "'");
        }
        return teleportCommand;
    }

    private String getServerIdentity() {
        if (!serverIdentityPresent) {
            throw new IllegalStateException("Missing effective server setting '" + SERVER_IDENTITY + "'");
        }
        return serverIdentity;
    }

    private String getWorldNameSuffix() {
        if (!worldNameSuffixPresent) {
            throw new IllegalStateException("Missing effective server setting '" + WORLD_NAME_SUFFIX + "'");
        }
        return worldNameSuffix;
    }

    private static boolean readBoolean(JsonElement value, String key, String context) {
        if (!value.isJsonPrimitive()) {
            throw new IllegalArgumentException("Setting '" + key + "' in " + context + " must be a boolean");
        }

        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (!primitive.isBoolean()) {
            throw new IllegalArgumentException("Setting '" + key + "' in " + context + " must be a boolean");
        }

        return primitive.getAsBoolean();
    }

    private static String readNullableString(JsonElement value, String key, String context) {
        if (value.isJsonNull()) {
            return null;
        }

        return readString(value, key, context);
    }

    private static String readString(JsonElement value, String key, String context) {
        if (!value.isJsonPrimitive()) {
            throw new IllegalArgumentException("Setting '" + key + "' in " + context + " must be a string");
        }

        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (!primitive.isString()) {
            throw new IllegalArgumentException("Setting '" + key + "' in " + context + " must be a string");
        }

        return primitive.getAsString();
    }

    private static void addNullableString(JsonObject json, String key, String value) {
        if (value == null) {
            json.add(key, JsonNull.INSTANCE);
        } else {
            json.addProperty(key, value);
        }
    }
}
