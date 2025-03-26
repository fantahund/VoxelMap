package com.mamiyaotaru.voxelmap.util;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.TriState;

public class GLUtils {
    public static void readTextureContentsToPixelArray(GpuTexture gpuTexture, Consumer<int[]> resultConsumer) {
        Preconditions.checkNotNull(resultConsumer);
        int size = gpuTexture.getWidth(0) * gpuTexture.getHeight(0);
        int bufferSize = gpuTexture.getFormat().pixelSize() * size;
        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture read buffer", BufferType.PIXEL_PACK, BufferUsage.STATIC_READ, bufferSize);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        commandEncoder.copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {
            try (GpuBuffer.ReadView readView = commandEncoder.readBuffer(gpuBuffer)) {
                int[] pixels = new int[size];
                readView.data().asIntBuffer().get(0, pixels);
                resultConsumer.accept(pixels);
            } finally {
                gpuBuffer.close();
            }
        }, 0);
    }

    public static void readTextureContentsToBufferedImage(GpuTexture gpuTexture, Consumer<BufferedImage> resultConsumer) {
        RenderSystem.assertOnRenderThread();
        int bytePerPixel = gpuTexture.getFormat().pixelSize();
        int width = gpuTexture.getWidth(0);
        int height = gpuTexture.getHeight(0);
        int bufferSize = bytePerPixel * width * height;
        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture read buffer", BufferType.PIXEL_PACK, BufferUsage.STATIC_READ, bufferSize);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            try (GpuBuffer.ReadView readView = commandEncoder.readBuffer(gpuBuffer)) {
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

    public static final RenderPipeline GUI_TEXTURED_EQUAL_DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET).withLocation("pipeline/gui_textured_equal_depth").withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST).build();

    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED_EQUAL_DEPTH = Util.memoize(
            (Function<ResourceLocation, RenderType>) (resourceLocation -> RenderType.create(
                    "voxelmap_gui_textured_equal_depth",
                    0x00C000,
                    GUI_TEXTURED_EQUAL_DEPTH_PIPELINE,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.TRUE, false))
                            .createCompositeState(false))));

    public static final RenderPipeline GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET).withLocation("pipeline/gui_textured_equal_depth").withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).build();

    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED_LESS_OR_EQUAL_DEPTH = Util.memoize(
            (Function<ResourceLocation, RenderType>) (resourceLocation -> RenderType.create(
                    "voxelmap_gui_textured_lequal_depth",
                    0x00C000,
                    GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.TRUE, false))
                            .createCompositeState(false))));

}
