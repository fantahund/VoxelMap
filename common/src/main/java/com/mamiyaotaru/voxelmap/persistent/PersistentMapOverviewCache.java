package com.mamiyaotaru.voxelmap.persistent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

final class PersistentMapOverviewCache {
    static final int SIZE = 64;
    static final int PIXEL_COUNT = SIZE * SIZE;
    static final int COLOR_BYTES = PIXEL_COUNT * 4;
    static final int LIGHT_BYTES = PIXEL_COUNT;
    static final int RAW_BYTES = COLOR_BYTES + LIGHT_BYTES;

    // Small changes accumulate because each comparison uses the lightmap that was
    // used for the image which is actually on screen.
    static final int MEAN_LIGHT_CHANGE_THRESHOLD = 2;
    static final int LOCAL_LIGHT_CHANGE_THRESHOLD = 6;
    static final int MIN_SIGNIFICANT_PIXELS = 8;

    private static final int MAGIC_V2 = 0x564D4F32; // VMO2
    private static final int FORMAT_VERSION_V2 = 2;
    private static final int MAGIC_V1 = 0x564D4F31; // VMO1
    private static final int FORMAT_VERSION_V1 = 1;
    private static final int MAX_COMPRESSED_BYTES = RAW_BYTES + 1024;

    private PersistentMapOverviewCache() {}

    static Optional<OverviewData> read(File overviewFile, File sourceFile, long renderSignature) {
        Optional<byte[]> raw = readPayload(
                overviewFile, sourceFile, renderSignature, MAGIC_V2, FORMAT_VERSION_V2, RAW_BYTES, MAX_COMPRESSED_BYTES);
        if (raw.isEmpty()) {
            return Optional.empty();
        }

        byte[] payload = raw.get();
        return Optional.of(new OverviewData(
                Arrays.copyOfRange(payload, 0, COLOR_BYTES), Arrays.copyOfRange(payload, COLOR_BYTES, RAW_BYTES)));
    }

    static Optional<byte[]> readLegacy(File overviewFile, File sourceFile, long renderSignature) {
        return readPayload(
                overviewFile, sourceFile, renderSignature, MAGIC_V1, FORMAT_VERSION_V1, COLOR_BYTES, COLOR_BYTES + 1024);
    }

    static void write(File overviewFile, File sourceFile, long renderSignature, OverviewData overview) throws IOException {
        byte[] payload = new byte[RAW_BYTES];
        System.arraycopy(overview.basePixels(), 0, payload, 0, COLOR_BYTES);
        System.arraycopy(overview.lightValues(), 0, payload, COLOR_BYTES, LIGHT_BYTES);
        writePayload(overviewFile, sourceFile, renderSignature, payload);
    }

    static byte[] applyLighting(OverviewData overview, int[] lightmap, boolean dynamicLighting) {
        byte[] basePixels = overview.basePixels();
        byte[] result = Arrays.copyOf(basePixels, basePixels.length);
        if (!dynamicLighting) {
            return result;
        }
        if (lightmap.length != 256) {
            throw new IllegalArgumentException("Expected 256 lightmap colors, got " + lightmap.length);
        }

        byte[] lightValues = overview.lightValues();
        for (int pixel = 0, offset = 0; pixel < PIXEL_COUNT; ++pixel, offset += 4) {
            int lightColor = lightmap[Byte.toUnsignedInt(lightValues[pixel])];
            // PersistentMap composes ABGR layers with the ARGB lightmap before its
            // final ARGB conversion. Raw overview bytes are RGBA, so red and blue
            // intentionally use the opposite packed lightmap components here.
            result[offset] = multiply(result[offset], lightColor);
            result[offset + 1] = multiply(result[offset + 1], lightColor >> 8);
            result[offset + 2] = multiply(result[offset + 2], lightColor >> 16);
        }
        return result;
    }

    static boolean lightingDifferenceExceedsThreshold(OverviewData overview, int[] displayedLightmap, int[] currentLightmap) {
        if (displayedLightmap == null || displayedLightmap.length != 256 || currentLightmap.length != 256) {
            return true;
        }

        int[] histogram = overview.lightHistogram();
        long totalDifference = 0L;
        int significantPixels = 0;
        int totalPixels = 0;
        for (int light = 0; light < histogram.length; ++light) {
            int count = histogram[light];
            if (count == 0) {
                continue;
            }

            int displayed = displayedLightmap[light];
            int current = currentLightmap[light];
            int redDifference = Math.abs((displayed >> 16 & 0xFF) - (current >> 16 & 0xFF));
            int greenDifference = Math.abs((displayed >> 8 & 0xFF) - (current >> 8 & 0xFF));
            int blueDifference = Math.abs((displayed & 0xFF) - (current & 0xFF));
            totalDifference += (long) count * (redDifference + greenDifference + blueDifference);
            totalPixels += count;
            if (Math.max(redDifference, Math.max(greenDifference, blueDifference)) >= LOCAL_LIGHT_CHANGE_THRESHOLD) {
                significantPixels += count;
            }
        }

        return totalPixels > 0
                && (totalDifference >= (long) totalPixels * 3 * MEAN_LIGHT_CHANGE_THRESHOLD
                        || significantPixels >= MIN_SIGNIFICANT_PIXELS);
    }

    static int findBestLight(
            int baseRed,
            int baseGreen,
            int baseBlue,
            int litRed,
            int litGreen,
            int litBlue,
            int preferredLight,
            int[] lightmap) {
        int blockLight = preferredLight & 0xF;
        int preferredSkyLight = preferredLight >> 4 & 0xF;
        int bestLight = preferredLight;
        long bestError = Long.MAX_VALUE;
        int bestLevelDistance = Integer.MAX_VALUE;
        for (int skyLight = 0; skyLight < 16; ++skyLight) {
            int candidate = blockLight | skyLight << 4;
            int lightColor = lightmap[candidate];
            int predictedRed = baseRed * (lightColor & 0xFF) / 255;
            int predictedGreen = baseGreen * (lightColor >> 8 & 0xFF) / 255;
            int predictedBlue = baseBlue * (lightColor >> 16 & 0xFF) / 255;
            long redDifference = predictedRed - litRed;
            long greenDifference = predictedGreen - litGreen;
            long blueDifference = predictedBlue - litBlue;
            long error = redDifference * redDifference + greenDifference * greenDifference + blueDifference * blueDifference;
            int levelDistance = Math.abs(skyLight - preferredSkyLight);
            if (error < bestError || error == bestError && levelDistance < bestLevelDistance) {
                bestError = error;
                bestLevelDistance = levelDistance;
                bestLight = candidate;
            }
        }
        return bestLight;
    }

    private static byte multiply(byte component, int lightComponent) {
        return (byte) (Byte.toUnsignedInt(component) * (lightComponent & 0xFF) / 255);
    }

    private static Optional<byte[]> readPayload(
            File overviewFile,
            File sourceFile,
            long renderSignature,
            int expectedMagic,
            int expectedVersion,
            int expectedRawBytes,
            int maxCompressedBytes) {
        if (!overviewFile.isFile()) {
            return Optional.empty();
        }

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(overviewFile.toPath())))) {
            if (input.readInt() != expectedMagic || input.readInt() != expectedVersion || input.readInt() != SIZE) {
                return Optional.empty();
            }
            if (input.readLong() != renderSignature) {
                return Optional.empty();
            }

            long sourceLength = input.readLong();
            long sourceModified = input.readLong();
            if (sourceLength != sourceLength(sourceFile) || sourceModified != sourceModified(sourceFile)) {
                return Optional.empty();
            }

            int rawLength = input.readInt();
            int compressedLength = input.readInt();
            long expectedCrc = Integer.toUnsignedLong(input.readInt());
            if (rawLength != expectedRawBytes || compressedLength <= 0 || compressedLength > maxCompressedBytes) {
                return Optional.empty();
            }

            byte[] compressed = input.readNBytes(compressedLength);
            if (compressed.length != compressedLength || input.read() != -1) {
                return Optional.empty();
            }
            byte[] payload = decompress(compressed, expectedRawBytes);
            if (crc32(payload) != expectedCrc) {
                return Optional.empty();
            }
            return Optional.of(payload);
        } catch (EOFException | DataFormatException ignored) {
            return Optional.empty();
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static void writePayload(File overviewFile, File sourceFile, long renderSignature, byte[] payload) throws IOException {
        byte[] compressed = compress(payload);
        Path target = overviewFile.toPath();
        Path directory = target.getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, overviewFile.getName() + ".", ".tmp");
        boolean moved = false;
        try {
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                output.writeInt(MAGIC_V2);
                output.writeInt(FORMAT_VERSION_V2);
                output.writeInt(SIZE);
                output.writeLong(renderSignature);
                output.writeLong(sourceLength(sourceFile));
                output.writeLong(sourceModified(sourceFile));
                output.writeInt(payload.length);
                output.writeInt(compressed.length);
                output.writeInt((int) crc32(payload));
                output.write(compressed);
            }

            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static long sourceLength(File sourceFile) {
        return sourceFile.isFile() ? sourceFile.length() : -1L;
    }

    private static long sourceModified(File sourceFile) {
        return sourceFile.isFile() ? sourceFile.lastModified() : -1L;
    }

    private static long crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    private static byte[] compress(byte[] payload) {
        Deflater deflater = new Deflater(1);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(payload.length)) {
            deflater.setInput(payload);
            deflater.finish();
            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int written = deflater.deflate(buffer);
                if (written == 0) {
                    throw new IllegalStateException("Overview compressor made no progress");
                }
                output.write(buffer, 0, written);
            }
            return output.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        } finally {
            deflater.close();
        }
    }

    private static byte[] decompress(byte[] compressed, int rawBytes) throws DataFormatException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            byte[] payload = new byte[rawBytes];
            int offset = 0;
            while (!inflater.finished()) {
                if (offset == payload.length) {
                    throw new DataFormatException("Overview expands beyond its declared size");
                }
                int written = inflater.inflate(payload, offset, payload.length - offset);
                if (written == 0) {
                    throw new DataFormatException("Truncated or dictionary-compressed overview");
                }
                offset += written;
            }
            if (offset != payload.length || inflater.getRemaining() != 0) {
                throw new DataFormatException("Overview has an invalid decompressed size");
            }
            return payload;
        } finally {
            inflater.close();
        }
    }

    static final class OverviewData {
        private final byte[] basePixels;
        private final byte[] lightValues;
        private final int[] lightHistogram;

        OverviewData(byte[] basePixels, byte[] lightValues) {
            if (basePixels.length != COLOR_BYTES || lightValues.length != LIGHT_BYTES) {
                throw new IllegalArgumentException("Invalid overview data size");
            }
            this.basePixels = basePixels;
            this.lightValues = lightValues;
            this.lightHistogram = new int[256];
            for (int pixel = 0; pixel < lightValues.length; ++pixel) {
                int offset = pixel * 4;
                if ((basePixels[offset] | basePixels[offset + 1] | basePixels[offset + 2]) != 0) {
                    ++this.lightHistogram[Byte.toUnsignedInt(lightValues[pixel])];
                }
            }
        }

        byte[] basePixels() {
            return this.basePixels;
        }

        byte[] lightValues() {
            return this.lightValues;
        }

        int[] lightHistogram() {
            return this.lightHistogram;
        }

        OverviewData copy() {
            return new OverviewData(Arrays.copyOf(this.basePixels, this.basePixels.length), Arrays.copyOf(this.lightValues, this.lightValues.length));
        }
    }
}
