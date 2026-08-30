package com.mamiyaotaru.voxelmap.persistent;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
