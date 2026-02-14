package com.mamiyaotaru.voxelmap.util;

import java.util.function.Function;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Util;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public class VoxelMapRenderTypes {
    public static final Function<Identifier, RenderType> GUI_TEXTURED = Util.memoize(
            identifier -> RenderType.create(
                    "voxelmap_gui_textured",
                    RenderSetup.builder(RenderPipelines.GUI_TEXTURED)
                            .withTexture("Sampler0", identifier)
                            .createRenderSetup()
            )
    );

    public static final Function<Identifier, RenderType> GUI_TEXTURED_NO_DEPTH_TEST = Util.memoize(
            identifier -> RenderType.create(
                    "voxelmap_gui_textured_no_depth_test",
                    RenderSetup.builder(VoxelMapPipelines.GUI_TEXTURED_NO_DEPTH_TEST)
                            .withTexture("Sampler0", identifier)
                            .createRenderSetup()
            )
    );

    public static final Function<Identifier, RenderType> GUI_TEXTURED_NO_DEPTH_TEST_DST_ALPHA = Util.memoize(
            identifier -> RenderType.create(
                    "voxelmap_gui_textured_no_depth_test_dst_alpha",
                    RenderSetup.builder(VoxelMapPipelines.GUI_TEXTURED_NO_DEPTH_TEST_DST_ALPHA)
                            .withTexture("Sampler0", identifier)
                            .createRenderSetup()
            )
    );

    public static final Function<Identifier, RenderType> GUI_TEXTURED_LEQUAL_DEPTH_TEST = Util.memoize(
            identifier -> RenderType.create(
                    "voxelmap_gui_textured_lequal_depth_test",
                    RenderSetup.builder(VoxelMapPipelines.GUI_TEXTURED_LEQUAL_DEPTH_TEST)
                            .withTexture("Sampler0", identifier)
                            .createRenderSetup()
            )
    );

    public static final RenderType WAYPOINT_BEAM = RenderType.create(
            "voxelmap_waypoint_beam",
            RenderSetup.builder(VoxelMapPipelines.WAYPOINT_BEAM)
                    .createRenderSetup()
    );

    public static final Function<Identifier, RenderType> WAYPOINT_ICON_DEPTHTEST = Util.memoize(
            identifier -> RenderType.create(
                    "voxelmap_icon_depthtest",
                    RenderSetup.builder(VoxelMapPipelines.WAYPOINT_ICON_DEPTH_TEST)
                            .withTexture("Sampler0", identifier)
                            .createRenderSetup()
            )
    );

    public static final Function<Identifier, RenderType> WAYPOINT_ICON_NO_DEPTHTEST = Util.memoize(
            identifier -> RenderType.create(
                    "voxelmap_icon_no_depthtest",
                    RenderSetup.builder(VoxelMapPipelines.WAYPOINT_ICON_NO_DEPTH_TEST)
                            .withTexture("Sampler0", identifier)
                            .createRenderSetup()
            )
    );

    public static final RenderType WAYPOINT_TEXT_BACKGROUND = RenderType.create(
            "voxelmap_beacon_text_background",
            RenderSetup.builder(VoxelMapPipelines.WAYPOINT_TEXT_BACKGROUND)
                    .createRenderSetup()
    );
}
