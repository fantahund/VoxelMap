package com.mamiyaotaru.voxelmap.rendering;

import java.util.function.Function;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public class VoxelMapRenderTypes {
    public static final Function<Identifier, RenderType> GUI_TEXTURED_GEQUAL_DEPTH = Util.memoize(
            identifier -> RenderType.create(
                    "voxelmap_gui_textured_gequal_gepth",
                    RenderSetup.builder(VoxelMapPipelines.GUI_TEXTURED_GEQUAL_DEPTH)
                            .withTexture("Sampler0", identifier)
                            .createRenderSetup()
            )
    );

    public static final Function<Identifier, RenderType> GUI_TEXTURED_ANY_DEPTH = Util.memoize(
            identifier -> RenderType.create(
                    "voxelmap_gui_textured_any_depth",
                    RenderSetup.builder(VoxelMapPipelines.GUI_TEXTURED_ANY_DEPTH)
                            .withTexture("Sampler0", identifier)
                            .createRenderSetup()
            )
    );

    public static final Function<Identifier, RenderType> GUI_TEXTURED_ANY_DEPTH_MASKED = Util.memoize(
            identifier -> RenderType.create(
                    "voxelmap_gui_textured_masked_any_depth_masked",
                    RenderSetup.builder(VoxelMapPipelines.GUI_TEXTURED_ANY_DEPTH_MASKED)
                            .withTexture("Sampler0", identifier)
                            .createRenderSetup()
            )
    );

    public static final RenderType WAYPOINT_TEXT_BACKGROUND = RenderType.create(
            "voxelmap_waypoint_text_background",
            RenderSetup.builder(VoxelMapPipelines.WAYPOINT_TEXT_BACKGROUND)
                    .createRenderSetup()
    );
}
