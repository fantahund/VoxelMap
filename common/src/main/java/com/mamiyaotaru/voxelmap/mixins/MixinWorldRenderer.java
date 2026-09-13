package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.rendering.RenderUtils;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinWorldRenderer {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderLevel(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        Matrix4fStack matrixStack = RenderUtils.getMatrixStack();
        matrixStack.pushMatrix();
        matrixStack.set(modelViewMatrix);
        VoxelConstants.onRenderWaypoints(matrixStack, Minecraft.getInstance().gameRenderer.mainCamera(), Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        matrixStack.popMatrix();
    }
}
