package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPipelines.class)
public class MixinRenderPipelines {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void voxelmap_onRegisterPipelines(CallbackInfo ci) {
        if (VoxelConstants.getModApiBridge().isModEnabled("iris")) {
            VoxelConstants.getLogger().info("Register Voxelmap Pipelines to Iris");
            //IrisApi.getInstance().assignPipeline(GLUtils.GUI_TEXTURED_EQUAL_DEPTH_PIPELINE, IrisProgram.BASIC);
            //IrisApi.getInstance().assignPipeline(GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE, IrisProgram.BASIC);
            IrisApi.getInstance().assignPipeline(GLUtils.WAYPOINT_BEAM_PIPELINE, IrisProgram.BASIC);
            IrisApi.getInstance().assignPipeline(GLUtils.WAYPOINT_ICON_DEPTHTEST_PIPELINE, IrisProgram.TEXTURED);
            IrisApi.getInstance().assignPipeline(GLUtils.WAYPOINT_ICON_NO_DEPTHTEST_PIPELINE, IrisProgram.TEXTURED);
            IrisApi.getInstance().assignPipeline(GLUtils.WAYPOINT_TEXT_BACKGROUND_PIPELINE, IrisProgram.BASIC);
            //IrisApi.getInstance().assignPipeline(GLUtils.ENTITY_ICON, IrisProgram.TEXTURED);
        }
    }
}
