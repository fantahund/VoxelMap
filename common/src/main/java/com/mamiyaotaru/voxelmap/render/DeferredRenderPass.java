package com.mamiyaotaru.voxelmap.render;

import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class DeferredRenderPass implements AutoCloseable {
    private final String passName;
    private final GpuTextureView colorTexture;
    private final GpuTextureView depthTexture;
    private RenderPipeline pipeline;
    private final HashMap<String, GpuBufferSlice> uniformsMap = new HashMap<>(8);
    private final HashMap<String, TextureBinding> texturesMap = new HashMap<>(8);
    private final Matrix4f matrixCache = new Matrix4f();
    private BufferBuilder bufferBuilder;
    private final GpuTextureView lastOutputColorTexture;
    private final GpuTextureView lastOutputDepthTexture;

    public DeferredRenderPass(String passName, GpuTextureView colorTexture, OptionalInt colorClear, GpuTextureView depthTexture, OptionalDouble depthClear) {
        this.passName = passName;
        this.colorTexture = colorTexture;
        this.depthTexture = depthTexture;

        // Bind output textures for BufferSource draws
        lastOutputColorTexture = RenderSystem.outputColorTextureOverride;
        lastOutputDepthTexture = RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride = colorTexture;
        RenderSystem.outputDepthTextureOverride = depthTexture;

        // Clear color and depth textures
        if (colorTexture != null && colorClear.isPresent()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(colorTexture.texture(), colorClear.getAsInt());
        }
        if (depthTexture != null && depthClear.isPresent()) {
            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(depthTexture.texture(), depthClear.getAsDouble());
        }
    }

    public void setPipeline(RenderPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void setUniform(String target, GpuBuffer uniform) {
        setUniform(target, uniform.slice());
    }

    public void setUniform(String target, GpuBufferSlice uniform) {
        uniformsMap.put(target, uniform);
    }

    public void bindTexture(String target, Identifier texture) {
        bindTexture(target, Minecraft.getInstance().getTextureManager().getTexture(texture));
    }

    public void bindTexture(String target, Identifier texture, GpuSampler sampler) {
        bindTexture(target, Minecraft.getInstance().getTextureManager().getTexture(texture), sampler);
    }

    public void bindTexture(String target, AbstractTexture texture) {
        bindTexture(target, texture.getTextureView(), texture.getSampler());
    }

    public void bindTexture(String target, AbstractTexture texture, GpuSampler sampler) {
        bindTexture(target, texture.getTextureView(), sampler);
    }

    public void bindTexture(String target, GpuTextureView texture, GpuSampler sampler) {
        texturesMap.put(target, new TextureBinding(texture, sampler));
    }

    public void drawSpriteRect(Matrix4f matrix, Sprite sprite, float x, float y, float z, float width, float height, int color) {
        drawTexturedModalRect(matrix, x, y, z, width, height, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), color);
    }

    public void drawBlitRect(Matrix4f matrix, float x, float y, float z, float width, float height, int color) {
        float v0 = RenderUtils.hasFlippedTexture() ? 1.0F : 0.0F;
        float v1 = RenderUtils.hasFlippedTexture() ? 0.0F : 1.0F;
        drawTexturedModalRect(matrix, x, y, z, width, height, 0.0F, 1.0F, v0, v1, color);
    }

    public void drawTexturedModalRect(Matrix4f matrix, float x, float y, float z, float width, float height, int color) {
        drawTexturedModalRect(matrix, x, y, z, width, height, 0.0F, 1.0F, 0.0F, 1.0F, color);
    }

    public void drawTexturedModalRect(Matrix4f matrix, float x, float y, float z, float width, float height, float u0, float u1, float v0, float v1, int color) {
        vertexBuffer().addVertex(matrix, x + 0.0F, y + 0.0F, z).setUv(u0, v0).setColor(color);
        vertexBuffer().addVertex(matrix, x + 0.0F, y + height, z).setUv(u0, v1).setColor(color);
        vertexBuffer().addVertex(matrix, x + width, y + height, z).setUv(u1, v1).setColor(color);
        vertexBuffer().addVertex(matrix, x + width, y + 0.0F, z).setUv(u1, v0).setColor(color);
    }

    public void drawCenteredString(Matrix4f matrix, String text, float x, float y, float z, int color, boolean shadow) {
        drawCenteredString(matrix, Component.nullToEmpty(text), x, y, z, color, shadow);
    }

    public void drawCenteredString(Matrix4f matrix, Component text, float x, float y, float z, int color, boolean shadow) {
        drawString(matrix, text, x - Minecraft.getInstance().font.width(text) / 2.0F, y, z, color, shadow);
    }

    public void drawString(Matrix4f matrix, String text, float x, float y, float z, int color, boolean shadow) {
        drawString(matrix, Component.nullToEmpty(text), x, y, z, color, shadow);
    }

    public void drawString(Matrix4f matrix, Component text, float x, float y, float z, int color, boolean shadow) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Matrix4f textMatrix = matrixCache.set(matrix).translate(x, y, z);
        Minecraft.getInstance().font.drawInBatch(text, 0.0F, 0.0F, color, shadow, textMatrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        bufferSource.endLastBatch();
    }

    public void beginBatch() {
        if (pipeline == null) {
            throw new IllegalStateException("Cannot begin batch! RenderPipeline is null.");
        }
        bufferBuilder = Tesselator.getInstance().begin(pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
    }

    public VertexConsumer vertexBuffer() {
        return bufferBuilder;
    }

    public void endBatch() {
        if (bufferBuilder == null) {
            throw new IllegalStateException("Cannot end batch! BufferBuilder is null.");
        }
        try (MeshData meshData = bufferBuilder.build()) {
            if (meshData != null) {
                drawWithShader(meshData);
            }
        }
    }

    private void drawWithShader(MeshData meshData) {
        GpuBuffer vertexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(meshData.vertexBuffer());
        GpuBuffer indexBuffer;
        VertexFormat.IndexType indexType;
        if (meshData.indexBuffer() == null) {
            RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
            indexBuffer = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
            indexType = autoStorageIndexBuffer.type();
        } else {
            indexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(meshData.indexBuffer());
            indexType = meshData.drawState().indexType();
        }
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> passName, colorTexture, OptionalInt.empty(), depthTexture, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
            if (scissorState.enabled()) {
                pass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
            }
            RenderSystem.bindDefaultUniforms(pass);
            for (Map.Entry<String, GpuBufferSlice> entry : uniformsMap.entrySet()) {
                pass.setUniform(entry.getKey(), entry.getValue());
            }
            pass.setVertexBuffer(0, vertexBuffer);
            for (Map.Entry<String, TextureBinding> entry : texturesMap.entrySet()) {
                pass.bindTexture(entry.getKey(), entry.getValue().texture(), entry.getValue().sampler());
            }
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
        }
    }

    @Override
    public void close()  {
        RenderSystem.outputColorTextureOverride = lastOutputColorTexture;
        RenderSystem.outputDepthTextureOverride = lastOutputDepthTexture;
        pipeline = null;
        uniformsMap.clear();
        texturesMap.clear();
    }

    static record TextureBinding(GpuTextureView texture, GpuSampler sampler) {
    }
}
