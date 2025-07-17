package com.mamiyaotaru.voxelmap.util;

import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class VoxelMapRenderTypes {

    public static final RenderType WAYPOINT_BEAM = RenderType.create(
            "voxelmap_waypoint_beam",
            0x00C000, // buffer size
            VoxelMapPipelines.WAYPOINT_BEAM_PIPELINE,
            RenderType.CompositeState.builder()
                    .createCompositeState(false));

    public static final Function<ResourceLocation, RenderType> WAYPOINT_ICON_DEPTHTEST = Util.memoize(
            (Function<ResourceLocation, RenderType>) (resourceLocation -> RenderType.create(
                    "voxelmap_icon_depthtest",
                    0x00C000, // buffer size
                    VoxelMapPipelines.WAYPOINT_ICON_DEPTHTEST_PIPELINE,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false))
                            .createCompositeState(false))));

    public static final Function<ResourceLocation, RenderType> WAYPOINT_ICON_NO_DEPTHTEST = Util.memoize(
            (Function<ResourceLocation, RenderType>) (resourceLocation -> RenderType.create(
                    "voxelmap_icon_no_depthtest",
                    0x00C000, // buffer size
                    VoxelMapPipelines.WAYPOINT_ICON_NO_DEPTHTEST_PIPELINE,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false))
                            .createCompositeState(false))));

    public static final RenderType WAYPOINT_TEXT_BACKGROUND = RenderType.create(
            "voxelmap_beacon_text_background",
            0x00C000, // buffer size
            VoxelMapPipelines.WAYPOINT_TEXT_BACKGROUND_PIPELINE,
            RenderType.CompositeState.builder()
                    .createCompositeState(false));
}
