package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinWorldRenderer {

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void renderLevel(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        VoxelConstants.onRenderWaypoints(deltaTracker.getGameTimeDeltaPartialTick(false), new Matrix4f(modelViewMatrix), Minecraft.getInstance().gameRenderer.getMainCamera());
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("RETURN"))
    public void postScheduleChunkRender(int x, int y, int z, boolean important, CallbackInfo ci) {
        if (VoxelConstants.getVoxelMapInstance().getWorldUpdateListener() != null) {
            VoxelConstants.getVoxelMapInstance().getWorldUpdateListener().notifyObservers(x, z);
        }
    }
}
