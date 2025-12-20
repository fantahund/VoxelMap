package com.mamiyaotaru.voxelmap.util;

import java.util.function.Function;
import net.minecraft.util.Util;
// TODO: 1.20.1 Port - RenderSetup doesn't exist in 1.20.1, this is a 1.21.x API
// import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public class VoxelMapRenderTypes {
    // TODO: 1.20.1 Port - RenderSetup and RenderPipeline APIs are 1.21.x only
    // These need to be replaced with 1.20.1 RenderType equivalents
    // For now, these are stubbed out to allow compilation

    // TODO: Replace with proper 1.20.1 RenderType for waypoint beams
    public static final RenderType WAYPOINT_BEAM = null; // RenderType.create(
            // "voxelmap_waypoint_beam",
            // RenderSetup.builder(VoxelMapPipelines.WAYPOINT_BEAM_PIPELINE)
            //         .createRenderSetup());

    // TODO: Replace with proper 1.20.1 RenderType for waypoint icons with depth test
    public static final Function<Identifier, RenderType> WAYPOINT_ICON_DEPTHTEST = Util.memoize(
            (Function<Identifier, RenderType>) (identifier -> null)); // RenderType.create(
                    // "voxelmap_icon_depthtest",
                    // RenderSetup.builder(VoxelMapPipelines.WAYPOINT_ICON_DEPTHTEST_PIPELINE)
                    //         .withTexture("Sampler0", identifier)
                    //         .createRenderSetup())));

    // TODO: Replace with proper 1.20.1 RenderType for waypoint icons without depth test
    public static final Function<Identifier, RenderType> WAYPOINT_ICON_NO_DEPTHTEST = Util.memoize(
            (Function<Identifier, RenderType>) (identifier -> null)); // RenderType.create(
                    // "voxelmap_icon_no_depthtest",
                    // RenderSetup.builder(VoxelMapPipelines.WAYPOINT_ICON_NO_DEPTHTEST_PIPELINE)
                    //         .withTexture("Sampler0", identifier)
                    //         .createRenderSetup())));

    // TODO: Replace with proper 1.20.1 RenderType for waypoint text background
    public static final RenderType WAYPOINT_TEXT_BACKGROUND = null; // RenderType.create(
            // "voxelmap_beacon_text_background",
            // RenderSetup.builder(VoxelMapPipelines.WAYPOINT_TEXT_BACKGROUND_PIPELINE)
            //         .createRenderSetup());
}
