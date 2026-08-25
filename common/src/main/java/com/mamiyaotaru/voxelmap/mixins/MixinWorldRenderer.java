package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.rendering.AlwaysOnTopSubmitter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class MixinWorldRenderer {

    @Unique private final PoseStack voxelmap$poseStack = new PoseStack();

    @Inject(method = "submitFeatures", at = @At("RETURN"))
    private void submitFeatures(LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, boolean renderOutline, CallbackInfo ci) {
        AlwaysOnTopSubmitter.beginFrame();
        voxelmap$poseStack.pushPose();
        VoxelConstants.onRenderWaypoints(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false), voxelmap$poseStack, submitNodeCollector, Minecraft.getInstance().gameRenderer.mainCamera());

        voxelmap$poseStack.popPose();
    }

    @Inject(method = "frameHasAlwaysOnTopGizmos", at = @At("RETURN"), cancellable = true)
    private void voxelmap$includeSubmittedAlwaysOnTopFeatures(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValue() || AlwaysOnTopSubmitter.hasSubmissions());
    }
}
