package com.mamiyaotaru.voxelmap.render;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public class VoxelMapPipelines {
    public static final GpuSampler NEAREST_CLAMP_SAMPLER;
    public static final GpuSampler NEAREST_REPEAT_SAMPLER;
    public static final GpuSampler LINEAR_CLAMP_SAMPLER;
    public static final GpuSampler LINEAR_REPEAT_SAMPLER;
    public static final RenderPipeline GUI_TEXTURED_NO_DEPTH_TEST;
    public static final RenderPipeline GUI_TEXTURED_NO_DEPTH_TEST_MASKED;
    public static final RenderPipeline GUI_TEXTURED_LEQUAL_DEPTH_TEST;
    public static final RenderPipeline GUI_NO_DEPTH_TEST;
    public static final RenderPipeline GUI_LEQUAL_DEPTH_TEST;
    public static final RenderPipeline WAYPOINT_ICON_DEPTH_TEST;
    public static final RenderPipeline WAYPOINT_ICON_NO_DEPTH_TEST;
    public static final RenderPipeline WAYPOINT_TEXT_BACKGROUND_DEPTH_TEST;
    public static final RenderPipeline WAYPOINT_TEXT_BACKGROUND_NO_DEPTH_TEST;
    public static final RenderPipeline ENTITY_ICON;
    public static final RenderPipeline ENTITY_ICON_CULLED;

    static {
        NEAREST_CLAMP_SAMPLER = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

        NEAREST_REPEAT_SAMPLER = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);

        LINEAR_CLAMP_SAMPLER = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

        LINEAR_REPEAT_SAMPLER = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);

        GUI_TEXTURED_NO_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/gui_textured_no_depth_test"))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
                .build();

        GUI_TEXTURED_NO_DEPTH_TEST_MASKED = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/gui_textured_no_depth_test_masked"))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
                .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR))
                .build();

        GUI_TEXTURED_LEQUAL_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/gui_textured_lequal_depth_test"))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                .build();

        GUI_NO_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/gui_no_depth_test"))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
                .build();

        GUI_LEQUAL_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/gui_lequal_depth_test"))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                .build();

        WAYPOINT_ICON_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/waypoint_icon_depth_test"))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                .build();

        WAYPOINT_ICON_NO_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/waypoint_icon_no_depth_test"))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
                .build();

        WAYPOINT_TEXT_BACKGROUND_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/waypoint_text_background_depth_test"))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                .build();

        WAYPOINT_TEXT_BACKGROUND_NO_DEPTH_TEST = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/waypoint_text_background_no_depth_test"))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .build();

        ENTITY_ICON = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/entity_icon"))
                .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(false)
                .build();

        ENTITY_ICON_CULLED = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "pipeline/entity_icon_culled"))
                .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(true)
                .build();
    }
}
