package com.mamiyaotaru.voxelmap.util;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;

public class GLUtils {
    public static void readTextureContentsToPixelArray(GpuTexture gpuTexture, Consumer<int[]> resultConsumer) {
        Preconditions.checkNotNull(resultConsumer);
        int size = gpuTexture.getWidth(0) * gpuTexture.getHeight(0);
        int bufferSize = gpuTexture.getFormat().pixelSize() * size;
        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture read buffer", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, bufferSize);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        commandEncoder.copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {
            try (GpuBuffer.MappedView readView = commandEncoder.mapBuffer(gpuBuffer, true, false)) {
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

    public static final RenderPipeline GUI_TEXTURED_EQUAL_DEPTH_PIPELINE = RenderPipeline
            .builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(ResourceLocation.parse("voxelmap:pipeline/gui_textured_equal_depth"))
            .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
            .build();

    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED_EQUAL_DEPTH = Util.memoize(
            (Function<ResourceLocation, RenderType>) (resourceLocation -> RenderType.create(
                    "voxelmap_gui_textured_equal_depth",
                    0x00C000,
                    GUI_TEXTURED_EQUAL_DEPTH_PIPELINE,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false))
                            .createCompositeState(false))));

    public static final RenderPipeline GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE = RenderPipeline
            .builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(ResourceLocation.parse("voxelmap:pipeline/gui_textured_equal_depth"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).build();

    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED_LESS_OR_EQUAL_DEPTH = Util.memoize(
            (Function<ResourceLocation, RenderType>) (resourceLocation -> RenderType.create(
                    "voxelmap_gui_textured_lequal_depth",
                    0x00C000,
                    GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false))
                            .createCompositeState(false))));

    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_FILTER_MIN = Util.memoize(
            (Function<ResourceLocation, RenderType>) (resourceLocation -> RenderType.create(
                    "voxelmap_gui_textured_lequal_depth_filter_min",
                    0x00C000,
                    GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE,
                    RenderType.CompositeState.builder()
                            .setTextureState(new ExtendedTextureStateShard(resourceLocation, FilterMode.LINEAR, FilterMode.NEAREST, true))
                            .createCompositeState(false))));

    public static class ExtendedTextureStateShard extends RenderStateShard.EmptyTextureStateShard {
        private final Optional<ResourceLocation> texture;
        private final FilterMode minFilter;
        private final FilterMode magFilter;
        private final boolean mipmap;

        public ExtendedTextureStateShard(ResourceLocation resourceLocation, FilterMode minFilter, FilterMode magFilter, boolean mipmap) {
            super(() -> {
                TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                AbstractTexture abstractTexture = textureManager.getTexture(resourceLocation);
                abstractTexture.getTexture().setTextureFilter(minFilter, magFilter, mipmap);
                RenderSystem.setShaderTexture(0, abstractTexture.getTextureView());
            }, () -> {
            });
            this.texture = Optional.of(resourceLocation);
            this.minFilter = minFilter;
            this.magFilter = magFilter;
            this.mipmap = mipmap;
        }

        @Override
        public String toString() {
            return this.name + "[" + this.texture + "(minFilter=" + this.minFilter + ", magFilter=" + this.magFilter + ", mipmap=" + this.mipmap + ")]";
        }

        @Override
        protected Optional<ResourceLocation> cutoutTexture() {
            return this.texture;
        }
    }

    public static final RenderPipeline WAYPOINT_BEAM_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(ResourceLocation.parse("voxelmap:pipeline/waypoint_beam"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.TRIANGLE_STRIP)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withBlend(BlendFunction.LIGHTNING)
            .withDepthWrite(false)
            .build();

    public static final RenderType WAYPOINT_BEAM = RenderType.create(
            "voxelmap_waypoint_beam",
            0x00C000, // buffer size
            WAYPOINT_BEAM_PIPELINE,
            RenderType.CompositeState.builder()
                    .createCompositeState(false));

    public static final RenderPipeline WAYPOINT_ICON_DEPTHTEST_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(ResourceLocation.parse("voxelmap:pipeline/waypoint_icon"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
            .withDepthWrite(true)
            .build();

    public static final RenderPipeline WAYPOINT_ICON_NO_DEPTHTEST_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(ResourceLocation.parse("voxelmap:pipeline/waypoint_icon"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
            .withDepthWrite(true)
            .build();

    public static final Function<ResourceLocation, RenderType> WAYPOINT_ICON_DEPTHTEST = Util.memoize(
            (Function<ResourceLocation, RenderType>) (resourceLocation -> RenderType.create(
                    "voxelmap_icon_depthtest",
                    0x00C000, // buffer size
                    WAYPOINT_ICON_DEPTHTEST_PIPELINE,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false))
                            .createCompositeState(false))));

    public static final Function<ResourceLocation, RenderType> WAYPOINT_ICON_NO_DEPTHTEST = Util.memoize(
            (Function<ResourceLocation, RenderType>) (resourceLocation -> RenderType.create(
                    "voxelmap_icon_no_depthtest",
                    0x00C000, // buffer size
                    WAYPOINT_ICON_NO_DEPTHTEST_PIPELINE,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false))
                            .createCompositeState(false))));

    public static final RenderPipeline WAYPOINT_TEXT_BACKGROUND_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(ResourceLocation.parse("voxelmap:pipeline/waypoint_background"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthBias(1.0F, 7.0F)
            .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
            .withDepthWrite(false)
            .build();

    public static final RenderType WAYPOINT_TEXT_BACKGROUND = RenderType.create(
            "voxelmap_beacon_text_background",
            0x00C000, // buffer size
            WAYPOINT_TEXT_BACKGROUND_PIPELINE,
            RenderType.CompositeState.builder()
                    .createCompositeState(false));

    public static final VertexFormat VF = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .build();

    public static final RenderPipeline ENTITY_ICON = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(ResourceLocation.parse("voxelmap:pipeline/entity_solid"))
            .withSampler("Sampler1")
            .withVertexFormat(VF, VertexFormat.Mode.QUADS)
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withBlend(BlendFunction.TRANSLUCENT)
            .build();
}
