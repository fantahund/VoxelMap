package com.mamiyaotaru.voxelmap.render;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

public final class RenderUtils {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final Matrix4fStack MATRIX_STACK = new Matrix4fStack(16);
    private static final RenderTarget FULLSCREEN_TARGET = new VoxelMapRenderTarget("VoxelMap Fullscreen Target", true, GpuFormat.RGBA8_UNORM);
    private static final ArrayDeque<ProjectionState> PROJECTION_STACK = new ArrayDeque<>();
    private static GpuBuffer immediateVertexBuffer;
    private static GpuBuffer immediateIndexBuffer;

    private RenderUtils() {
    }

    public static void init() {
        int width = getSafeScreenWidth();
        int height = getSafeScreenHeight();
        FULLSCREEN_TARGET.createBuffers(width, height);
    }

    public static boolean hasFlippedTexture() {
        return !VoxelConstants.hasVulkanMod() || RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
    }

    public static float getGuiWidth() {
        return (float) MINECRAFT.getWindow().getWidth() / MINECRAFT.getWindow().getGuiScale();
    }

    public static float getGuiHeight() {
        return (float) MINECRAFT.getWindow().getHeight() / MINECRAFT.getWindow().getGuiScale();
    }

    public static GpuBuffer createVertexBuffer(ByteBuffer vertexBuf) {
        if (immediateVertexBuffer != null && !immediateVertexBuffer.isClosed()) {
            immediateVertexBuffer.close();
        }
        immediateVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap Immediate Vertex Buffer", GpuBuffer.USAGE_VERTEX, vertexBuf);
        return immediateVertexBuffer;
    }

    public static GpuBuffer createIndexBuffer(ByteBuffer indexBuf) {
        if (immediateIndexBuffer != null && !immediateIndexBuffer.isClosed()) {
            immediateIndexBuffer.close();
        }
        immediateIndexBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap Immediate Index Buffer", GpuBuffer.USAGE_INDEX, indexBuf);
        return immediateIndexBuffer;
    }

    public static Matrix4fStack getMatrixStack() {
        return MATRIX_STACK;
    }

    public static void drawTooltip(GuiGraphicsExtractor graphics, Tooltip tooltip, int x, int y) {
        if (tooltip == null) {
            return;
        }
        graphics.setTooltipForNextFrame(tooltip.toCharSequence(MINECRAFT), x, y);
    }

    public static void blitRenderTarget(GuiGraphicsExtractor graphics, RenderTarget renderTarget) {
        float v0 = hasFlippedTexture() ? 1.0F : 0.0F;
        float v1 = hasFlippedTexture() ? 0.0F : 1.0F;
        graphics.guiRenderState.addGuiElement(new FloatBlitRenderState(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, TextureSetup.singleTexture(renderTarget.getColorTextureView(), VoxelMapPipelines.NEAREST_REPEAT_SAMPLER), graphics.pose(), 0.0F, 0.0F, getGuiWidth(), getGuiHeight(), 0.0F, 1.0F, v0, v1, 0xFFFFFFFF, 0xFFFFFFFF, graphics.scissorStack.peek()));
    }

    public static DeferredRenderPass createDeferredRenderPass(String passName, GpuTextureView colorTexture, Optional<Vector4f> colorClear, GpuTextureView depthTexture, OptionalDouble depthClear) {
        GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
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

    public static RenderTarget getFullscreenTarget() {
        RenderSystem.assertOnRenderThread();
        int width = getSafeScreenWidth();
        int height = getSafeScreenHeight();
        if (width != FULLSCREEN_TARGET.width || height != FULLSCREEN_TARGET.height) {
            FULLSCREEN_TARGET.resize(width, height);
        }
        return FULLSCREEN_TARGET;
    }

    private static int getSafeScreenWidth() {
        return Math.max(1, MINECRAFT.getWindow().getScreenWidth());
    }

    private static int getSafeScreenHeight() {
        return Math.max(1, MINECRAFT.getWindow().getScreenHeight());
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

    static record ProjectionState(GpuBufferSlice matrix, ProjectionType type) {
    }
}
