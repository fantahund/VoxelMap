package com.mamiyaotaru.voxelmap.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressionUtils {
    public static byte[] compress(byte[] dataToCompress) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setLevel(1);
        deflater.setInput(dataToCompress);
        deflater.finish();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(dataToCompress.length);
        byte[] buffer = new byte[1024];

        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }

        outputStream.close();
        deflater.end();
        byte[] output = outputStream.toByteArray();
        return output;
    }

    public static byte[] decompress(byte[] dataToDecompress) throws IOException, DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(dataToDecompress);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(dataToDecompress.length);
        byte[] buffer = new byte[1024];

        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }

        outputStream.close();
        inflater.end();
        byte[] output = outputStream.toByteArray();
        return output;
    }
}
