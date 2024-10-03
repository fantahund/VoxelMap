package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
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

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void postRender(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        if (VoxelMap.mapOptions.waypointsAllowed && (VoxelConstants.getVoxelMapInstance().getMapOptions().showBeacons || VoxelConstants.getVoxelMapInstance().getMapOptions().showWaypoints)) {
            if (VoxelConstants.isFabulousGraphicsOrBetter()) {
                RenderTarget framebuffer = VoxelConstants.getMinecraft().getMainRenderTarget();
                GlStateManager._glBindFramebuffer(OpenGL.GL30_GL_READ_FRAMEBUFFER, this.getTranslucentTarget().frameBufferId);
                GlStateManager._glBindFramebuffer(OpenGL.GL30_GL_DRAW_FRAMEBUFFER, framebuffer.frameBufferId);
                GlStateManager._glBlitFrameBuffer(0, 0, this.getTranslucentTarget().width, this.getTranslucentTarget().height, 0, 0, framebuffer.width, framebuffer.height, 256, OpenGL.GL11_GL_NEAREST);
            }

            boolean drawSignForeground = !VoxelConstants.isFabulousGraphicsOrBetter();
            Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
            try {
                matrixStack.pushMatrix();
                matrixStack.mul(matrix4f);
                //1.21.2 RenderSystem.applyModelViewMatrix();
                VoxelConstants.onRenderHand(deltaTracker.getGameTimeDeltaPartialTick(false), new Matrix4fStack(5), VoxelConstants.getVoxelMapInstance().getMapOptions().showBeacons, VoxelConstants.getVoxelMapInstance().getMapOptions().showWaypoints, drawSignForeground, true);
            } finally {
                matrixStack.popMatrix();
            }
        }

    }

    @Inject(method = "renderSectionLayer", at = @At("RETURN"))
    private void postRenderLayer(RenderType renderLayer, double x, double y, double z, Matrix4f matrix4f, Matrix4f positionMatrix, CallbackInfo ci) {
        if (VoxelConstants.isFabulousGraphicsOrBetter() && VoxelConstants.getVoxelMapInstance().getMapOptions().showWaypoints && renderLayer == RenderType.translucent() && VoxelConstants.getMinecraft().levelRenderer.getTranslucentTarget() != null) {
            VoxelConstants.getMinecraft().levelRenderer.getTranslucentTarget().bindWrite(false);
            Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
            try {
                matrixStack.pushMatrix();
                matrixStack.mul(matrix4f);
                //1.21.2 RenderSystem.applyModelViewMatrix();
                VoxelConstants.onRenderHand(VoxelConstants.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false), new Matrix4fStack(5), false, true, true, false);
            } finally {
                matrixStack.popMatrix();
            }
            VoxelConstants.getMinecraft().getMainRenderTarget().bindWrite(false);
        }

    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("RETURN"))
    public void postScheduleChunkRender(int x, int y, int z, boolean important, CallbackInfo ci) {
        VoxelConstants.getVoxelMapInstance().getWorldUpdateListener().notifyObservers(x, z);
    }
}
