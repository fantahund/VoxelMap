package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Shadow
    private Framebuffer translucentFramebuffer;

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "render", at = @At("RETURN"))
    private void postRender(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        if (VoxelMap.mapOptions.waypointsAllowed && (VoxelConstants.getVoxelMapInstance().getMapOptions().showBeacons || VoxelConstants.getVoxelMapInstance().getMapOptions().showWaypoints)) {
            if (VoxelConstants.isFabulousGraphicsOrBetter()) {
                Framebuffer framebuffer = VoxelConstants.getMinecraft().getFramebuffer();
                GlStateManager._glBindFramebuffer(OpenGL.GL30_GL_READ_FRAMEBUFFER, this.translucentFramebuffer.fbo);
                GlStateManager._glBindFramebuffer(OpenGL.GL30_GL_DRAW_FRAMEBUFFER, framebuffer.fbo);
                GlStateManager._glBlitFrameBuffer(0, 0, this.translucentFramebuffer.textureWidth, this.translucentFramebuffer.textureHeight, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight, 256, OpenGL.GL11_GL_NEAREST);
            }

            boolean drawSignForeground = !VoxelConstants.isFabulousGraphicsOrBetter();
            Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
            try {
                matrixStack.pushMatrix();
                matrixStack.mul(matrix4f);
                RenderSystem.applyModelViewMatrix();
                FabricModVoxelMap.onRenderHand(tickCounter.getTickDelta(false), new Matrix4fStack(5), VoxelConstants.getVoxelMapInstance().getMapOptions().showBeacons, VoxelConstants.getVoxelMapInstance().getMapOptions().showWaypoints, drawSignForeground, true);
            } finally {
                matrixStack.popMatrix();
            }
        }

    }

    @Inject(method = "renderLayer", at = @At("RETURN"))
    private void postRenderLayer(RenderLayer renderLayer, double x, double y, double z, Matrix4f matrix4f, Matrix4f positionMatrix, CallbackInfo ci) {
        if (VoxelConstants.isFabulousGraphicsOrBetter() && VoxelConstants.getVoxelMapInstance().getMapOptions().showWaypoints && renderLayer == RenderLayer.getTranslucent() && VoxelConstants.getMinecraft().worldRenderer.getTranslucentFramebuffer() != null) {
            VoxelConstants.getMinecraft().worldRenderer.getTranslucentFramebuffer().beginWrite(false);
            Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
            try {
                matrixStack.pushMatrix();
                matrixStack.mul(matrix4f);
                RenderSystem.applyModelViewMatrix();
                FabricModVoxelMap.onRenderHand(VoxelConstants.getMinecraft().getRenderTickCounter().getTickDelta(false), new Matrix4fStack(5), false, true, true, false);
            } finally {
                matrixStack.popMatrix();
            }
            VoxelConstants.getMinecraft().getFramebuffer().beginWrite(false);
        }

    }

    @Inject(method = "scheduleChunkRender(IIIZ)V", at = @At("RETURN"))
    public void postScheduleChunkRender(int x, int y, int z, boolean important, CallbackInfo ci) {
        VoxelConstants.getVoxelMapInstance().getWorldUpdateListener().notifyObservers(x, z);
    }
}
