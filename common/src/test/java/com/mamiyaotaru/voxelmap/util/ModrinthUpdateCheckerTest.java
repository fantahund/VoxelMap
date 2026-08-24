package com.mamiyaotaru.voxelmap.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class ModrinthUpdateCheckerTest {
    private static final String MINECRAFT_VERSION = "26.2";
    private static final String LOADER = "fabric";

    private final ModrinthUpdateChecker checker = new ModrinthUpdateChecker("voxelmap-updated", LOADER, MINECRAFT_VERSION);

    @Test
    void olderCompatibleVersionDoesNotTriggerNotification() {
        ModrinthUpdateChecker.UpdateResult result = checker.buildUpdateResult(
                "26.2-1.16.9",
                versions(version("26.2-1.16.8", MINECRAFT_VERSION, LOADER))
        );

        assertNotNull(result);
        assertEquals("26.2-1.16.8", result.latestVersion());
        assertTrue(result.updates().isEmpty());
        assertFalse(ModrinthUpdateChecker.shouldNotifyAboutUpdate("26.2-1.16.9", result));
    }

    @Test
    void equalCompatibleVersionDoesNotTriggerNotification() {
        ModrinthUpdateChecker.UpdateResult result = checker.buildUpdateResult(
                "26.2-1.16.9",
                versions(version("26.2-1.16.9", MINECRAFT_VERSION, LOADER))
        );

        assertNotNull(result);
        assertTrue(result.updates().isEmpty());
        assertFalse(ModrinthUpdateChecker.shouldNotifyAboutUpdate("26.2-1.16.9", result));
    }

    @Test
    void newerCompatibleVersionTriggersNotification() {
        ModrinthUpdateChecker.UpdateResult result = checker.buildUpdateResult(
                "26.2-1.16.9",
                versions(
                        version("26.2-1.16.8", MINECRAFT_VERSION, LOADER),
                        version("26.2-1.16.10", MINECRAFT_VERSION, LOADER)
                )
        );

        assertNotNull(result);
        assertEquals("26.2-1.16.10", result.latestVersion());
        assertEquals(1, result.updates().size());
        assertEquals("26.2-1.16.10", result.updates().getFirst().version());
        assertTrue(ModrinthUpdateChecker.shouldNotifyAboutUpdate("26.2-1.16.9", result));
    }

    @Test
    void versionsForAnotherMinecraftVersionOrLoaderAreIgnored() {
        ModrinthUpdateChecker.UpdateResult result = checker.buildUpdateResult(
                "26.2-1.16.9",
                versions(
                        version("26.3-1.17.0", "26.3", LOADER),
                        version("26.2-1.17.0", MINECRAFT_VERSION, "forge")
                )
        );

        assertNull(result);
    }

    @Test
    void versionSegmentsAreComparedNumerically() {
        assertTrue(ModrinthUpdateChecker.compareVersions("26.2-1.16.10", "26.2-1.16.9") > 0);
        assertTrue(ModrinthUpdateChecker.compareVersions("26.2-1.16.8", "26.2-1.16.9") < 0);
        assertEquals(0, ModrinthUpdateChecker.compareVersions("26.2-1.16.9", "26.2-1.16.9"));
    }

    private static JsonArray versions(JsonObject... versions) {
        JsonArray array = new JsonArray();
        for (JsonObject version : versions) {
            array.add(version);
        }
        return array;
    }

    private static JsonObject version(String versionNumber, String minecraftVersion, String loader) {
        JsonObject version = new JsonObject();
        version.addProperty("version_number", versionNumber);
        version.add("game_versions", strings(minecraftVersion));
        version.add("loaders", strings(loader));
        version.addProperty("changelog", "Changes");
        return version;
    }

    private static JsonArray strings(String value) {
        JsonArray array = new JsonArray();
        array.add(value);
        return array;
    }
}
