package com.mamiyaotaru.voxelmap.persistent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import org.junit.jupiter.api.Test;

class DirtyPixelMaskTest {
    @Test
    void interiorChunkIncludesOnePixelBorder() {
        BitSet chunks = chunks(5, 5);
        BitSet pixels = DirtyPixelMask.fromChunks(chunks);

        assertEquals(18 * 18, pixels.cardinality());
        assertTrue(pixels.get(pixel(79, 79)));
        assertTrue(pixels.get(pixel(96, 96)));
        assertFalse(pixels.get(pixel(78, 79)));
    }

    @Test
    void borderIsClippedAtRegionEdge() {
        BitSet pixels = DirtyPixelMask.fromChunks(chunks(0, 0));

        assertEquals(17 * 17, pixels.cardinality());
        assertTrue(pixels.get(pixel(0, 0)));
        assertTrue(pixels.get(pixel(16, 16)));
        assertFalse(pixels.get(pixel(17, 16)));
    }

    @Test
    void adjacentChunksMergeWithoutDuplicatePixels() {
        BitSet chunks = chunks(5, 5);
        chunks.set(5 * 16 + 6);

        assertEquals(34 * 18, DirtyPixelMask.fromChunks(chunks).cardinality());
    }

    @Test
    void allChunksCoverTheWholeRegion() {
        BitSet chunks = new BitSet(256);
        chunks.set(0, 256);

        assertEquals(256 * 256, DirtyPixelMask.fromChunks(chunks).cardinality());
    }

    @Test
    void partialRenderingMatchesFullRenderingInsideTheDirtyAreaOnly() {
        BitSet dirtyPixels = DirtyPixelMask.fromChunks(chunks(8, 8));
        int[] previous = new int[256 * 256];
        int[] partial = new int[previous.length];
        int[] full = new int[previous.length];
        for (int index = 0; index < previous.length; ++index) {
            previous[index] = index * 17;
            partial[index] = previous[index];
            full[index] = renderPixel(index);
        }
        for (int index = dirtyPixels.nextSetBit(0); index >= 0; index = dirtyPixels.nextSetBit(index + 1)) {
            partial[index] = renderPixel(index);
        }

        for (int index = 0; index < partial.length; ++index) {
            assertEquals(dirtyPixels.get(index) ? full[index] : previous[index], partial[index]);
        }
    }

    private static BitSet chunks(int chunkX, int chunkZ) {
        BitSet chunks = new BitSet(256);
        chunks.set(chunkZ * 16 + chunkX);
        return chunks;
    }

    private static int pixel(int x, int z) {
        return z * 256 + x;
    }

    private static int renderPixel(int index) {
        int x = index % 256;
        int z = index / 256;
        return x * 31 + z * 101;
    }
}
