package com.mamiyaotaru.voxelmap.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class VoxelmapServerConfigTest {
    private static final Gson GSON = new Gson();

    @Test
    void defaultConfigCreatesCompleteClientJson() {
        JsonObject settings = VoxelmapServerConfig.defaults().createClientSettingsJson("minecraft:overworld");

        assertEquals("minecraft:overworld", settings.get("worldName").getAsString());
        assertTrue(settings.get("radarAllowed").getAsBoolean());
        assertTrue(settings.get("radarMobsAllowed").getAsBoolean());
        assertTrue(settings.get("radarPlayersAllowed").getAsBoolean());
        assertTrue(settings.get("cavesAllowed").getAsBoolean());
        assertTrue(settings.get("minimapAllowed").getAsBoolean());
        assertTrue(settings.get("worldmapAllowed").getAsBoolean());
        assertTrue(settings.get("waypointsAllowed").getAsBoolean());
        assertTrue(settings.get("deathWaypointAllowed").getAsBoolean());
        assertTrue(settings.has("teleportCommand"));
        assertTrue(settings.get("teleportCommand").isJsonNull());
        assertFalse(settings.has("worldNameSuffix"));
    }

    @Test
    void overrideMergesOnlyConfiguredValues() {
        VoxelmapServerConfig config = parseConfig("""
                {
                  "defaultConfig": {
                    "radarAllowed": true,
                    "radarMobsAllowed": true,
                    "radarPlayersAllowed": true,
                    "cavesAllowed": true,
                    "minimapAllowed": true,
                    "worldmapAllowed": true,
                    "waypointsAllowed": true,
                    "deathWaypointAllowed": true,
                    "teleportCommand": null,
                    "worldNameSuffix": ""
                  },
                  "worldOverrides": [
                    {
                      "worlds": ["minecraft:overworld"],
                      "settings": {
                        "radarAllowed": false,
                        "worldNameSuffix": "_custom"
                      }
                    }
                  ]
                }
                """);

        JsonObject settings = config.createClientSettingsJson("minecraft:overworld");

        assertEquals("minecraft:overworld_custom", settings.get("worldName").getAsString());
        assertFalse(settings.get("radarAllowed").getAsBoolean());
        assertTrue(settings.get("radarMobsAllowed").getAsBoolean());
        assertTrue(settings.get("teleportCommand").isJsonNull());
    }

    @Test
    void duplicateWorldOverridesFail() {
        JsonObject root = GSON.fromJson("""
                {
                  "defaultConfig": {},
                  "worldOverrides": [
                    {
                      "worlds": ["minecraft:overworld"],
                      "settings": {
                        "radarAllowed": false
                      }
                    },
                    {
                      "worlds": ["minecraft:overworld"],
                      "settings": {
                        "cavesAllowed": false
                      }
                    }
                  ]
                }
                """, JsonObject.class);

        assertThrows(IllegalArgumentException.class, () -> VoxelmapServerConfig.fromJson(root));
    }

    @Test
    void payloadEncoderWritesHeaderVarIntAndUtf8() {
        byte[] payload = VoxelmapSettingsPayloadEncoder.encode("{\"worldName\":\"minecraft:overworld\"}");
        byte[] jsonBytes = "{\"worldName\":\"minecraft:overworld\"}".getBytes(StandardCharsets.UTF_8);
        byte[] expected = new byte[2 + jsonBytes.length];
        expected[0] = 0;
        expected[1] = (byte) jsonBytes.length;
        System.arraycopy(jsonBytes, 0, expected, 2, jsonBytes.length);

        assertArrayEquals(expected, payload);
    }

    private static VoxelmapServerConfig parseConfig(String json) {
        return VoxelmapServerConfig.fromJson(GSON.fromJson(json, JsonObject.class));
    }
}
