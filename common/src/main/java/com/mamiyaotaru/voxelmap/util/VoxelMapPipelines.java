package com.mamiyaotaru.voxelmap.util;

// TODO: 1.20.1 Port - RenderPipeline doesn't exist in 1.20.1, this is a 1.21.x API
// All pipeline definitions are temporarily set to null to allow compilation
// These need to be replaced with proper 1.20.1 rendering equivalents

public class VoxelMapPipelines {
    // TODO: 1.20.1 Port - Replace with proper 1.20.1 RenderType or rendering approach
    // Original 1.21.x code:
    // RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
    //     .withLocation(new ResourceLocation("voxelmap:pipeline/gui_textured_any_depth"))
    //     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build()
    public static final Object GUI_TEXTURED_ANY_DEPTH_PIPELINE = null;

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 RenderType or rendering approach
    // Original 1.21.x code:
    // RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
    //     .withLocation(new ResourceLocation("voxelmap:pipeline/gui_textured_any_depth_dst_alpha"))
    //     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
    //     .withBlend(new BlendFunction(SourceFactor.DST_ALPHA, DestFactor.ONE_MINUS_DST_ALPHA)).build()
    public static final Object GUI_TEXTURED_ANY_DEPTH_DST_ALPHA_PIPELINE = null;

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 RenderType or rendering approach
    // Original 1.21.x code:
    // RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
    //     .withLocation(new ResourceLocation("voxelmap:pipeline/gui_textured_equal_depth"))
    //     .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).build()
    public static final Object GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE = null;

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 RenderType or rendering approach
    // Original 1.21.x code:
    // RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
    //     .withLocation(new ResourceLocation("voxelmap:pipeline/waypoint_beam"))
    //     .withVertexShader("core/position_color").withFragmentShader("core/position_color")
    //     .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.TRIANGLE_STRIP)
    //     .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
    //     .withBlend(BlendFunction.LIGHTNING).withDepthWrite(false).build()
    public static final Object WAYPOINT_BEAM_PIPELINE = null;

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 RenderType or rendering approach
    // Original 1.21.x code:
    // RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
    //     .withLocation(new ResourceLocation("voxelmap:pipeline/waypoint_icon"))
    //     .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
    //     .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, ...)).withDepthWrite(true).build()
    public static final Object WAYPOINT_ICON_DEPTHTEST_PIPELINE = null;

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 RenderType or rendering approach
    // Original 1.21.x code:
    // RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
    //     .withLocation(new ResourceLocation("voxelmap:pipeline/waypoint_icon"))
    //     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
    //     .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, ...)).withDepthWrite(true).build()
    public static final Object WAYPOINT_ICON_NO_DEPTHTEST_PIPELINE = null;

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 RenderType or rendering approach
    // Original 1.21.x code:
    // RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
    //     .withLocation(new ResourceLocation("voxelmap:pipeline/waypoint_background"))
    //     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthBias(1.0F, 7.0F)
    //     .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, ...)).withDepthWrite(false).build()
    public static final Object WAYPOINT_TEXT_BACKGROUND_PIPELINE = null;

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 RenderType or rendering approach
    // Original 1.21.x code:
    // RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
    //     .withLocation(new ResourceLocation("voxelmap:pipeline/entity_solid"))
    //     .withSampler("Sampler1").withVertexFormat(VF, VertexFormat.Mode.QUADS)
    //     .withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY")
    //     .withShaderDefine("NO_CARDINAL_LIGHTING").withShaderDefine("ALPHA_CUTOUT", 0.1F)
    //     .withBlend(BlendFunction.TRANSLUCENT).build()
    public static final Object ENTITY_ICON_PIPELINE = null;
}
