package com.mamiyaotaru.voxelmap.render;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class RenderUtils {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();

    private static final Matrix4fStack MATRIX_STACK = new Matrix4fStack(16);
    private static final Matrix4f MATRIX_CACHE = new Matrix4f();
    private static final GpuSampler DEFAULT_SAMPLER = RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.LINEAR, false);
    private static final GpuSampler BLIT_SAMPLER = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
    private static final RenderTarget FULLSCREEN_RENDER_TARGET = new VoxelMapRenderTarget("VoxelMap Fullscreen Target", true);
    private static int lastScreenWidth;
    private static int lastScreenHeight;

    private static final Tesselator TESSELATOR = Tesselator.getInstance();
    private static TextureSetup boundTexture;
    private static RenderPipeline pipeline;
    private static BufferBuilder bufferBuilder;

    private static final ArrayDeque<ProjectionState> PROJECTION_STACK = new ArrayDeque<>();
    private static final ArrayDeque<RenderTargetState> RENDER_TARGET_STACK = new ArrayDeque<>();

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
        guiGraphics.guiRenderState.submitBlitToCurrentLayer(new BlitRenderState(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, TextureSetup.singleTexture(renderTarget.getColorTextureView(), BLIT_SAMPLER), guiGraphics.pose(), 0, 0, (int) getGuiWidth(), (int) getGuiHeight(), 0.0F, 1.0F, v0, v1, 0xFFFFFFFF, guiGraphics.scissorStack.peek()));
    }

    public static void drawString(Matrix4f matrix, String text, float x, float y, float z, int color, boolean shadow) {
        drawString(matrix, Component.nullToEmpty(text), x, y, z, color, shadow);
    }

    public static void drawString(Matrix4f matrix, Component text, float x, float y, float z, int color, boolean shadow) {
        MATRIX_CACHE.set(matrix);
        matrix.translate(x, y, z);
        MINECRAFT.font.drawInBatch(text, 0.0F, 0.0F, color, shadow, matrix, MINECRAFT.renderBuffers().bufferSource(), Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        MINECRAFT.renderBuffers().bufferSource().endLastBatch();
        matrix.set(MATRIX_CACHE);
    }

    public static void drawCenteredString(Matrix4f matrix, String text, float x, float y, float z, int color, boolean shadow) {
        drawCenteredString(matrix, Component.nullToEmpty(text), x, y, z, color, shadow);
    }

    public static void drawCenteredString(Matrix4f matrix, Component text, float x, float y, float z, int color, boolean shadow) {
        drawString(matrix, text, x - (MINECRAFT.font.width(text) / 2.0F), y, z, color, shadow);
    }
    public static void drawSpriteRect(Matrix4f matrix, Sprite sprite, float x, float y, float z, float width, float height, int color) {
        drawTexturedModalRect(matrix, x, y, z, width, height, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), color);
    }

    public static void drawBlitRect(Matrix4f matrix, float x, float y, float z, float width, float height, int color) {
        float v0 = hasFlippedTexture() ? 1.0F : 0.0F;
        float v1 = hasFlippedTexture() ? 0.0F : 1.0F;
        drawTexturedModalRect(matrix, x, y, z, width, height, 0.0F, 1.0F, v0, v1, color);
    }

    public static void drawTexturedModalRect(Matrix4f matrix, float x, float y, float z, float width, float height, int color) {
        drawTexturedModalRect(matrix, x, y, z, width, height, 0.0F, 1.0F, 0.0F, 1.0F, color);
    }

    public static void drawTexturedModalRect(Matrix4f matrix, float x, float y, float z, float width, float height, float u0, float u1, float v0, float v1, int color) {
        vertexBuffer().addVertex(matrix, x + 0.0F, y + 0.0F, z).setUv(u0, v0).setColor(color);
        vertexBuffer().addVertex(matrix, x + 0.0F, y + height, z).setUv(u0, v1).setColor(color);
        vertexBuffer().addVertex(matrix, x + width, y + height, z).setUv(u1, v1).setColor(color);
        vertexBuffer().addVertex(matrix, x + width, y + 0.0F, z).setUv(u1, v0).setColor(color);
    }

    public static void beginBatch(RenderPipeline renderPipeline, Identifier texture) {
        beginBatch(renderPipeline, MINECRAFT.getTextureManager().getTexture(texture));
    }

    public static void beginBatch(RenderPipeline renderPipeline, AbstractTexture texture) {
        beginBatch(renderPipeline, TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler()));
    }

    public static void beginBatch(RenderPipeline renderPipeline, GpuTextureView texture) {
        beginBatch(renderPipeline, TextureSetup.singleTexture(texture, DEFAULT_SAMPLER));
    }

    public static void beginBatch(RenderPipeline renderPipeline, TextureSetup textureSetup) {
        RenderSystem.assertOnRenderThread();
        boundTexture = textureSetup;
        pipeline = renderPipeline;
        bufferBuilder = TESSELATOR.begin(VertexFormat.Mode.QUADS, pipeline.getVertexFormat());
    }

    public static VertexConsumer vertexBuffer() {
        return bufferBuilder;
    }

    public static void endBatch() {
        RenderSystem.assertOnRenderThread();
        try (MeshData meshData = bufferBuilder.build()) {
            if (meshData == null) {
                return;
            }
            GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
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
            GpuTextureView outputColorTexture = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : MINECRAFT.getMainRenderTarget().getColorTextureView();
            GpuTextureView outputDepthTexture = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputDepthTextureOverride : MINECRAFT.getMainRenderTarget().getDepthTextureView();
            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "VoxelMap Draw Pass", outputColorTexture, OptionalInt.empty(), outputDepthTexture, OptionalDouble.empty())) {
                renderPass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.bindTexture("Sampler0", boundTexture.texure0(), boundTexture.sampler0());
                renderPass.bindTexture("Sampler1", boundTexture.texure1(), boundTexture.sampler1());
                renderPass.bindTexture("Sampler2", boundTexture.texure2(), boundTexture.sampler2());
                renderPass.setIndexBuffer(indexBuffer, indexType);
                renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
            }
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Immediate draw failed! ", e);
        } finally {
            TESSELATOR.clear();
        }
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
        RenderSystem.assertOnRenderThread();
        ProjectionState projectionState = new ProjectionState(RenderSystem.getProjectionMatrixBuffer(), RenderSystem.getProjectionType());
        PROJECTION_STACK.push(projectionState);
        RenderSystem.setProjectionMatrix(matrix, type);
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.getModelViewStack().translate(0.0F, 0.0F, initialDepth);
    }

    public static void restoreProjectionMatrix() {
        RenderSystem.assertOnRenderThread();
        RenderSystem.getModelViewStack().popMatrix();
        ProjectionState projectionState = PROJECTION_STACK.pop();
        RenderSystem.setProjectionMatrix(projectionState.matrix(), projectionState.type());
    }

    public static void setRenderTarget(RenderTarget renderTarget) {
        RenderSystem.assertOnRenderThread();
        RenderTargetState renderTargetState = new RenderTargetState(RenderSystem.outputColorTextureOverride, RenderSystem.outputDepthTextureOverride);
        RENDER_TARGET_STACK.push(renderTargetState);
        RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();
    }

    public static void restoreRenderTarget() {
        RenderSystem.assertOnRenderThread();
        RenderTargetState renderTargetState = RENDER_TARGET_STACK.pop();
        RenderSystem.outputColorTextureOverride = renderTargetState.color();
        RenderSystem.outputDepthTextureOverride = renderTargetState.depth();
    }

    public static void clearRenderTarget(RenderTarget renderTarget, int colorClear, double depthClear) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        if (renderTarget.getColorTexture() != null) {
            commandEncoder.clearColorTexture(renderTarget.getColorTexture(), colorClear);
        }
        if (renderTarget.getDepthTexture() != null) {
            commandEncoder.clearDepthTexture(renderTarget.getDepthTexture(), depthClear);
        }
    }

    static record ProjectionState(GpuBufferSlice matrix, ProjectionType type) {
    }

    static record RenderTargetState(GpuTextureView color, GpuTextureView depth) {
    }
}
