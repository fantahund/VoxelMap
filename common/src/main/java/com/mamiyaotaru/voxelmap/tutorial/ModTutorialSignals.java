package com.mamiyaotaru.voxelmap.tutorial;

public final class ModTutorialSignals {
    private static boolean menuOpened;

    private ModTutorialSignals() {
    }

    public static void menuOpened() {
        menuOpened = true;
    }

    public static boolean consumeMenuOpened() {
        boolean result = menuOpened;
        menuOpened = false;
        return result;
    }

    public static void reset() {
        menuOpened = false;
    }
}
