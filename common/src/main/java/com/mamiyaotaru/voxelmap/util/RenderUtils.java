package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
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
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.image.BufferedImage;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

public class RenderUtils {
    private static final Tesselator TESSELATOR = new Tesselator(4096);
    private static final CachedOrthoProjectionMatrixBuffer FULLSCREEN_PROJECTION = new CachedOrthoProjectionMatrixBuffer("VoxelMap Fullscreen Projection", 1000.0F, 3000.0F, true);
    private static final CachedOrthoProjectionMatrixBuffer BLIT_PROJECTION = new CachedOrthoProjectionMatrixBuffer("VoxelMap Blit Projection", 1000.0F, 3000.0F, true);
    private static GpuTextureView colorScratch;
    private static GpuTextureView depthScratch;
    private static final int SCRATCH_TEXTURE_SIZE = 2048;
    private static final int SCRATCH_TEXTURE_USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;

    public static float getGuiWidth() {
        return (float) Minecraft.getInstance().getWindow().getWidth() / Minecraft.getInstance().getWindow().getGuiScale();
    }

    public static float getGuiHeight() {
        return (float) Minecraft.getInstance().getWindow().getHeight() / Minecraft.getInstance().getWindow().getGuiScale();
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
        Minecraft.getInstance().font.drawInBatch(text, 0.0F, 0.0F, color, shadow, matrixStack, bufferSource, Font.DisplayMode.NORMAL, 0, 0x00F000F0);

        matrixStack.popMatrix();
    }

    public static void drawCenteredString(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, String text, float x, float y, float z, int color, boolean shadow) {
        drawCenteredString(matrixStack, bufferSource, Component.nullToEmpty(text), x, y, z, color, shadow);
    }

    public static void drawCenteredString(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, Component text, float x, float y, float z, int color, boolean shadow) {
        drawString(matrixStack, bufferSource, text, x - (Minecraft.getInstance().font.width(text) / 2.0F), y, z, color, shadow);
    }

    public static void drawTooltip(GuiGraphics guiGraphics, Tooltip tooltip, int x, int y) {
        if (tooltip == null) {
            return;
        }

        guiGraphics.setTooltipForNextFrame(tooltip.toCharSequence(Minecraft.getInstance()), x, y);
    }

    public static GpuTextureView getScratchTexture(TextureFormat format) {
        RenderSystem.assertOnRenderThread();
        if (format == TextureFormat.RGBA8) {
            if (colorScratch == null) {
                GpuTexture colorTex = RenderSystem.getDevice().createTexture("VoxelMap Scratch Color Texture", SCRATCH_TEXTURE_USAGE, TextureFormat.RGBA8, SCRATCH_TEXTURE_SIZE, SCRATCH_TEXTURE_SIZE, 1, 1);
                colorScratch = RenderSystem.getDevice().createTextureView(colorTex);
            }
            return colorScratch;
        }
        if (format == TextureFormat.DEPTH32) {
            if (depthScratch == null) {
                GpuTexture depthTex = RenderSystem.getDevice().createTexture("VoxelMap Scratch Depth Texture", SCRATCH_TEXTURE_USAGE, TextureFormat.DEPTH32, SCRATCH_TEXTURE_SIZE, SCRATCH_TEXTURE_SIZE, 1, 1);
                depthScratch = RenderSystem.getDevice().createTextureView(depthTex);
            }
            return depthScratch;
        }
        return null;
    }

    public static void readTextureContentsToBufferedImage(GpuTexture gpuTexture, Consumer<BufferedImage> resultConsumer) {
        RenderSystem.assertOnRenderThread();
        int bytePerPixel = gpuTexture.getFormat().pixelSize();
        int width = gpuTexture.getWidth(0);
        int height = gpuTexture.getHeight(0);
        int bufferSize = bytePerPixel * width * height;
        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture read buffer", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, bufferSize);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {
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
            resultConsumer.accept(image);
        }, 0);
    }

    public static void flipTexture(GpuTextureView textureView, boolean flipX, boolean flipY) {
        RenderSystem.assertOnRenderThread();

        float u00 = flipX ? 1.0F : 0.0F;
        float u01 = flipX ? 0.0F : 1.0F;
        float v00 = flipY ? 0.0F : 1.0F;
        float v01 = flipY ? 1.0F : 0.0F;

        float u10 = 0.0F;
        float u11 = 1.0F;
        float v10 = 1.0F;
        float v11 = 0.0F;

        GpuTextureView scratchTex = getScratchTexture(textureView.texture().getFormat());
        simpleBlit(textureView, scratchTex, u00, u01, v00, v01, 0xFFFFFFFF); // flip
        simpleBlit(scratchTex, textureView, u10, u11, v10, v11, 0xFFFFFFFF); // copy
    }

    public static void simpleBlit(GpuTextureView src, GpuTextureView dst, float u0, float u1, float v0, float v1, int color) {
        RenderSystem.assertOnRenderThread();

        RenderPipeline pipeline = VoxelMapPipelines.GUI_TEXTURED_NO_DEPTH_TEST;
        BufferBuilder bufferBuilder = TESSELATOR.begin(VertexFormat.Mode.QUADS, pipeline.getVertexFormat());
        bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setUv(u0, v0).setColor(color);
        bufferBuilder.addVertex(0.0F, 1.0F, 0.0F).setUv(u0, v1).setColor(color);
        bufferBuilder.addVertex(1.0F, 1.0F, 0.0F).setUv(u1, v1).setColor(color);
        bufferBuilder.addVertex(1.0F, 0.0F, 0.0F).setUv(u1, v0).setColor(color);

        GpuBufferSlice projection = BLIT_PROJECTION.getBuffer(1.0F, 1.0F);
        TextureSetup textureSetup = TextureSetup.singleTexture(src, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        try (MeshData meshData = bufferBuilder.build()) {
            RenderUtils.drawMeshWithTexture(dst, null, projection, -2000.0F, meshData, pipeline, textureSetup);
        }

        TESSELATOR.clear();
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
            VoxelConstants.getLogger().error("Failed to render with custom projection. ", e);
        } finally {
            RenderSystem.outputColorTextureOverride = lastColorTexture;
            RenderSystem.outputDepthTextureOverride = lastDepthTexture;
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(lastProjectionMatrix, lastProjectionType);
        }

        flipTexture(renderTarget.getColorTextureView(), false, true);
    }

    public static void renderWithFullscreenProjection(RenderTarget renderTarget, Runnable runnable) {
        RenderSystem.assertOnRenderThread();

        if (renderTarget.getColorTexture() == null || renderTarget.getDepthTexture() == null) {
            return;
        }

        int windowWidth = Minecraft.getInstance().getWindow().getWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getHeight();

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
            RenderSystem.setProjectionMatrix(FULLSCREEN_PROJECTION.getBuffer(getGuiWidth(), getGuiHeight()), ProjectionType.ORTHOGRAPHIC);
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            RenderSystem.getModelViewStack().translate(0.0F, 0.0F, -2000.0F);
            RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();

            runnable.run();
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Failed to render with fullscreen projection. ", e);
        } finally {
            RenderSystem.outputColorTextureOverride = lastColorTexture;
            RenderSystem.outputDepthTextureOverride = lastDepthTexture;
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(lastProjectionMatrix, lastProjectionType);
        }

        flipTexture(renderTarget.getColorTextureView(), false, true);
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
            VoxelConstants.getLogger().error("Immediate draw failed.", e);
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(lastProjectionMatrix, lastProjectionType);
        }
    }
}
