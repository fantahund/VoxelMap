package com.mamiyaotaru.voxelmap.rendering;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.util.ARGB;
import org.joml.Matrix4fStack;
import org.joml.Vector4fc;

public class RenderUtils {
    private static final Matrix4fStack MATRIX_STACK = new Matrix4fStack(16);
    private static final SubmitNodeStorage SUBMIT_NODE_STORAGE = new SubmitNodeStorage();
    private static final VoxelMapRenderTarget FULLSCREEN_TARGET = new VoxelMapRenderTarget("VoxelMap Fullscreen Target", GpuFormat.RGBA8_UNORM, true);
    private static final ArrayDeque<ProjectionEntry> PROJECTION_STACK = new ArrayDeque<>();

    public static void init() {
        FULLSCREEN_TARGET.createBuffers(getSafeScreenWidth(), getSafeScreenHeight());
    }

    private static int getSafeScreenWidth() {
        return Math.max(1, Minecraft.getInstance().getWindow().getScreenWidth());
    }

    private static int getSafeScreenHeight() {
        return Math.max(1, Minecraft.getInstance().getWindow().getScreenHeight());
    }

    public static float getGuiWidth() {
        return (float) getSafeScreenWidth() / Minecraft.getInstance().getWindow().getGuiScale();
    }

    public static float getGuiHeight() {
        return (float) getSafeScreenHeight() / Minecraft.getInstance().getWindow().getGuiScale();
    }

    public static Matrix4fStack getMatrixStack() {
        return MATRIX_STACK;
    }

    public static SubmitNodeStorage getSubmitNodeStorage() {
        return SUBMIT_NODE_STORAGE;
    }

    public static boolean hasFlippedV() {
        return !VoxelConstants.hasVulkanMod(); // Returns true if the renderer uses flipped textures
    }

    public static void blitToScreen(GuiGraphicsExtractor graphics, GpuTextureView texture, float x, float y, float width, float height, int color) {
        float v0 = RenderUtils.hasFlippedV() ? 1.0F : 0.0F;
        float v1 = RenderUtils.hasFlippedV() ? 0.0F : 1.0F;
        VoxelMapGuiGraphics.blitFloat(graphics, RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x, y, width, height, 0.0F, 1.0F, v0, v1, color);
    }

    public static SubmitPass createSubmitPass(String name, RenderTarget target, Vector4fc colorClear, double depthClear) {
        return new SubmitPass(name, target.getColorTextureView(), Optional.of(colorClear), target.getDepthTextureView(), OptionalDouble.of(depthClear));
    }

    public static RenderPass createRenderPass(String name, RenderTarget target, Vector4fc colorClear, double depthClear) {
        return RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> name, target.getColorTextureView(), Optional.of(colorClear), target.getDepthTextureView(), OptionalDouble.of(depthClear));
    }

    public static void flushCmds() {
        RenderSystem.assertOnRenderThread();
        if (!VoxelConstants.hasVulkanMod()) {
            RenderSystem.getDevice().createCommandEncoder().submit();
        } else {
            try {
                Class<?> vkRendererClass = Class.forName("net.vulkanmod.vulkan.Renderer");
                vkRendererClass.getMethod("flushCmds").invoke(vkRendererClass.getMethod("getInstance").invoke(null));
            } catch (Exception ignored) {
            }
        }
    }

    public static VoxelMapRenderTarget getFullscreenTarget() {
        int width = getSafeScreenWidth();
        int height = getSafeScreenHeight();
        if (FULLSCREEN_TARGET.width != width || FULLSCREEN_TARGET.height != height) {
            FULLSCREEN_TARGET.resize(width, height);
        }
        return FULLSCREEN_TARGET;
    }

    public static void setupProjectionMatrix(GpuBufferSlice matrix, ProjectionType type) {
        setupProjectionMatrix(matrix, type, 0.0F);
    }

    public static void setupProjectionMatrix(GpuBufferSlice matrix, ProjectionType type, float initialDepth) {
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.getModelViewStack().translate(0.0f, 0.0F, initialDepth);
        PROJECTION_STACK.push(new ProjectionEntry(RenderSystem.getProjectionMatrixBuffer(), RenderSystem.getProjectionType()));
        RenderSystem.setProjectionMatrix(matrix, type);
    }

    public static void restoreProjectionMatrix() {
        ProjectionEntry projection = PROJECTION_STACK.pop();
        RenderSystem.setProjectionMatrix(projection.matrix(), projection.type());
        RenderSystem.getModelViewStack().popMatrix();
    }

    public static void readTextureContentsToBufferedImage(GpuTexture gpuTexture, Consumer<BufferedImage> resultConsumer) {
        RenderSystem.assertOnRenderThread();
        int bytePerPixel = gpuTexture.getFormat().blockSize();
        int width = gpuTexture.getWidth(0);
        int height = gpuTexture.getHeight(0);
        int bufferSize = bytePerPixel * width * height;
        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture read buffer", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, bufferSize);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            try (GpuBufferSlice.MappedView readView = gpuBuffer.map(true, false)) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pixel = readView.data().getInt((x + y * width) * bytePerPixel);
                        image.setRGB(x, y, ARGB.fromABGR(pixel));
                    }
                }
            }
            gpuBuffer.close();
            resultConsumer.accept(image);
        }, 0);
    }

    public static record ProjectionEntry(GpuBufferSlice matrix, ProjectionType type) {
    }
}
