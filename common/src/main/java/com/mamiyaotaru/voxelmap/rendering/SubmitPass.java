package com.mamiyaotaru.voxelmap.rendering;

import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.joml.Vector4fc;

public class SubmitPass implements AutoCloseable {
    private static final PoseStack POSE_CACHE = new PoseStack();
    private final GpuTextureView lastOutputColorTexture;
    private final GpuTextureView lastOutputDepthTexture;
    private final String name;
    private final SubmitNodeStorage submitNodeStorage;

    private RenderType currentRenderType;

    public SubmitPass(String passName, GpuTextureView colorTexture, Optional<Vector4fc> colorClear, GpuTextureView depthTexture, OptionalDouble depthClear) {
        RenderSystem.assertOnRenderThread();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        if (colorTexture != null && colorClear.isPresent()) {
            encoder.clearColorTexture(colorTexture.texture(), colorClear.get());
        }
        if (depthTexture != null && depthClear.isPresent()) {
            encoder.clearDepthTexture(depthTexture.texture(), depthClear.getAsDouble());
        }

        lastOutputColorTexture = RenderSystem.outputColorTextureOverride;
        lastOutputDepthTexture = RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride = colorTexture;
        RenderSystem.outputDepthTextureOverride = depthTexture;

        name = passName;
        submitNodeStorage = RenderUtils.getSubmitNodeStorage();
    }

    public SubmitNodeStorage getSubmitNodeStorage() {
        return submitNodeStorage;
    }

    public void setRenderType(RenderType renderType) {
        currentRenderType = renderType;
    }

    private PoseStack asPoseStack(Matrix4f matrix) {
        return asPoseStack(matrix, 0.0F, 0.0F, 0.0F);
    }

    private PoseStack asPoseStack(Matrix4f matrix, float xo, float yo, float zo) {
        POSE_CACHE.setIdentity();
        POSE_CACHE.last().pose().set(matrix);
        POSE_CACHE.translate(xo, yo, zo);
        return POSE_CACHE;
    }

    private void submitGeometry(Matrix4f matrix, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        if (currentRenderType == null) {
            throw new IllegalStateException("Set RenderType before submitting geometry!");
        }
        submitNodeStorage.submitCustomGeometry(asPoseStack(matrix), currentRenderType, renderer);
    }

    public void submitBlit(Matrix4f matrix, float x, float y, float z, float width, float height, int color) {
        float v0 = RenderUtils.hasFlippedV() ? 1.0F : 0.0F;
        float v1 = RenderUtils.hasFlippedV() ? 0.0F : 1.0F;
        submitQuad(matrix, x, y, z, width, height, 0.0F, 1.0F, v0, v1, color);
    }

    public void submitQuad(Matrix4f matrix, Sprite sprite, float x, float y, float z, float width, float height, int color) {
        submitQuad(matrix, x, y, z, width, height, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), color);
    }

    public void submitQuad(Matrix4f matrix, float x, float y, float z, float width, float height, int color) {
        submitQuad(matrix, x, y, z, width, height, 0.0F, 1.0F, 0.0F, 1.0F, color);
    }

    public void submitQuad(Matrix4f matrix, float x, float y, float z, float width, float height, float u0, float u1, float v0, float v1, int color) {
        submitGeometry(matrix, (pose, buffer) -> {
            buffer.addVertex(pose, x + 0.0F, y + 0.0F, z).setUv(u0, v0).setColor(color);
            buffer.addVertex(pose, x + 0.0F, y + height, z).setUv(u0, v1).setColor(color);
            buffer.addVertex(pose, x + width, y + height, z).setUv(u1, v1).setColor(color);
            buffer.addVertex(pose, x + width, y + 0.0F, z).setUv(u1, v0).setColor(color);
        });
    }

    public void submitCenteredText(Matrix4f matrix, String text, float x, float y, float z, int color, boolean shadow) {
        submitCenteredText(matrix, Component.nullToEmpty(text), x, y, z, color, shadow);
    }

    public void submitCenteredText(Matrix4f matrix, Component text, float x, float y, float z, int color, boolean shadow) {
        submitText(matrix, text, x - Minecraft.getInstance().font.width(text) / 2.0F, y, z, color, shadow);
    }

    public void submitText(Matrix4f matrix, String text, float x, float y, float z, int color, boolean shadow) {
        submitText(matrix, Component.nullToEmpty(text), x, y, z, color, shadow);
    }

    public void submitText(Matrix4f matrix, Component text, float x, float y, float z, int color, boolean shadow) {
        submitNodeStorage.submitText(asPoseStack(matrix, x, y, z), 0.0F, 0.0F, text.getVisualOrderText(), shadow, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, color, 0x00000000, 0x00000000);
    }

    public void flush() {
        Minecraft.getInstance().gameRenderer.featureRenderDispatcher().renderAllFeatures(submitNodeStorage);
    }

    @Override
    public void close() {
        flush();
        RenderSystem.outputColorTextureOverride = lastOutputColorTexture;
        RenderSystem.outputDepthTextureOverride = lastOutputDepthTexture;
    }
}
