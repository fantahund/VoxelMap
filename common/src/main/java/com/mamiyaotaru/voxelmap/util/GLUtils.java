package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.PlatformResolver;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.ARGB;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class GLUtils {
    public static GlTexture getGlTexture(GpuTexture gpuTexture) {
        return PlatformResolver.resolve(PlatformResolver.ResolverType.GPU_TEXTURE_TO_GL_TEXTURE, gpuTexture);
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

    private static final Long2ObjectOpenHashMap<GpuTexture> TEXTURE_CACHES = new Long2ObjectOpenHashMap<>();

    public static void postProcessTexture(GpuTexture gpuTexture, BiConsumer<GpuTexture, GpuTexture> consumer) {
        RenderSystem.assertOnRenderThread();

        int width = gpuTexture.getWidth(0);
        int height = gpuTexture.getHeight(0);
        TextureFormat format = gpuTexture.getFormat();

        long key = ((long) width << 40) | ((long) height << 16) | format.ordinal();
        GpuTexture copy;
        if (TEXTURE_CACHES.containsKey(key) && !TEXTURE_CACHES.get(key).isClosed()) {
            copy = TEXTURE_CACHES.get(key);
        } else {
            int usage = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;
            copy = RenderSystem.getDevice().createTexture("VoxelMap Texture Cache " + key, usage, format, width, height, 1, 1);

            TEXTURE_CACHES.put(key, copy);
        }

        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(gpuTexture, copy, 0, 0, 0, 0, 0, width, height);

        consumer.accept(copy, gpuTexture);

    }

    public static void flipTexture(GpuTexture src, GpuTexture dst, boolean flipX, boolean flipY) {
        RenderSystem.assertOnRenderThread();
        GlDevice device = (GlDevice) RenderSystem.getDevice();
        GlTexture src2 = getGlTexture(src);
        GlTexture dst2 = getGlTexture(dst);

        int width = src2.getWidth(0);
        int height = src2.getHeight(0);
        int x0 = flipX ? width : 0;
        int y0 = flipY ? height : 0;
        int x1 = flipX ? 0 : width;
        int y1 = flipY ? 0 : height;

        int lastReadFramebuffer = GlStateManager.getFrameBuffer(GL30.GL_READ_FRAMEBUFFER);
        int lastDrawFramebuffer = GlStateManager.getFrameBuffer(GL30.GL_DRAW_FRAMEBUFFER);

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src2.getFbo(device.directStateAccess(), null));
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst2.getFbo(device.directStateAccess(), null));
        GlStateManager._glBlitFrameBuffer(0, 0, width, height, x0, y0, x1, y1, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, lastReadFramebuffer);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, lastDrawFramebuffer);

    }
}
