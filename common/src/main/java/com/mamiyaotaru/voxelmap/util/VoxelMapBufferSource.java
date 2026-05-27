package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class VoxelMapBufferSource {
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private final Map<RenderType, Batch> batches = new LinkedHashMap<>();
    private RenderType lastRenderType;

    public BufferBuilder getBuffer(RenderType renderType) {
        lastRenderType = renderType;
        return batches.computeIfAbsent(renderType, Batch::new).bufferBuilder;
    }

    public void endLastBatch() {
        if (lastRenderType != null) {
            endBatch(lastRenderType);
            lastRenderType = null;
        }
    }

    public void endBatch(RenderType renderType) {
        Batch batch = batches.remove(renderType);
        if (batch != null) {
            draw(renderType, batch);
        }
    }

    public void endBatch() {
        for (Map.Entry<RenderType, Batch> entry : batches.entrySet()) {
            draw(entry.getKey(), entry.getValue());
        }
        batches.clear();
        lastRenderType = null;
    }

    private static void draw(RenderType renderType, Batch batch) {
        try (Batch ignored = batch; MeshData meshData = batch.bufferBuilder.build()) {
            if (meshData == null) {
                return;
            }

            GpuBuffer vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap " + renderType + " Vertex Buffer", GpuBuffer.USAGE_VERTEX, meshData.vertexBuffer());
            GpuBuffer indexBuffer = null;
            boolean closeIndexBuffer = false;
            IndexType indexType;
            ByteBuffer customIndexBuffer = meshData.indexBuffer();
            if (customIndexBuffer == null) {
                RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().primitiveTopology());
                indexBuffer = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
                indexType = autoStorageIndexBuffer.type();
            } else {
                indexBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap " + renderType + " Index Buffer", GpuBuffer.USAGE_INDEX, customIndexBuffer);
                indexType = meshData.drawState().indexType();
                closeIndexBuffer = true;
            }

            try {
                renderType.prepare().drawFromBuffer(vertexBuffer, indexBuffer, indexType, 0, 0, meshData.drawState().indexCount());
            } finally {
                vertexBuffer.close();
                if (closeIndexBuffer) {
                    indexBuffer.close();
                }
            }
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Failed drawing VoxelMap batch for " + renderType, e);
        }
    }

    private static class Batch implements AutoCloseable {
        private final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(DEFAULT_BUFFER_SIZE);
        private final BufferBuilder bufferBuilder;

        private Batch(RenderType renderType) {
            this.bufferBuilder = new BufferBuilder(byteBufferBuilder, renderType.primitiveTopology(), renderType.format());
        }

        @Override
        public void close() {
            byteBufferBuilder.close();
        }
    }
}
