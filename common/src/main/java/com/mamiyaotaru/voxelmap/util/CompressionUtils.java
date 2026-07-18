package com.mamiyaotaru.voxelmap.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class CompressionUtils {
    private CompressionUtils() {
    }

    public static byte[] compress(byte[] dataToCompress) {
        Deflater deflater = new Deflater();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(dataToCompress.length)) {
            deflater.setLevel(1);
            deflater.setInput(dataToCompress);
            deflater.finish();
            byte[] buffer = new byte[1024];

            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("IOException should not happen for ByteArrayOutputStream", e);
        } finally {
            deflater.end();
        }
    }

    public static byte[] decompress(byte[] dataToDecompress) throws DataFormatException {
        Inflater inflater = new Inflater();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(dataToDecompress.length)) {
            inflater.setInput(dataToDecompress);
            byte[] buffer = new byte[1024];

            while (!(inflater.finished())) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("IOException should not happen for ByteArrayOutputStream", e);
        } finally {
            inflater.end();
        }
    }
}
