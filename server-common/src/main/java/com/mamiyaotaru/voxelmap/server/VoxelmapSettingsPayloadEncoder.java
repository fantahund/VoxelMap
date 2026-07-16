package com.mamiyaotaru.voxelmap.server;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class VoxelmapSettingsPayloadEncoder {
    private static final int MAX_STRING_LENGTH = 32767;

    private VoxelmapSettingsPayloadEncoder() {
    }

    public static byte[] encode(String settingsJson) {
        byte[] utf8 = settingsJson.getBytes(StandardCharsets.UTF_8);
        if (settingsJson.length() > MAX_STRING_LENGTH || utf8.length > MAX_STRING_LENGTH * 3) {
            throw new IllegalArgumentException("Encoded settings JSON is too large");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(1 + varIntSize(utf8.length) + utf8.length);
        out.write(0);
        writeVarInt(out, utf8.length);
        out.writeBytes(utf8);
        return out.toByteArray();
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & -128) != 0) {
            out.write(value & 127 | 128);
            value >>>= 7;
        }

        out.write(value);
    }

    private static int varIntSize(int value) {
        for (int bytes = 1; bytes < 5; bytes++) {
            if ((value & -1 << bytes * 7) == 0) {
                return bytes;
            }
        }

        return 5;
    }
}
