package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.util.ARGB;

import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GLUtils {
    private static final Tesselator TESSELATOR = new Tesselator(4096);
    private static final Projection BLIT_PROJECTION  = new Projection();
    private static final ProjectionMatrixBuffer BLIT_PROJECTION_MATRIX = new ProjectionMatrixBuffer("VoxelMap Blit Projection Matrix");
    static { BLIT_PROJECTION.setupOrtho(1000.0F, 3000.0F, 1.0F, 1.0F, true); }

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

        GpuTextureView tempTexture = TextureCache.getOrCreate(textureView.texture());
        simpleBlit(textureView, tempTexture, u00, u01, v00, v01, 0xFFFFFFFF); // flip
        simpleBlit(tempTexture, textureView, u10, u11, v10, v11, 0xFFFFFFFF); // copy
    }

    private static GpuBufferSlice getBlitProjection() {
        return BLIT_PROJECTION_MATRIX.getBuffer(BLIT_PROJECTION);
    }

    public static void simpleBlit(GpuTextureView src, GpuTextureView dst, float u0, float u1, float v0, float v1, int color) {
        RenderSystem.assertOnRenderThread();

        RenderPipeline pipeline = VoxelMapPipelines.GUI_TEXTURED_NO_DEPTH_TEST;
        BufferBuilder bufferBuilder = TESSELATOR.begin(VertexFormat.Mode.QUADS, pipeline.getVertexFormat());
        bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setUv(u0, v0).setColor(color);
        bufferBuilder.addVertex(0.0F, 1.0F, 0.0F).setUv(u0, v1).setColor(color);
        bufferBuilder.addVertex(1.0F, 1.0F, 0.0F).setUv(u1, v1).setColor(color);
        bufferBuilder.addVertex(1.0F, 0.0F, 0.0F).setUv(u1, v0).setColor(color);

        GpuBufferSlice projection = getBlitProjection();
        TextureSetup textureSetup = TextureSetup.singleTexture(src, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        try (MeshData meshData = bufferBuilder.build()) {
            RenderUtils.drawMeshWithTexture(dst, null, projection, -2000.0F, meshData, pipeline, textureSetup);
        }

        TESSELATOR.clear();
    }

    private static class TextureCache {
        private static final ConcurrentHashMap<Long, GpuTextureView> POOL = new ConcurrentHashMap<>();
        private static final int USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;
        private static final int MAX_SIZE = 32;

        private static int lastGuiWidth = -1;
        private static int lastGuiHeight = -1;

        public static GpuTextureView getOrCreate(GpuTexture texture) {
            return getOrCreate(texture.getWidth(0), texture.getHeight(0), texture.getFormat());
        }

        public static GpuTextureView getOrCreate(int width, int height, TextureFormat format) {
            RenderSystem.assertOnRenderThread();
            handleCaches();
            long data = ((long) width << 32) | ((long) height << 8) | (format.ordinal() & 0xFFL);
            return POOL.computeIfAbsent(data, key -> {
                GpuTexture texture = RenderSystem.getDevice().createTexture("VoxelMap Texture Cache " + key, USAGE, format, width, height, 1, 1);
                return RenderSystem.getDevice().createTextureView(texture);
            });
        }

        public static void clear() {
            POOL.values().forEach(view -> {
                view.close();
                view.texture().close();
            });
            POOL.clear();
        }

        private static void handleCaches() {
            int guiWidth = (int) RenderUtils.getGuiWidth();
            int guiHeight = (int) RenderUtils.getGuiHeight();
            if (POOL.size() > MAX_SIZE || guiWidth != lastGuiWidth || guiHeight != lastGuiHeight) {
                clear();
                lastGuiWidth = guiWidth;
                lastGuiHeight = guiHeight;
            }
        }
    }
}
