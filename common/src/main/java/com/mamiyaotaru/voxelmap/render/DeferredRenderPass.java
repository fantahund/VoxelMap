package com.mamiyaotaru.voxelmap.render;

import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.IndexType;
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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

public class DeferredRenderPass implements AutoCloseable {
    private final String passName;
    private final GpuTextureView colorTexture;
    private final GpuTextureView depthTexture;
    private RenderPipeline pipeline;
    private final HashMap<String, GpuBufferSlice> uniformsMap = new HashMap<>(8);
    private final HashMap<String, TextureBinding> texturesMap = new HashMap<>(8);
    private final PoseStack poseCache = new PoseStack();
    private BufferBuilder bufferBuilder;
    private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
    private final GpuTextureView lastOutputColorTexture;
    private final GpuTextureView lastOutputDepthTexture;

    public DeferredRenderPass(String passName, GpuTextureView colorTexture, Optional<Vector4f> colorClear, GpuTextureView depthTexture, OptionalDouble depthClear) {
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
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(colorTexture.texture(), colorClear.get());
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
        drawStringInBatch(matrix, x, y, z, text, shadow, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, color, 0x00000000, 0x00000000);
    }

    public void drawStringInBatch(Matrix4f matrix, float x, float y, float z, String text, boolean shadow, Font.DisplayMode displayMode, int light, int color, int backgroundColor, int outlineColor) {
        drawStringInBatch(matrix, x, y, z, Component.nullToEmpty(text), shadow, displayMode, light, color, backgroundColor, outlineColor);
    }

    public void drawStringInBatch(Matrix4f matrix, float x, float y, float z, Component text, boolean shadow, Font.DisplayMode displayMode, int light, int color, int backgroundColor, int outlineColor) {
        poseCache.last().pose().set(matrix).translate(x, y, z);
        submitNodeStorage.submitText(poseCache, 0.0F, 0.0F, text.getVisualOrderText(), shadow, displayMode, light, color, backgroundColor, outlineColor);
        Minecraft.getInstance().gameRenderer.featureRenderDispatcher().renderAllFeatures(submitNodeStorage);
    }

    public void beginBatch() {
        if (pipeline == null) {
            throw new IllegalStateException("Cannot begin batch! RenderPipeline is null.");
        }
        bufferBuilder = Tesselator.getInstance().begin(pipeline);
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
        GpuBuffer vertexBuffer = RenderUtils.createVertexBuffer(meshData.vertexBuffer());
        GpuBuffer indexBuffer;
        IndexType indexType;
        if (meshData.indexBuffer() == null) {
            RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().primitiveTopology());
            indexBuffer = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
            indexType = autoStorageIndexBuffer.type();
        } else {
            indexBuffer = RenderUtils.createIndexBuffer(meshData.indexBuffer());
            indexType = meshData.drawState().indexType();
        }
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> passName, colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
            if (scissorState.enabled()) {
                pass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
            }
            RenderSystem.bindDefaultUniforms(pass);
            for (Map.Entry<String, GpuBufferSlice> entry : uniformsMap.entrySet()) {
                pass.setUniform(entry.getKey(), entry.getValue());
            }
            pass.setVertexBuffer(0, vertexBuffer.slice());
            for (Map.Entry<String, TextureBinding> entry : texturesMap.entrySet()) {
                pass.bindTexture(entry.getKey(), entry.getValue().texture(), entry.getValue().sampler());
            }
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.drawIndexed(meshData.drawState().indexCount(), 1, 0, 0, 0);
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
