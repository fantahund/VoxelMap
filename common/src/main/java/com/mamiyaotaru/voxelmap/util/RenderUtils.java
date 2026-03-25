package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class RenderUtils {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final Projection FULLSCREEN_PROJECTION  = new Projection();
    private static final ProjectionMatrixBuffer FULLSCREEN_PROJECTION_MATRIX = new ProjectionMatrixBuffer("VoxelMap Fullscreen Projection Matrix");
    static { FULLSCREEN_PROJECTION.setupOrtho(1000.0F, 3000.0F, 0.0F, 0.0F, true); }

    public static float getGuiWidth() {
        return (float) MINECRAFT.getWindow().getWidth() / MINECRAFT.getWindow().getGuiScale();
    }

    public static float getGuiHeight() {
        return (float) MINECRAFT.getWindow().getHeight() / MINECRAFT.getWindow().getGuiScale();
    }

    public static void drawTexturedModalRect(Matrix4fStack matrixStack, VertexConsumer vertexConsumer, float x, float y, float z, float width, float height, int color) {
        drawTexturedModalRect(matrixStack, vertexConsumer, x, y, z, width, height, 0.0F, 1.0F, 0.0F, 1.0F, color);
    }

    public static void drawTexturedModalRect(Matrix4fStack matrixStack, VertexConsumer vertexConsumer, Sprite sprite, float x, float y, float z, float width, float height, int color) {
        drawTexturedModalRect(matrixStack, vertexConsumer, x, y, z, width, height, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), color);
    }

    public static void drawTexturedModalRect(Matrix4fStack matrixStack, VertexConsumer vertexConsumer, float x, float y, float z, float width, float height, float u0, float u1, float v0, float v1, int color) {
        vertexConsumer.addVertex(matrixStack, x + 0.0F, y + 0.0F, z).setUv(u0, v0).setColor(color);
        vertexConsumer.addVertex(matrixStack, x + 0.0F, y + height, z).setUv(u0, v1).setColor(color);
        vertexConsumer.addVertex(matrixStack, x + width, y + height, z).setUv(u1, v1).setColor(color);
        vertexConsumer.addVertex(matrixStack, x + width, y + 0.0F, z).setUv(u1, v0).setColor(color);
    }

    public static void drawString(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, String text, float x, float y, float z, int color, boolean shadow) {
        drawString(matrixStack, bufferSource, Component.nullToEmpty(text), x, y, z, color, shadow);
    }

    public static void drawString(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, Component text, float x, float y, float z, int color, boolean shadow) {
        matrixStack.pushMatrix();
        matrixStack.translate(x, y, z);
        MINECRAFT.font.drawInBatch(text, 0.0F, 0.0F, color, shadow, matrixStack, bufferSource, Font.DisplayMode.NORMAL, 0, 0x00F000F0);

        matrixStack.popMatrix();
    }

    public static void drawCenteredString(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, String text, float x, float y, float z, int color, boolean shadow) {
        drawCenteredString(matrixStack, bufferSource, Component.nullToEmpty(text), x, y, z, color, shadow);
    }

    public static void drawCenteredString(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, Component text, float x, float y, float z, int color, boolean shadow) {
        drawString(matrixStack, bufferSource, text, x - (MINECRAFT.font.width(text) / 2.0F), y, z, color, shadow);
    }

    public static void renderWithCustomProjection(RenderTarget renderTarget, GpuBufferSlice projection, float initialDepth, Runnable runnable) {
        RenderSystem.assertOnRenderThread();

        if (renderTarget.getColorTexture() == null || renderTarget.getDepthTexture() == null) {
            return;
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(renderTarget.getColorTexture(), 0x00000000);
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(renderTarget.getDepthTexture(), 1.0);

        GpuBufferSlice lastProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType lastProjectionType = RenderSystem.getProjectionType();
        GpuTextureView lastColorTexture = RenderSystem.outputColorTextureOverride;
        GpuTextureView lastDepthTexture = RenderSystem.outputDepthTextureOverride;

        try {
            RenderSystem.setProjectionMatrix(projection, ProjectionType.ORTHOGRAPHIC);
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            RenderSystem.getModelViewStack().translate(0.0F, 0.0F, initialDepth);
            RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();

            runnable.run();
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Failed to render with custom projection. Exception: " + e);
        } finally {
            RenderSystem.outputColorTextureOverride = lastColorTexture;
            RenderSystem.outputDepthTextureOverride = lastDepthTexture;
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(lastProjectionMatrix, lastProjectionType);
        }

        GLUtils.flipTexture(renderTarget.getColorTextureView(), false, true);
    }

    public static void renderWithFullscreenProjection(RenderTarget renderTarget, Runnable runnable) {
        RenderSystem.assertOnRenderThread();

        if (renderTarget.getColorTexture() == null || renderTarget.getDepthTexture() == null) {
            return;
        }

        int windowWidth = MINECRAFT.getWindow().getWidth();
        int windowHeight = MINECRAFT.getWindow().getHeight();

        if (renderTarget.width != windowWidth || renderTarget.height != windowHeight) {
            renderTarget.resize(windowWidth, windowHeight);
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(renderTarget.getColorTexture(), 0x00000000);
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(renderTarget.getDepthTexture(), 1.0);

        GpuBufferSlice lastProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType lastProjectionType = RenderSystem.getProjectionType();
        GpuTextureView lastColorTexture = RenderSystem.outputColorTextureOverride;
        GpuTextureView lastDepthTexture = RenderSystem.outputDepthTextureOverride;

        try {
            FULLSCREEN_PROJECTION.setSize(getGuiWidth(), getGuiHeight());
            RenderSystem.setProjectionMatrix(FULLSCREEN_PROJECTION_MATRIX.getBuffer(FULLSCREEN_PROJECTION), ProjectionType.ORTHOGRAPHIC);
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            RenderSystem.getModelViewStack().translate(0.0F, 0.0F, -2000.0F);
            RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();

            runnable.run();
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Failed to render with fullscreen projection. Exception: " + e);
        } finally {
            RenderSystem.outputColorTextureOverride = lastColorTexture;
            RenderSystem.outputDepthTextureOverride = lastDepthTexture;
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(lastProjectionMatrix, lastProjectionType);
        }

        GLUtils.flipTexture(renderTarget.getColorTextureView(), false, true);
    }

    public static void drawMeshWithTexture(GpuTextureView colorTexture, GpuTextureView depthTexture, GpuBufferSlice projection, float initialDepth, MeshData meshData, RenderPipeline pipeline, TextureSetup textureSetup) {
        if (meshData == null) {
            return;
        }

        GpuBufferSlice lastProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType lastProjectionType = RenderSystem.getProjectionType();
        try {
            RenderSystem.setProjectionMatrix(projection, ProjectionType.ORTHOGRAPHIC);
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            RenderSystem.getModelViewStack().translate(0.0F, 0.0F, initialDepth);
            GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().writeTransform(
                    RenderSystem.getModelViewMatrix(),
                    new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                    new Vector3f(),
                    new Matrix4f());
            GpuBuffer vertexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(meshData.vertexBuffer());
            GpuBuffer indexBuffer;
            VertexFormat.IndexType indexType;
            if (meshData.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
                indexBuffer = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
                indexType = autoStorageIndexBuffer.type();
            } else {
                indexBuffer = pipeline.getVertexFormat().uploadImmediateIndexBuffer(meshData.indexBuffer());
                indexType = meshData.drawState().indexType();
            }
            OptionalInt colorClear = OptionalInt.of(0x00000000);
            OptionalDouble depthClear = depthTexture == null ? OptionalDouble.empty() : OptionalDouble.of(1.0);
            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "VoxelMap Immediate Draw", colorTexture, colorClear, depthTexture, depthClear)) {
                renderPass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indexBuffer, indexType);
                renderPass.bindTexture("Sampler0", textureSetup.texure0(), textureSetup.sampler0());
                renderPass.bindTexture("Sampler1", textureSetup.texure1(), textureSetup.sampler1());
                renderPass.bindTexture("Sampler2", textureSetup.texure2(), textureSetup.sampler2());
                renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
            }
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Immediate draw failed. Exception: " + e);
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(lastProjectionMatrix, lastProjectionType);
        }
    }
}
