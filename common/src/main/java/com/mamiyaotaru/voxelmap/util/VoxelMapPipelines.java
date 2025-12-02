package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class VoxelMapPipelines {

    public static final RenderPipeline GUI_TEXTURED_ANY_DEPTH_PIPELINE = RenderPipeline
            .builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.parse("voxelmap:pipeline/gui_textured_any_depth"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build();

    public static final BlendFunction DST_ALPHA = new BlendFunction(SourceFactor.DST_ALPHA, DestFactor.ONE_MINUS_DST_ALPHA);

    public static final RenderPipeline GUI_TEXTURED_ANY_DEPTH_DST_ALPHA_PIPELINE = RenderPipeline
            .builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.parse("voxelmap:pipeline/gui_textured_any_depth_dst_alpha"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withBlend(DST_ALPHA)
            .build();

    public static final RenderPipeline GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE = RenderPipeline
            .builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.parse("voxelmap:pipeline/gui_textured_equal_depth"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).build();

    public static final RenderPipeline WAYPOINT_BEAM_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.parse("voxelmap:pipeline/waypoint_beam"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.TRIANGLE_STRIP)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withBlend(BlendFunction.LIGHTNING)
            .withDepthWrite(false)
            .build();

    public static final RenderPipeline WAYPOINT_ICON_DEPTHTEST_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.parse("voxelmap:pipeline/waypoint_icon"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
            .withDepthWrite(true)
            .build();

    public static final RenderPipeline WAYPOINT_ICON_NO_DEPTHTEST_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.parse("voxelmap:pipeline/waypoint_icon"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
            .withDepthWrite(true)
            .build();

    public static final RenderPipeline WAYPOINT_TEXT_BACKGROUND_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.parse("voxelmap:pipeline/waypoint_background"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthBias(1.0F, 7.0F)
            .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
            .withDepthWrite(false)
            .build();

    public static final VertexFormat VF = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .build();

    public static final RenderPipeline ENTITY_ICON_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.parse("voxelmap:pipeline/entity_solid"))
            .withSampler("Sampler1")
            .withVertexFormat(VF, VertexFormat.Mode.QUADS)
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withBlend(BlendFunction.TRANSLUCENT)
            .build();
}
