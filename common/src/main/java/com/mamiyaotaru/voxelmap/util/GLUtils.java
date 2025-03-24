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
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
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

    private static final RenderPipeline GUI_TEXTURED_EQUAL_DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET).withLocation("pipeline/gui_textured_equal_depth").withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST).build();

    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED_EQUAL_DEPTH = Util.memoize(
            (Function<ResourceLocation, RenderType>) (resourceLocation -> RenderType.create(
                    "gui_textured_equal_depth",
                    0x00C000,
                    GUI_TEXTURED_EQUAL_DEPTH_PIPELINE,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.FALSE, false))
                            .createCompositeState(false))));
}
