package com.mamiyaotaru.voxelmap.render;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class RenderUtils {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final Matrix4fStack MATRIX_STACK = new Matrix4fStack(16);
    private static final RenderTarget FULLSCREEN_RENDER_TARGET = new VoxelMapRenderTarget("VoxelMap Fullscreen Target", true);
    private static int lastScreenWidth;
    private static int lastScreenHeight;
    private static final ArrayDeque<ProjectionState> PROJECTION_STACK = new ArrayDeque<>();

    private RenderUtils() {
    }

    public static boolean hasFlippedTexture() {
        return !VoxelConstants.hasVulkanMod();
    }

    public static float getGuiWidth() {
        return (float) MINECRAFT.getWindow().getWidth() / MINECRAFT.getWindow().getGuiScale();
    }

    public static float getGuiHeight() {
        return (float) MINECRAFT.getWindow().getHeight() / MINECRAFT.getWindow().getGuiScale();
    }

    public static Matrix4fStack getRenderMatrixStack() {
        return MATRIX_STACK;
    }

    public static RenderTarget getFullscreenRenderTarget() {
        RenderSystem.assertOnRenderThread();
        int width = MINECRAFT.getWindow().getScreenWidth();
        int height = MINECRAFT.getWindow().getScreenHeight();
        if (width > 0 && height > 0 && (width != lastScreenWidth || height != lastScreenHeight)) {
            lastScreenWidth = width;
            lastScreenHeight = height;
            FULLSCREEN_RENDER_TARGET.resize(width, height);
        }
        return FULLSCREEN_RENDER_TARGET;
    }

    public static void drawTooltip(GuiGraphics guiGraphics, Tooltip tooltip, int x, int y) {
        if (tooltip == null) {
            return;
        }
        guiGraphics.setTooltipForNextFrame(tooltip.toCharSequence(MINECRAFT), x, y);
    }

    public static void blitRenderTarget(GuiGraphics guiGraphics, RenderTarget renderTarget) {
        float v0 = hasFlippedTexture() ? 1.0F : 0.0F;
        float v1 = hasFlippedTexture() ? 0.0F : 1.0F;
        guiGraphics.guiRenderState.submitBlitToCurrentLayer(new BlitRenderState(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, TextureSetup.singleTexture(renderTarget.getColorTextureView(), VoxelMapPipelines.NEAREST_REPEAT_SAMPLER), guiGraphics.pose(), 0, 0, (int) getGuiWidth(), (int) getGuiHeight(), 0.0F, 1.0F, v0, v1, 0xFFFFFFFF, guiGraphics.scissorStack.peek()));
    }

    public static DeferredRenderPass createDeferredRenderPass(String passName, GpuTextureView colorTexture, OptionalInt colorClear, GpuTextureView depthTexture, OptionalDouble depthClear) {
        GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
        DeferredRenderPass pass = new DeferredRenderPass(passName, colorTexture, colorClear, depthTexture, depthClear);
        pass.setUniform("DynamicTransforms", dynamicUniforms);
        return pass;
    }

    public static void forceFlushCommands() {
        RenderSystem.assertOnRenderThread();
        if (VoxelConstants.hasVulkanMod()) {
            try {
                Class<?> vkRendererClass = Class.forName("net.vulkanmod.vulkan.Renderer");
                vkRendererClass.getMethod("flushCmds").invoke(vkRendererClass.getMethod("getInstance").invoke(null));
            } catch (Exception ignored) {
            }
        }
    }

    public static BufferedImage readTextureContentsToBufferedImage(GpuTexture gpuTexture) {
        RenderSystem.assertOnRenderThread();
        int bytePerPixel = gpuTexture.getFormat().pixelSize();
        int width = gpuTexture.getWidth(0);
        int height = gpuTexture.getHeight(0);
        int bufferSize = bytePerPixel * width * height;
        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture read buffer", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, bufferSize);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {}, 0);
        try (GpuFence fence = commandEncoder.createFence()) {
            fence.awaitCompletion(Long.MAX_VALUE);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        try (GpuBuffer.MappedView readView = commandEncoder.mapBuffer(gpuBuffer, true, false)) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = readView.data().getInt((x + y * width) * bytePerPixel);
                    image.setRGB(x, y, ARGB.fromABGR(pixel));
                }
            }
        }
        gpuBuffer.close();
        return image;
    }

    public static void setProjectionMatrix(GpuBufferSlice matrix, ProjectionType type, float initialDepth) {
        ProjectionState projectionState = new ProjectionState(RenderSystem.getProjectionMatrixBuffer(), RenderSystem.getProjectionType());
        PROJECTION_STACK.push(projectionState);
        RenderSystem.setProjectionMatrix(matrix, type);
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.getModelViewStack().translate(0.0F, 0.0F, initialDepth);
    }

    public static void restoreProjectionMatrix() {
        RenderSystem.getModelViewStack().popMatrix();
        ProjectionState projectionState = PROJECTION_STACK.pop();
        RenderSystem.setProjectionMatrix(projectionState.matrix(), projectionState.type());
    }

    static record ProjectionState(GpuBufferSlice matrix, ProjectionType type) {
    }

    static record RenderTargetState(GpuTextureView color, GpuTextureView depth) {
    }
}
