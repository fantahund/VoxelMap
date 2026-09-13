package com.mamiyaotaru.voxelmap.rendering;

import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.joml.Vector4fc;

public class SubmitPass implements AutoCloseable {
    private static final PoseStack POSE_CACHE = new PoseStack();
    private final GpuTextureView colorTexture;
    private final Optional<Vector4fc> colorClear;
    private final GpuTextureView depthTexture;
    private final OptionalDouble depthClear;
    private final String name;
    private final SubmitNodeStorage submitNodeStorage;

    private int submitOrder;
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

        this.colorTexture = colorTexture;
        this.colorClear = colorClear;
        this.depthTexture = depthTexture;
        this.depthClear = depthClear;

        name = passName;
        submitNodeStorage = RenderUtils.getSubmitNodeStorage();
    }

    private OrderedSubmitNodeCollector getSubmit() {
        return submitNodeStorage.order(submitOrder);
    }

    public void nextDraw() {
        submitOrder++;
    }

    public void setOrder(int i) {
        submitOrder = i;
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

    public void submitGeometry(Matrix4f matrix, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        if (currentRenderType == null) {
            throw new IllegalStateException("Set RenderType before submitting geometry!");
        }
        getSubmit().submitCustomGeometry(asPoseStack(matrix), currentRenderType, renderer);
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
        submitText(matrix, x, y, z, text, shadow, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, color, 0x00000000, 0x00000000);
    }

    public void submitText(Matrix4f matrix, float x, float y, float z, String text, boolean shadow, Font.DisplayMode displayMode, int light, int color, int backgroundColor, int outlineColor) {
        submitText(matrix, x, y, z, Component.nullToEmpty(text), shadow, displayMode, light, color, backgroundColor, outlineColor);
    }

    public void submitText(Matrix4f matrix, float x, float y, float z, Component text, boolean shadow, Font.DisplayMode displayMode, int light, int color, int backgroundColor, int outlineColor) {
        getSubmit().submitText(asPoseStack(matrix, x, y, z), 0.0F, 0.0F, text.getVisualOrderText(), shadow, displayMode, light, color, backgroundColor, outlineColor);
    }

    public void flush() {
        try (
            FeatureRenderDispatcher.PreparedFrame frame = Minecraft.getInstance().gameRenderer.featureRenderDispatcher().prepareFrame(submitNodeStorage);
            RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "VoxelMap SubmitPass Draw", colorTexture, colorClear, depthTexture, depthClear)
        ) {
            RenderSystem.bindDefaultUniforms(pass);
            FeatureRenderDispatcher.renderAllFeatures(pass, frame);
        }

        submitOrder = 0;
        currentRenderType = null;
    }

    @Override
    public void close() {
        flush();
    }
}
