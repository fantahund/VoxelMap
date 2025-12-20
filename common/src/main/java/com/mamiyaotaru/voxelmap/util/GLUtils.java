package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;

public class GLUtils {
    public static void readTextureContentsToBufferedImage(GpuTexture gpuTexture, Consumer<BufferedImage> resultConsumer) {
        // TODO: 1.20.1 Port - RenderSystem.getDevice() doesn't exist in 1.20.1
        // RenderSystem.assertOnRenderThread();
        // int bytePerPixel = gpuTexture.getFormat().pixelSize();
        // int width = gpuTexture.getWidth(0);
        // int height = gpuTexture.getHeight(0);
        // int bufferSize = bytePerPixel * width * height;
        // GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture read buffer", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, bufferSize);
        // CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        // commandEncoder.copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {
        //     BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        //     try (GpuBuffer.MappedView readView = commandEncoder.mapBuffer(gpuBuffer, true, false)) {
        //         for (int y = 0; y < height; y++) {
        //             for (int x = 0; x < width; x++) {
        //                 int pixel = readView.data().getInt((x + y * width) * bytePerPixel);
        //                 image.setRGB(x, y, ARGB.fromABGR(pixel));
        //             }
        //         }
        //     }
        //     gpuBuffer.close();
        //     resultConsumer.accept(image);
        // }, 0);
    }
}
