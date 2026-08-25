package com.mamiyaotaru.voxelmap;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDLScancode;

class MapSettingsManagerTest {
    @Test
    void migratesLegacyNegativeKeyboardBindingToUnknown() {
        InputConstants.Key legacyUnbound = InputConstants.Type.KEYBOARD.getOrCreate(-1);

        assertSame(InputConstants.UNKNOWN, MapSettingsManager.sanitizeKey(legacyUnbound));
    }

    @Test
    void rejectsKeyboardBindingOutsideSdlStateBuffer() {
        InputConstants.Key outOfRange = InputConstants.Type.KEYBOARD.getOrCreate(SDLScancode.SDL_SCANCODE_COUNT);

        assertSame(InputConstants.UNKNOWN, MapSettingsManager.sanitizeKey(outOfRange));
    }

    @Test
    void preservesValidKeyboardBinding() {
        InputConstants.Key z = InputConstants.getKey("key.keyboard.z");

        assertSame(z, MapSettingsManager.sanitizeKey(z));
    }
}
