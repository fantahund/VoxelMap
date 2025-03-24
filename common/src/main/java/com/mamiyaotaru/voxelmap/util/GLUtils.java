package com.mamiyaotaru.voxelmap.util;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.util.function.Consumer;

public class GLUtils {
    public static void readTextureContentsToPixelArray(GpuTexture gpuTexture, Consumer<int[]> resultConsumer) {
        Preconditions.checkNotNull(resultConsumer);
        int bufferSize = gpuTexture.getFormat().pixelSize() * gpuTexture.getWidth(0) * gpuTexture.getHeight(0);
        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture read buffer", BufferType.PIXEL_PACK, BufferUsage.STATIC_READ, bufferSize);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        commandEncoder.copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {
            try (GpuBuffer.ReadView readView = commandEncoder.readBuffer(gpuBuffer)) {
                int[] pixels = new int[bufferSize];
                readView.data().asIntBuffer().get(0, pixels);
                resultConsumer.accept(pixels);
            } finally {
                gpuBuffer.close();
            }
        }, 0);
    }
}
