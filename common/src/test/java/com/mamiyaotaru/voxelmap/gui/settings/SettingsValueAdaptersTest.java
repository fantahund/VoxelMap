package com.mamiyaotaru.voxelmap.gui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SettingsValueAdaptersTest {
    @Test
    void waypointStyleRoundTripsExistingCombinations() {
        for (int style = 1; style <= 3; style++) {
            SettingsValueAdapters.WaypointStyle flags = SettingsValueAdapters.waypointStyle(style);
            assertEquals(style, SettingsValueAdapters.waypointStyle(flags.beacons(), flags.signs()));
        }
        assertFalse(SettingsValueAdapters.waypointsVisible(false, false));
        assertTrue(SettingsValueAdapters.waypointsVisible(true, false));
    }

    @Test
    void optionalLegacyModesUseOneAsTheirEnabledDefault() {
        assertEquals(0, SettingsValueAdapters.optionalMode(false, 2));
        assertEquals(1, SettingsValueAdapters.optionalMode(true, 0));
        assertEquals(2, SettingsValueAdapters.optionalMode(true, 2));
    }
}
