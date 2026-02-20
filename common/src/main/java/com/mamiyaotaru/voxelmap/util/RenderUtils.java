package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4fStack;

public class RenderUtils {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final CachedOrthoProjectionMatrixBuffer FULLSCREEN_PROJECTION = new CachedOrthoProjectionMatrixBuffer("VoxelMap Fullscreen GUI Projection", 1000.0F, 3000.0F, true);

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

    public static void renderWithCustomProjection(DynamicAllocatedTexture colorTexture, DynamicAllocatedTexture depthTexture, GpuBufferSlice projection, float initialDepth, Runnable runnable) {
        RenderSystem.assertOnRenderThread();

        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(colorTexture.getTexture(), 0x00000000);
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(depthTexture.getTexture(), 1.0);

        GpuBufferSlice lastProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType lastProjectionType = RenderSystem.getProjectionType();
        GpuTextureView lastColorTexture = RenderSystem.outputColorTextureOverride;
        GpuTextureView lastDepthTexture = RenderSystem.outputDepthTextureOverride;

        try {
            RenderSystem.setProjectionMatrix(projection, ProjectionType.ORTHOGRAPHIC);
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            RenderSystem.getModelViewStack().translate(0.0F, 0.0F, initialDepth);
            RenderSystem.outputColorTextureOverride = colorTexture.getTextureView();
            RenderSystem.outputDepthTextureOverride = depthTexture.getTextureView();

            runnable.run();
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Failed to render with custom projection. Exception: " + e);
        } finally {
            RenderSystem.outputColorTextureOverride = lastColorTexture;
            RenderSystem.outputDepthTextureOverride = lastDepthTexture;
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(lastProjectionMatrix, lastProjectionType);
        }

        GLUtils.PostProcess.postProcessTexture(colorTexture.getTexture(), (src, dst) -> {
            GLUtils.flipTexture(src, dst, false, true);
        });
    }

    public static void renderWithFullscreenProjection(DynamicAllocatedTexture colorTexture, DynamicAllocatedTexture depthTexture, Runnable runnable) {
        RenderSystem.assertOnRenderThread();

        int windowWidth = MINECRAFT.getWindow().getWidth();
        int windowHeight = MINECRAFT.getWindow().getHeight();
        int guiWidth = MINECRAFT.getWindow().getGuiScaledWidth();
        int guiHeight = MINECRAFT.getWindow().getGuiScaledHeight();

        if (colorTexture.getWidth(0) != windowWidth || colorTexture.getHeight(0) != windowHeight) {
            GpuTexture texture = colorTexture.getTexture();
            colorTexture.setTexture(RenderSystem.getDevice().createTexture(texture.getLabel(), texture.usage(), texture.getFormat(), windowWidth, windowHeight, 1, 1));
        }

        if (depthTexture.getWidth(0) != windowWidth || depthTexture.getHeight(0) != windowHeight) {
            GpuTexture texture = depthTexture.getTexture();
            depthTexture.setTexture(RenderSystem.getDevice().createTexture(texture.getLabel(), texture.usage(), texture.getFormat(), windowWidth, windowHeight, 1, 1));
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(colorTexture.getTexture(), 0x00000000);
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(depthTexture.getTexture(), 1.0);

        GpuBufferSlice lastProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType lastProjectionType = RenderSystem.getProjectionType();
        GpuTextureView lastColorTexture = RenderSystem.outputColorTextureOverride;
        GpuTextureView lastDepthTexture = RenderSystem.outputDepthTextureOverride;

        try {
            RenderSystem.setProjectionMatrix(FULLSCREEN_PROJECTION.getBuffer(guiWidth, guiHeight), ProjectionType.ORTHOGRAPHIC);
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            RenderSystem.getModelViewStack().translate(0.0F, 0.0F, -2000.0F);
            RenderSystem.outputColorTextureOverride = colorTexture.getTextureView();
            RenderSystem.outputDepthTextureOverride = depthTexture.getTextureView();

            runnable.run();
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Failed to render with fullscreen projection. Exception: " + e);
        } finally {
            RenderSystem.outputColorTextureOverride = lastColorTexture;
            RenderSystem.outputDepthTextureOverride = lastDepthTexture;
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(lastProjectionMatrix, lastProjectionType);
        }

        GLUtils.PostProcess.postProcessTexture(colorTexture.getTexture(), (src, dst) -> {
            GLUtils.flipTexture(src, dst, false, true);
        });
    }
}
