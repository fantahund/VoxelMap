package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import java.util.Optional;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class VoxelMapPipelines {

    public static final RenderPipeline GUI_TEXTURED_NO_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/gui_textured_no_depth_test"))
            .withDepthStencilState(Optional.empty())
            .build();

    public static final RenderPipeline GUI_TEXTURED_LEQUAL_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/gui_textured_lequal_depth_test"))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .build();

    public static final RenderPipeline GUI_TEXTURED_MASKED_NO_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/gui_textured_masked_no_depth_test"))
            .withDepthStencilState(Optional.empty())
            .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR))
            .build();

    public static final RenderPipeline WAYPOINT_TEXT_BACKGROUND = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/waypoint_text_background"))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .build();

    public static final RenderPipeline ENTITY_ICON = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/entity_icon"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .build();

    public static final RenderPipeline ENTITY_ICON_CULLED = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/entity_icon_culled"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(true)
            .build();
}
