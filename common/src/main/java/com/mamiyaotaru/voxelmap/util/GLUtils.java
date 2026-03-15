package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.util.ARGB;

import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;
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

    public static void flipTexture(GpuTextureView src, GpuTextureView dst, boolean flipX, boolean flipY) {
        RenderSystem.assertOnRenderThread();

        RenderPipeline pipeline = VoxelMapPipelines.GUI_TEXTURED_NO_DEPTH_TEST;
        BufferBuilder bufferBuilder = TESSELATOR.begin(VertexFormat.Mode.QUADS, pipeline.getVertexFormat());
        float u0 = flipX ? 1.0F : 0.0F;
        float u1 = flipX ? 0.0F : 1.0F;
        float v0 = flipY ? 0.0F : 1.0F;
        float v1 = flipY ? 1.0F : 0.0F;
        bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setUv(u0, v0).setColor(0xFFFFFFFF);
        bufferBuilder.addVertex(0.0F, 1.0F, 0.0F).setUv(u0, v1).setColor(0xFFFFFFFF);
        bufferBuilder.addVertex(1.0F, 1.0F, 0.0F).setUv(u1, v1).setColor(0xFFFFFFFF);
        bufferBuilder.addVertex(1.0F, 0.0F, 0.0F).setUv(u1, v0).setColor(0xFFFFFFFF);

        try (MeshData meshData = bufferBuilder.build()) {
            GpuSampler nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            GpuBufferSlice projection = BLIT_PROJECTION_MATRIX.getBuffer(BLIT_PROJECTION);
            RenderUtils.drawMeshWithTexture(dst, null, projection, -2000.0F, meshData, pipeline, TextureSetup.singleTexture(src, nearest));
        }

        TESSELATOR.clear();
    }

    public static class PostProcess {
        private static final Long2ObjectOpenHashMap<GpuTextureView> POST_PROCESS_CACHE = new Long2ObjectOpenHashMap<>();
        private static final int MAX_CACHE_COUNT = 32;
        private static final int CACHE_USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;
        private static int lastWindowWidth;
        private static int lastWindowHeight;

        public static void postProcessTexture(GpuTextureView src, BiConsumer<GpuTextureView, GpuTextureView> consumer) {
            RenderSystem.assertOnRenderThread();
            handlePostProcessCache();

            GpuTexture texture = src.texture();
            TextureFormat format = texture.getFormat();
            int width = texture.getWidth(0);
            int height = texture.getHeight(0);

            // width (16bit), height (16bit), format (8bit)
            long key = ((long) width << 24) | ((long) height << 8) | (format.ordinal() & 0xFFL);
            GpuTextureView dst = POST_PROCESS_CACHE.get(key);
            if (dst == null || dst.isClosed()) {
                GpuTexture destTexture = RenderSystem.getDevice().createTexture("VoxelMap Texture Cache " + key, CACHE_USAGE, format, width, height, 1, 1);
                dst = RenderSystem.getDevice().createTextureView(destTexture);

                POST_PROCESS_CACHE.put(key, dst);
            }

            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(texture, dst.texture(), 0, 0, 0, 0, 0, width, height);
            consumer.accept(dst, src);
        }

        private static void handlePostProcessCache() {
            RenderSystem.assertOnRenderThread();

            boolean overCapacity = POST_PROCESS_CACHE.size() > MAX_CACHE_COUNT;
            int windowWidth = VoxelConstants.getMinecraft().getWindow().getWidth();
            int windowHeight = VoxelConstants.getMinecraft().getWindow().getHeight();

            if (overCapacity || windowWidth != lastWindowWidth || windowHeight != lastWindowHeight) {
                for (GpuTextureView cache : POST_PROCESS_CACHE.values()) {
                    if (cache != null && !cache.isClosed()) {
                        cache.close();
                        cache.texture().close();
                    }
                }
                POST_PROCESS_CACHE.clear();
            }

            lastWindowWidth = windowWidth;
            lastWindowHeight = windowHeight;
        }
    }
}
