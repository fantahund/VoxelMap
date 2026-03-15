package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinWorldRenderer {

    @Shadow @Final private Minecraft minecraft;
    @Unique private final PoseStack voxelmap$poseStack = new PoseStack();

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void renderLevel(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4f modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        voxelmap$poseStack.pushPose();
        voxelmap$poseStack.last().pose().mul(modelViewMatrix);
        BufferSource bufferSource = VoxelConstants.getMinecraft().renderBuffers().bufferSource();
        VoxelConstants.onRenderWaypoints(deltaTracker.getGameTimeDeltaPartialTick(false), voxelmap$poseStack, bufferSource, minecraft.gameRenderer.getMainCamera());

        voxelmap$poseStack.popPose();
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("RETURN"))
    public void postScheduleChunkRender(int sectionX, int sectionY, int sectionZ, boolean playerChanged, CallbackInfo ci) {
        if (VoxelConstants.getVoxelMapInstance().getWorldUpdateListener() != null) {
            VoxelConstants.getVoxelMapInstance().getWorldUpdateListener().notifyObservers(sectionX, sectionZ);
        }
    }
}
