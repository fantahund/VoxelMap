package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinWorldRenderer {


    @Shadow @Nullable public abstract RenderTarget getTranslucentTarget();

    @Inject(method = "renderEntities", at = @At("RETURN"))
    private void renderEntities(PoseStack poseStack, BufferSource bufferSource, Camera camera, DeltaTracker deltaTracker, List<Entity> list, CallbackInfo ci) {
        VoxelConstants.onRenderWaypoints(deltaTracker.getGameTimeDeltaPartialTick(false), poseStack, bufferSource, camera);
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("RETURN"))
    public void postScheduleChunkRender(int x, int y, int z, boolean important, CallbackInfo ci) {
        if (VoxelConstants.getVoxelMapInstance().getWorldUpdateListener() != null) {
            VoxelConstants.getVoxelMapInstance().getWorldUpdateListener().notifyObservers(x, z);
        }
    }
}
