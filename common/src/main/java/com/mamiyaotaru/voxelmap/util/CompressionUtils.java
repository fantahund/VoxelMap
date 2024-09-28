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
        deflater.setLevel(1);
        deflater.setInput(dataToCompress);
        deflater.finish();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(dataToCompress.length)) {
            byte[] buffer = new byte[1024];

            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            deflater.end();

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("IOException should not happen for ByteArrayOutputStream", e);
        }
    }

    public static byte[] decompress(byte[] dataToDecompress) throws DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(dataToDecompress);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(dataToDecompress.length)) {
            byte[] buffer = new byte[1024];

            while (!(inflater.finished())) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            inflater.end();

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("IOException should not happen for ByteArrayOutputStream", e);
        }
    }
}
