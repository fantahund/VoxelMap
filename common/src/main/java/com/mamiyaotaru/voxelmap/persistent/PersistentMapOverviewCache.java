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
    static final int PRIMARY_COLOR_BYTES = PIXEL_COUNT * 4;
    static final int SECONDARY_COLOR_BYTES = PIXEL_COUNT * 3;
    static final int LIGHT_BYTES = PIXEL_COUNT;
    static final int RAW_BYTES = PRIMARY_COLOR_BYTES + SECONDARY_COLOR_BYTES + LIGHT_BYTES * 2;

    // Small changes accumulate because each comparison uses the lightmap that was
    // used for the image which is actually on screen.
    static final int MEAN_LIGHT_CHANGE_THRESHOLD = 2;
    static final int LOCAL_LIGHT_CHANGE_THRESHOLD = 6;
    static final int MIN_SIGNIFICANT_PIXELS = 8;

    private static final int MAGIC = 0x564D4F33; // VMO3
    private static final int FORMAT_VERSION = 3;
    private static final int MAX_COMPRESSED_BYTES = RAW_BYTES + 1024;

    private PersistentMapOverviewCache() {}

    static Optional<OverviewData> read(File overviewFile, File sourceFile, long renderSignature) {
        Optional<byte[]> raw = readPayload(overviewFile, sourceFile, renderSignature);
        if (raw.isEmpty()) {
            return Optional.empty();
        }

        byte[] payload = raw.get();
        int secondaryOffset = PRIMARY_COLOR_BYTES;
        int primaryLightOffset = secondaryOffset + SECONDARY_COLOR_BYTES;
        int secondaryLightOffset = primaryLightOffset + LIGHT_BYTES;
        return Optional.of(new OverviewData(
                Arrays.copyOfRange(payload, 0, PRIMARY_COLOR_BYTES),
                Arrays.copyOfRange(payload, secondaryOffset, primaryLightOffset),
                Arrays.copyOfRange(payload, primaryLightOffset, secondaryLightOffset),
                Arrays.copyOfRange(payload, secondaryLightOffset, RAW_BYTES)));
    }

    static void write(File overviewFile, File sourceFile, long renderSignature, OverviewData overview) throws IOException {
        byte[] payload = new byte[RAW_BYTES];
        int secondaryOffset = PRIMARY_COLOR_BYTES;
        int primaryLightOffset = secondaryOffset + SECONDARY_COLOR_BYTES;
        int secondaryLightOffset = primaryLightOffset + LIGHT_BYTES;
        System.arraycopy(overview.primaryPixels(), 0, payload, 0, PRIMARY_COLOR_BYTES);
        System.arraycopy(overview.secondaryPixels(), 0, payload, secondaryOffset, SECONDARY_COLOR_BYTES);
        System.arraycopy(overview.primaryLightValues(), 0, payload, primaryLightOffset, LIGHT_BYTES);
        System.arraycopy(overview.secondaryLightValues(), 0, payload, secondaryLightOffset, LIGHT_BYTES);
        writePayload(overviewFile, sourceFile, renderSignature, payload);
    }

    static byte[] applyLighting(OverviewData overview, int[] lightmap, boolean dynamicLighting) {
        if (dynamicLighting && lightmap.length != 256) {
            throw new IllegalArgumentException("Expected 256 lightmap colors, got " + lightmap.length);
        }

        byte[] primaryPixels = overview.primaryPixels();
        byte[] secondaryPixels = overview.secondaryPixels();
        byte[] primaryLights = overview.primaryLightValues();
        byte[] secondaryLights = overview.secondaryLightValues();
        byte[] result = new byte[PRIMARY_COLOR_BYTES];
        for (int pixel = 0, primaryOffset = 0, secondaryOffset = 0;
                pixel < PIXEL_COUNT;
                ++pixel, primaryOffset += 4, secondaryOffset += 3) {
            int primaryLight = dynamicLighting ? lightmap[Byte.toUnsignedInt(primaryLights[pixel])] : -1;
            int secondaryLight = dynamicLighting ? lightmap[Byte.toUnsignedInt(secondaryLights[pixel])] : -1;
            // PersistentMap composes ABGR layers with the ARGB lightmap before its
            // final ARGB conversion. Raw overview bytes are RGBA, so red and blue
            // intentionally use the opposite packed lightmap components here.
            result[primaryOffset] = (byte) saturatedComponent(
                    primaryPixels[primaryOffset], primaryLight, secondaryPixels[secondaryOffset], secondaryLight, dynamicLighting, 0);
            result[primaryOffset + 1] = (byte) saturatedComponent(
                    primaryPixels[primaryOffset + 1], primaryLight, secondaryPixels[secondaryOffset + 1], secondaryLight, dynamicLighting, 8);
            result[primaryOffset + 2] = (byte) saturatedComponent(
                    primaryPixels[primaryOffset + 2], primaryLight, secondaryPixels[secondaryOffset + 2], secondaryLight, dynamicLighting, 16);
            result[primaryOffset + 3] = primaryPixels[primaryOffset + 3];
        }
        return result;
    }

    static boolean lightingDifferenceExceedsThreshold(OverviewData overview, int[] displayedLightmap, int[] currentLightmap) {
        if (displayedLightmap == null || displayedLightmap.length != 256 || currentLightmap.length != 256) {
            return true;
        }

        byte[] primaryPixels = overview.primaryPixels();
        byte[] secondaryPixels = overview.secondaryPixels();
        byte[] primaryLights = overview.primaryLightValues();
        byte[] secondaryLights = overview.secondaryLightValues();
        long totalDifference = 0L;
        int significantPixels = 0;
        int totalPixels = 0;
        for (int pixel = 0, primaryOffset = 0, secondaryOffset = 0;
                pixel < PIXEL_COUNT;
                ++pixel, primaryOffset += 4, secondaryOffset += 3) {
            if ((primaryPixels[primaryOffset]
                            | primaryPixels[primaryOffset + 1]
                            | primaryPixels[primaryOffset + 2]
                            | secondaryPixels[secondaryOffset]
                            | secondaryPixels[secondaryOffset + 1]
                            | secondaryPixels[secondaryOffset + 2])
                    == 0) {
                continue;
            }

            int displayedPrimaryLight = displayedLightmap[Byte.toUnsignedInt(primaryLights[pixel])];
            int displayedSecondaryLight = displayedLightmap[Byte.toUnsignedInt(secondaryLights[pixel])];
            int currentPrimaryLight = currentLightmap[Byte.toUnsignedInt(primaryLights[pixel])];
            int currentSecondaryLight = currentLightmap[Byte.toUnsignedInt(secondaryLights[pixel])];
            int redDifference = componentDifference(
                    primaryPixels[primaryOffset], displayedPrimaryLight, currentPrimaryLight,
                    secondaryPixels[secondaryOffset], displayedSecondaryLight, currentSecondaryLight, 0);
            int greenDifference = componentDifference(
                    primaryPixels[primaryOffset + 1], displayedPrimaryLight, currentPrimaryLight,
                    secondaryPixels[secondaryOffset + 1], displayedSecondaryLight, currentSecondaryLight, 8);
            int blueDifference = componentDifference(
                    primaryPixels[primaryOffset + 2], displayedPrimaryLight, currentPrimaryLight,
                    secondaryPixels[secondaryOffset + 2], displayedSecondaryLight, currentSecondaryLight, 16);
            totalDifference += redDifference + greenDifference + blueDifference;
            ++totalPixels;
            if (Math.max(redDifference, Math.max(greenDifference, blueDifference)) >= LOCAL_LIGHT_CHANGE_THRESHOLD) {
                ++significantPixels;
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

    private static int saturatedComponent(
            byte primary, int primaryLight, byte secondary, int secondaryLight, boolean dynamicLighting, int lightShift) {
        int primaryValue = Byte.toUnsignedInt(primary);
        int secondaryValue = Byte.toUnsignedInt(secondary);
        if (dynamicLighting) {
            primaryValue = primaryValue * (primaryLight >> lightShift & 0xFF) / 255;
            secondaryValue = secondaryValue * (secondaryLight >> lightShift & 0xFF) / 255;
        }
        return Math.min(255, primaryValue + secondaryValue);
    }

    private static int componentDifference(
            byte primary,
            int displayedPrimaryLight,
            int currentPrimaryLight,
            byte secondary,
            int displayedSecondaryLight,
            int currentSecondaryLight,
            int lightShift) {
        int displayed = saturatedComponent(primary, displayedPrimaryLight, secondary, displayedSecondaryLight, true, lightShift);
        int current = saturatedComponent(primary, currentPrimaryLight, secondary, currentSecondaryLight, true, lightShift);
        return Math.abs(displayed - current);
    }

    private static Optional<byte[]> readPayload(File overviewFile, File sourceFile, long renderSignature) {
        if (!overviewFile.isFile()) {
            return Optional.empty();
        }

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(overviewFile.toPath())))) {
            if (input.readInt() != MAGIC || input.readInt() != FORMAT_VERSION || input.readInt() != SIZE) {
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
            if (rawLength != RAW_BYTES || compressedLength <= 0 || compressedLength > MAX_COMPRESSED_BYTES) {
                return Optional.empty();
            }

            byte[] compressed = input.readNBytes(compressedLength);
            if (compressed.length != compressedLength || input.read() != -1) {
                return Optional.empty();
            }
            byte[] payload = decompress(compressed, RAW_BYTES);
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
                output.writeInt(MAGIC);
                output.writeInt(FORMAT_VERSION);
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
        private final byte[] primaryPixels;
        private final byte[] secondaryPixels;
        private final byte[] primaryLightValues;
        private final byte[] secondaryLightValues;

        OverviewData(byte[] primaryPixels, byte[] secondaryPixels, byte[] primaryLightValues, byte[] secondaryLightValues) {
            if (primaryPixels.length != PRIMARY_COLOR_BYTES
                    || secondaryPixels.length != SECONDARY_COLOR_BYTES
                    || primaryLightValues.length != LIGHT_BYTES
                    || secondaryLightValues.length != LIGHT_BYTES) {
                throw new IllegalArgumentException("Invalid overview data size");
            }
            this.primaryPixels = primaryPixels;
            this.secondaryPixels = secondaryPixels;
            this.primaryLightValues = primaryLightValues;
            this.secondaryLightValues = secondaryLightValues;
        }

        byte[] primaryPixels() {
            return this.primaryPixels;
        }

        byte[] secondaryPixels() {
            return this.secondaryPixels;
        }

        byte[] primaryLightValues() {
            return this.primaryLightValues;
        }

        byte[] secondaryLightValues() {
            return this.secondaryLightValues;
        }

        OverviewData copy() {
            return new OverviewData(
                    Arrays.copyOf(this.primaryPixels, this.primaryPixels.length),
                    Arrays.copyOf(this.secondaryPixels, this.secondaryPixels.length),
                    Arrays.copyOf(this.primaryLightValues, this.primaryLightValues.length),
                    Arrays.copyOf(this.secondaryLightValues, this.secondaryLightValues.length));
        }
    }
}
