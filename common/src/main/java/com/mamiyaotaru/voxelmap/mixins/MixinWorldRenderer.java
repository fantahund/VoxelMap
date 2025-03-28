package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
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

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void postRender(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        // if (VoxelMap.mapOptions.waypointsAllowed && (VoxelConstants.getVoxelMapInstance().getMapOptions().showBeacons || VoxelConstants.getVoxelMapInstance().getMapOptions().showWaypoints)) {
        // boolean drawSignForeground = !VoxelConstants.isFabulousGraphicsOrBetter() || this.getTranslucentTarget() == null;
        // if (!drawSignForeground) {
        // RenderTarget framebuffer = VoxelConstants.getMinecraft().getMainRenderTarget();
        // GlStateManager._glBindFramebuffer(OpenGL.GL30_GL_READ_FRAMEBUFFER, this.getTranslucentTarget().frameBufferId);
        // GlStateManager._glBindFramebuffer(OpenGL.GL30_GL_DRAW_FRAMEBUFFER, framebuffer.frameBufferId);
        // GlStateManager._glBlitFrameBuffer(0, 0, this.getTranslucentTarget().width, this.getTranslucentTarget().height, 0, 0, framebuffer.width, framebuffer.height, 256, OpenGL.GL11_GL_NEAREST);
        // }
        //
        // Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
        // try {
        // matrixStack.pushMatrix();
        // matrixStack.mul(matrix4f);
        // VoxelConstants.onRenderHand(deltaTracker.getGameTimeDeltaPartialTick(false), new Matrix4fStack(5), VoxelConstants.getVoxelMapInstance().getMapOptions().showBeacons, VoxelConstants.getVoxelMapInstance().getMapOptions().showWaypoints, drawSignForeground, true);
        // } finally {
        // matrixStack.popMatrix();
        // }
        // }
    }

    @Inject(method = "renderSectionLayer", at = @At("RETURN"))
    private void postRenderLayer(RenderType renderLayer, double x, double y, double z, Matrix4f matrix4f, Matrix4f positionMatrix, CallbackInfo ci) {
        // if (VoxelConstants.isFabulousGraphicsOrBetter() && VoxelConstants.getVoxelMapInstance().getMapOptions().showWaypoints && renderLayer == RenderType.translucent() && VoxelConstants.getMinecraft().levelRenderer.getTranslucentTarget() != null) {
        // VoxelConstants.getMinecraft().levelRenderer.getTranslucentTarget().bindWrite(false);
        // Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
        // try {
        // matrixStack.pushMatrix();
        // matrixStack.mul(matrix4f);
        // VoxelConstants.onRenderHand(VoxelConstants.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false), new Matrix4fStack(5), false, true, true, false);
        // } finally {
        // matrixStack.popMatrix();
        // }
        // VoxelConstants.getMinecraft().getMainRenderTarget().bindWrite(false);
        // }

    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("RETURN"))
    public void postScheduleChunkRender(int x, int y, int z, boolean important, CallbackInfo ci) {
        if (VoxelConstants.getVoxelMapInstance().getWorldUpdateListener() != null) {
            VoxelConstants.getVoxelMapInstance().getWorldUpdateListener().notifyObservers(x, z);
        }
    }
}
