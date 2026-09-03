package com.mamiyaotaru.voxelmap.persistent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mamiyaotaru.voxelmap.util.ColorUtils;
import net.minecraft.util.ARGB;
import org.junit.jupiter.api.Test;

class PersistentMapOverviewLightingTest {
    @Test
    void waterLightIncludesMostlyVisibleSeafloorInsteadOfUsingSurfaceLightOnly() {
        int light = PersistentMap.composeApproximateLight(
                0x1BFFFFFF,
                0xF0,
                0xFFFFFFFF,
                0x30,
                0,
                0,
                0,
                0,
                64,
                48,
                -64,
                -64,
                -64,
                true,
                new PersistentMap.LightAccumulator());

        assertEquals(0x40, light);
    }

    @Test
    void opaqueTopLayerDeterminesApproximateLight() {
        int light = PersistentMap.composeApproximateLight(
                0xFFFFFFFF,
                0xF0,
                0,
                0,
                0xFFFFFFFF,
                0x21,
                0,
                0,
                64,
                -64,
                65,
                -64,
                -64,
                false,
                new PersistentMap.LightAccumulator());

        assertEquals(0x21, light);
    }

    @Test
    void mapsMixedBrightnessBackThroughTheActualNonlinearLightmap() {
        int[] lightmap = new int[256];
        for (int skyLight = 0; skyLight < 16; ++skyLight) {
            int brightness = skyLight == 4 ? 30 : skyLight == 5 ? 45 : 0;
            lightmap[7 | skyLight << 4] = 0xFF000000 | brightness << 16 | brightness << 8 | brightness;
        }

        int bestLight = PersistentMapOverviewCache.findBestLight(200, 200, 200, 35, 35, 35, 0x47, lightmap);

        assertEquals(0x57, bestLight);
    }

    @Test
    void fullRenderAndRawOverviewApplyAsymmetricLightmapChannelsIdentically() {
        int baseArgb = 0xFFC86432;
        int lightArgb = 0xFFC88040;
        int fullRenderArgb = ARGB.toABGR(ColorUtils.colorMultiplier(ARGB.toABGR(baseArgb), lightArgb));

        byte[] primaryPixels = new byte[PersistentMapOverviewCache.PRIMARY_COLOR_BYTES];
        primaryPixels[0] = (byte) 200;
        primaryPixels[1] = (byte) 100;
        primaryPixels[2] = (byte) 50;
        primaryPixels[3] = (byte) 255;
        byte[] secondaryPixels = new byte[PersistentMapOverviewCache.SECONDARY_COLOR_BYTES];
        byte[] primaryLights = new byte[PersistentMapOverviewCache.LIGHT_BYTES];
        byte[] secondaryLights = new byte[PersistentMapOverviewCache.LIGHT_BYTES];
        primaryLights[0] = 7;
        int[] lightmap = new int[256];
        lightmap[7] = lightArgb;
        byte[] overview = PersistentMapOverviewCache.applyLighting(
                new PersistentMapOverviewCache.OverviewData(
                        primaryPixels, secondaryPixels, primaryLights, secondaryLights),
                lightmap,
                true);

        assertEquals(0xFF323227, fullRenderArgb);
        assertEquals(fullRenderArgb >> 16 & 0xFF, Byte.toUnsignedInt(overview[0]));
        assertEquals(fullRenderArgb >> 8 & 0xFF, Byte.toUnsignedInt(overview[1]));
        assertEquals(fullRenderArgb & 0xFF, Byte.toUnsignedInt(overview[2]));
    }

    @Test
    void waterSurfaceAndSeafloorUseIndependentNonlinearLightsBeforeTheyAreAdded() {
        byte[] primaryPixels = new byte[PersistentMapOverviewCache.PRIMARY_COLOR_BYTES];
        byte[] secondaryPixels = new byte[PersistentMapOverviewCache.SECONDARY_COLOR_BYTES];
        byte[] primaryLights = new byte[PersistentMapOverviewCache.LIGHT_BYTES];
        byte[] secondaryLights = new byte[PersistentMapOverviewCache.LIGHT_BYTES];
        primaryPixels[0] = 100;
        primaryPixels[1] = 80;
        primaryPixels[2] = 60;
        primaryPixels[3] = (byte) 200;
        secondaryPixels[0] = 90;
        secondaryPixels[1] = 70;
        secondaryPixels[2] = 50;
        primaryLights[0] = 3;
        secondaryLights[0] = 9;
        int[] lightmap = new int[256];
        lightmap[3] = 0xFF804020;
        lightmap[9] = 0xFF204080;

        byte[] pixels = PersistentMapOverviewCache.applyLighting(
                new PersistentMapOverviewCache.OverviewData(
                        primaryPixels, secondaryPixels, primaryLights, secondaryLights),
                lightmap,
                true);

        assertEquals(57, Byte.toUnsignedInt(pixels[0]));
        assertEquals(37, Byte.toUnsignedInt(pixels[1]));
        assertEquals(36, Byte.toUnsignedInt(pixels[2]));
        assertEquals(200, Byte.toUnsignedInt(pixels[3]));
    }

    @Test
    void disabledDynamicLightingAddsBothUnlitComponentsWithSaturation() {
        byte[] primaryPixels = new byte[PersistentMapOverviewCache.PRIMARY_COLOR_BYTES];
        byte[] secondaryPixels = new byte[PersistentMapOverviewCache.SECONDARY_COLOR_BYTES];
        primaryPixels[0] = (byte) 220;
        primaryPixels[1] = 40;
        primaryPixels[2] = 30;
        primaryPixels[3] = (byte) 255;
        secondaryPixels[0] = 80;
        secondaryPixels[1] = 70;
        secondaryPixels[2] = 60;

        byte[] pixels = PersistentMapOverviewCache.applyLighting(
                new PersistentMapOverviewCache.OverviewData(
                        primaryPixels,
                        secondaryPixels,
                        new byte[PersistentMapOverviewCache.LIGHT_BYTES],
                        new byte[PersistentMapOverviewCache.LIGHT_BYTES]),
                new int[0],
                false);

        assertEquals(255, Byte.toUnsignedInt(pixels[0]));
        assertEquals(110, Byte.toUnsignedInt(pixels[1]));
        assertEquals(90, Byte.toUnsignedInt(pixels[2]));
        assertEquals(255, Byte.toUnsignedInt(pixels[3]));
    }

    @Test
    void relightThresholdUsesTheVisibleTwoComponentResult() {
        byte[] primaryPixels = new byte[PersistentMapOverviewCache.PRIMARY_COLOR_BYTES];
        byte[] secondaryPixels = new byte[PersistentMapOverviewCache.SECONDARY_COLOR_BYTES];
        byte[] primaryLights = new byte[PersistentMapOverviewCache.LIGHT_BYTES];
        byte[] secondaryLights = new byte[PersistentMapOverviewCache.LIGHT_BYTES];
        primaryPixels[0] = 1;
        secondaryPixels[0] = 1;
        primaryLights[0] = 1;
        secondaryLights[0] = 2;
        PersistentMapOverviewCache.OverviewData overview = new PersistentMapOverviewCache.OverviewData(
                primaryPixels, secondaryPixels, primaryLights, secondaryLights);
        int[] displayedLightmap = new int[256];
        int[] currentLightmap = new int[256];
        displayedLightmap[1] = 0xFF000000;
        displayedLightmap[2] = 0xFF000000;
        currentLightmap[1] = 0xFFFFFFFF;
        currentLightmap[2] = 0xFFFFFFFF;

        assertFalse(PersistentMapOverviewCache.lightingDifferenceExceedsThreshold(
                overview, displayedLightmap, currentLightmap));

        for (int pixel = 0; pixel < PersistentMapOverviewCache.MIN_SIGNIFICANT_PIXELS; ++pixel) {
            primaryPixels[pixel * 4] = (byte) 255;
            primaryLights[pixel] = 1;
        }
        assertTrue(PersistentMapOverviewCache.lightingDifferenceExceedsThreshold(
                overview, displayedLightmap, currentLightmap));
    }
}
