package com.mamiyaotaru.voxelmap.util;

import java.util.function.Function;
import net.minecraft.util.Util;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public class VoxelMapRenderTypes {
    public static final RenderType WAYPOINT_BEAM = RenderType.create(
            "voxelmap_waypoint_beam",
            RenderSetup.builder(VoxelMapPipelines.WAYPOINT_BEAM_PIPELINE)
                    .createRenderSetup());

    public static final Function<Identifier, RenderType> WAYPOINT_ICON_DEPTHTEST = Util.memoize(
            (Function<Identifier, RenderType>) (identifier -> RenderType.create(
                    "voxelmap_icon_depthtest",
                    RenderSetup.builder(VoxelMapPipelines.WAYPOINT_ICON_DEPTHTEST_PIPELINE)
                            .withTexture("Sampler0", identifier)
                            .createRenderSetup())));

    public static final Function<Identifier, RenderType> WAYPOINT_ICON_NO_DEPTHTEST = Util.memoize(
            (Function<Identifier, RenderType>) (identifier -> RenderType.create(
                    "voxelmap_icon_no_depthtest",
                    RenderSetup.builder(VoxelMapPipelines.WAYPOINT_ICON_NO_DEPTHTEST_PIPELINE)
                            .withTexture("Sampler0", identifier)
                            .createRenderSetup())));

    public static final RenderType WAYPOINT_TEXT_BACKGROUND = RenderType.create(
            "voxelmap_beacon_text_background",
            RenderSetup.builder(VoxelMapPipelines.WAYPOINT_TEXT_BACKGROUND_PIPELINE)
                    .createRenderSetup());
}
