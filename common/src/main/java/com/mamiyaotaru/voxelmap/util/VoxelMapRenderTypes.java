package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class VoxelMapRenderTypes extends RenderStateShard {
    private VoxelMapRenderTypes(String p_173178_, Runnable p_173179_, Runnable p_173180_) {
        super(p_173178_, p_173179_, p_173180_);
    }

    // Waypoint beam render type - lines with transparency, no depth test
    public static final RenderType WAYPOINT_BEAM = RenderType.create(
            "voxelmap_waypoint_beam",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.DEBUG_LINES,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(java.util.OptionalDouble.empty()))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    );

    // Waypoint icon with depth test - textured quads with transparency and depth test
    public static final Function<ResourceLocation, RenderType> WAYPOINT_ICON_DEPTHTEST = Util.memoize(
            (texture) -> RenderType.create(
                    "voxelmap_icon_depthtest",
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_TEXT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(COLOR_DEPTH_WRITE)
                            .setCullState(NO_CULL)
                            .setLightmapState(NO_LIGHTMAP)
                            .createCompositeState(false)
            )
    );

    // Waypoint icon without depth test - textured quads with transparency, no depth test
    public static final Function<ResourceLocation, RenderType> WAYPOINT_ICON_NO_DEPTHTEST = Util.memoize(
            (texture) -> RenderType.create(
                    "voxelmap_icon_no_depthtest",
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_TEXT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .setCullState(NO_CULL)
                            .setLightmapState(NO_LIGHTMAP)
                            .createCompositeState(false)
            )
    );

    // Waypoint text background - solid color quads with transparency
    public static final RenderType WAYPOINT_TEXT_BACKGROUND = RenderType.create(
            "voxelmap_beacon_text_background",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_GUI_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    );

    // GUI textured render type - for map and UI elements
    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED = Util.memoize(
            (texture) -> RenderType.create(
                    "voxelmap_gui_textured",
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_TEXT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .setCullState(NO_CULL)
                            .createCompositeState(false)
            )
    );

    // GUI textured unfiltered - for pixel-perfect rendering
    public static final Function<ResourceLocation, RenderType> GUI_TEXTURED_UNFILTERED = Util.memoize(
            (texture) -> RenderType.create(
                    "voxelmap_gui_textured_unfiltered",
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_TEXT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, true)) // mipmap = true for unfiltered
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .setCullState(NO_CULL)
                            .createCompositeState(false)
            )
    );

    // GUI solid color render type
    public static final RenderType GUI = RenderType.create(
            "voxelmap_gui",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_GUI_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    );
}
