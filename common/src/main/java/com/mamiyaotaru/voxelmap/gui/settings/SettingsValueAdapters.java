package com.mamiyaotaru.voxelmap.gui.settings;

public final class SettingsValueAdapters {
    private SettingsValueAdapters() {
    }

    public record WaypointStyle(boolean beacons, boolean signs) {}

    public static boolean waypointsVisible(boolean beacons, boolean signs) {
        return beacons || signs;
    }

    public static WaypointStyle waypointVisibility(boolean visible) {
        return visible ? new WaypointStyle(false, true) : new WaypointStyle(false, false);
    }

    public static int waypointStyle(boolean beacons, boolean signs) {
        if (beacons && signs)
            return 3;
        if (beacons)
            return 1;
        return 2;
    }

    public static WaypointStyle waypointStyle(int style) {
        return new WaypointStyle(style == 1 || style == 3, style == 2 || style == 3);
    }

    public static int optionalMode(boolean enabled, int currentMode) {
        if (!enabled)
            return 0;
        return currentMode == 0 ? 1 : currentMode;
    }
}
