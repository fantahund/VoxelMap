package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
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
    @Final
    @Shadow
    private BufferBuilderStorage bufferBuilders;

    @Inject(method = "render", at = @At("RETURN"))
    private void postRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix, CallbackInfo ci) {
        if (VoxelMap.getInstance().getMapOptions().showBeacons || VoxelMap.getInstance().getMapOptions().showWaypoints) {
            if (VoxelConstants.isFabulousGraphicsOrBetter()) {
                Framebuffer framebuffer = VoxelConstants.getMinecraft().getFramebuffer();
                GlStateManager._glBindFramebuffer(36008, this.translucentFramebuffer.fbo);
                GlStateManager._glBindFramebuffer(36009, framebuffer.fbo);
                GlStateManager._glBlitFrameBuffer(0, 0, this.translucentFramebuffer.textureWidth, this.translucentFramebuffer.textureHeight, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight, 256, GL11.GL_NEAREST);
            }

            boolean drawSignForeground = !VoxelConstants.isFabulousGraphicsOrBetter();
            FabricModVoxelMap.onRenderHand(tickDelta, limitTime, matrices, VoxelMap.getInstance().getMapOptions().showBeacons, VoxelMap.getInstance().getMapOptions().showWaypoints, drawSignForeground, true);
        }

    }

    @Inject(method = "renderLayer", at = @At("RETURN"))
    private void postRenderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double x, double y, double z, Matrix4f matrix4f, CallbackInfo ci) {
        if (VoxelConstants.isFabulousGraphicsOrBetter() && VoxelMap.getInstance().getMapOptions().showWaypoints && renderLayer == RenderLayer.getTranslucent() && VoxelConstants.getMinecraft().worldRenderer.getTranslucentFramebuffer() != null) {
            VoxelConstants.getMinecraft().worldRenderer.getTranslucentFramebuffer().beginWrite(false);
            FabricModVoxelMap.onRenderHand(VoxelConstants.getMinecraft().getTickDelta(), 0L, matrixStack, false, true, true, false);
            VoxelConstants.getMinecraft().getFramebuffer().beginWrite(false);
        }

    }

    @Inject(method = "scheduleChunkRender(IIIZ)V", at = @At("RETURN"))
    public void postScheduleChunkRender(int x, int y, int z, boolean dunno, CallbackInfo ci) {
        VoxelMap.instance.getWorldUpdateListener().notifyObservers(x, z);
    }
}
