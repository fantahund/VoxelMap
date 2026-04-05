package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class VoxelMapPipelines {

    public static final RenderPipeline GUI_TEXTURED_NO_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/gui_textured_no_depth_test"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build();

    public static final RenderPipeline GUI_TEXTURED_LEQUAL_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/gui_textured_lequal_depth_test"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build();

    public static final RenderPipeline GUI_TEXTURED_MASKED_NO_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/gui_textured_masked_no_depth_test"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withColorWrite(true, false)
            .build();

    public static final RenderPipeline WAYPOINT_ICON_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/waypoint_icon_detph_test"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(true)
            .build();

    public static final RenderPipeline WAYPOINT_ICON_NO_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/waypoint_icon_no_depth_test"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(true)
            .build();

    public static final RenderPipeline WAYPOINT_TEXT_BACKGROUND_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/waypoint_text_background_depth_test"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthBias(1.0F, 7.0F)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .build();

    public static final RenderPipeline WAYPOINT_TEXT_BACKGROUND_NO_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/waypoint_text_background_no_depth_test"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthBias(1.0F, 7.0F)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .build();


    public static final VertexFormat ENTITY_VERTEX = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .build();

    public static final RenderPipeline ENTITY_ICON = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/entity_icon"))
            .withSampler("Sampler1")
            .withVertexFormat(ENTITY_VERTEX, VertexFormat.Mode.QUADS)
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build();

    public static final RenderPipeline ENTITY_ICON_CULLED = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/entity_icon_culled"))
            .withSampler("Sampler1")
            .withVertexFormat(ENTITY_VERTEX, VertexFormat.Mode.QUADS)
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build();
}
