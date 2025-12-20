package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Function;

// 1.20.1 Port - Replaced RenderPipeline with RenderType
// These are now simple references to VoxelMapRenderTypes or inline RenderType definitions
public class VoxelMapPipelines extends RenderStateShard {
    private VoxelMapPipelines(String p_173178_, Runnable p_173179_, Runnable p_173180_) {
        super(p_173178_, p_173179_, p_173180_);
    }

    // GUI textured with no depth test - standard GUI rendering
    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED_ANY_DEPTH_PIPELINE =
            VoxelMapRenderTypes.GUI_TEXTURED;

    // GUI textured with DST_ALPHA blending
    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED_ANY_DEPTH_DST_ALPHA_PIPELINE =
            net.minecraft.Util.memoize((texture) -> RenderType.create(
                    "voxelmap_gui_textured_dst_alpha",
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_TEXT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(new TransparencyStateShard("dst_alpha_transparency",
                                    () -> {
                                        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                                        com.mojang.blaze3d.systems.RenderSystem.blendFunc(
                                                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.DST_ALPHA,
                                                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_DST_ALPHA
                                        );
                                    },
                                    () -> {
                                        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                                        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                                    }))
                            .setDepthTestState(NO_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .setCullState(NO_CULL)
                            .createCompositeState(false)
            ));

    // GUI textured with LEQUAL depth test
    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE =
            net.minecraft.Util.memoize((texture) -> RenderType.create(
                    "voxelmap_gui_textured_lequal_depth",
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_TEXT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(COLOR_DEPTH_WRITE)
                            .setCullState(NO_CULL)
                            .createCompositeState(false)
            ));

    // Waypoint beam - just reference VoxelMapRenderTypes
    public static final RenderType WAYPOINT_BEAM_PIPELINE = VoxelMapRenderTypes.WAYPOINT_BEAM;

    // Waypoint icon with depth test - reference VoxelMapRenderTypes
    public static final Function<ResourceLocation, RenderType> WAYPOINT_ICON_DEPTHTEST_PIPELINE =
            VoxelMapRenderTypes.WAYPOINT_ICON_DEPTHTEST;

    // Waypoint icon without depth test - reference VoxelMapRenderTypes
    public static final Function<ResourceLocation, RenderType> WAYPOINT_ICON_NO_DEPTHTEST_PIPELINE =
            VoxelMapRenderTypes.WAYPOINT_ICON_NO_DEPTHTEST;

    // Waypoint text background - reference VoxelMapRenderTypes
    public static final RenderType WAYPOINT_TEXT_BACKGROUND_PIPELINE =
            VoxelMapRenderTypes.WAYPOINT_TEXT_BACKGROUND;

    // Entity icon pipeline - textured rendering for entities on minimap
    public static final Function<ResourceLocation, RenderType> ENTITY_ICON_PIPELINE =
            net.minecraft.Util.memoize((texture) -> RenderType.create(
                    "voxelmap_entity_icon",
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(NO_OVERLAY)
                            .createCompositeState(true)
            ));

    // General GUI and GUI_TEXTURED - reference VoxelMapRenderTypes
    public static final RenderType GUI = VoxelMapRenderTypes.GUI;
    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED = VoxelMapRenderTypes.GUI_TEXTURED;
    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED_UNFILTERED = VoxelMapRenderTypes.GUI_TEXTURED_UNFILTERED;
}
