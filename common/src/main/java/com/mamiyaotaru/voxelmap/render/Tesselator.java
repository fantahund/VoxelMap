package com.mamiyaotaru.voxelmap.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

public class Tesselator {
    private static final int MAX_BYTES = 0xC0000;
    private final ByteBufferBuilder buffer;
    private static Tesselator instance;

    public static void init() {
        if (instance != null) {
            throw new IllegalStateException("Tesselator has already been initialized");
        } else {
            instance = new Tesselator();
        }
    }

    public static Tesselator getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Tesselator has not been initialized");
        } else {
            return instance;
        }
    }

    public Tesselator(int size) {
        buffer = new ByteBufferBuilder(size);
    }

    public Tesselator() {
        this(MAX_BYTES);
    }

    public BufferBuilder begin(PrimitiveTopology topology, VertexFormat format) {
        return new BufferBuilder(buffer, topology, format);
    }

    public void clear() {
        buffer.clear();
    }
}
