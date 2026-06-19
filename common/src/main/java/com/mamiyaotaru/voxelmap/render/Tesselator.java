package com.mamiyaotaru.voxelmap.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;

public final class Tesselator {
    private static Tesselator instance;
    private final ByteBufferBuilder buffer;

    public Tesselator() {
        this(16384);
    }

    public Tesselator(int bufSize) {
        buffer = new ByteBufferBuilder(bufSize);
        instance = this;
    }

    public static Tesselator getInstance() {
        if (instance == null) {
            new Tesselator();
        }
        return instance;
    }

    public void clear() {
        buffer.clear();
    }

    public BufferBuilder begin(RenderPipeline pipeline) {
        return new BufferBuilder(buffer, pipeline.getPrimitiveTopology(), pipeline.getVertexFormatBinding(0));
    }
}
