package com.mamiyaotaru.voxelmap.gui.settings;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettingsTranslationsTest {
    private static final List<String> CATEGORIES = List.of("general", "minimap", "worldmap", "waypoints", "radar", "controls", "advanced");
    private static final List<String> OPTION_IDS = List.of(
            "minimap.visible", "minimap.location", "minimap.size", "minimap.shape", "minimap.orientation", "minimap.coordinates",
            "minimap.biome", "minimap.caves", "minimap.chunkGrid", "minimap.slimeChunks", "minimap.worldBorder",
            "minimap.effects", "minimap.scoreboard", "waypoints.visible", "waypoints.style", "waypoints.distance", "waypoints.scale",
            "waypoints.names", "waypoints.namePosition", "waypoints.distances", "waypoints.distancePosition", "waypoints.units",
            "waypoints.deathpoints", "waypoints.deathpointRetention", "radar.visible", "radar.mode", "radar.elevation", "radar.invisible",
            "radar.players", "radar.playerNames", "radar.playerHelmets", "radar.sneaking", "radar.facing", "radar.hostiles", "radar.neutrals",
            "radar.mobNames", "radar.mobHelmets", "radar.fullNames", "radar.individualEntities", "radar.filtering", "radar.outlines", "worldmap.coordinates",
            "worldmap.waypoints", "worldmap.names", "worldmap.distant", "worldmap.nearestZoom", "worldmap.farthestZoom", "worldmap.cache",
            "advanced.lighting", "advanced.terrain", "advanced.water", "advanced.blocks", "advanced.biomes", "advanced.biomeOverlay",
            "advanced.filtering", "advanced.seed", "advanced.teleport", "advanced.compatibilityRenderer", "advanced.colorPicker", "advanced.updates");

    @Test
    void englishAndGermanContainEverySettingsCategoryAndTooltip() {
        for (String language : List.of("en_us", "de_de")) {
            JsonObject translations = load(language);
            for (String category : CATEGORIES)
                assertTrue(translations.has("options.voxelmap.category." + category), language + ": " + category);
            for (String option : OPTION_IDS)
                assertTrue(translations.has("options.voxelmap." + option + ".tooltip"), language + ": " + option);
        }
    }

    private static JsonObject load(String language) {
        String path = "/assets/voxelmap/lang/" + language + ".json";
        return JsonParser.parseReader(new InputStreamReader(SettingsTranslationsTest.class.getResourceAsStream(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
