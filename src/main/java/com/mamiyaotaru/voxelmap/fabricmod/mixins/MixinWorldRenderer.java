package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
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
    private BufferBuilderStorage bufferBuilders;

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;FJZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lnet/minecraft/util/math/Matrix4f;)V", at = @At("RETURN"))
    private void postRender(MatrixStack matrixStack, float partialTicks, long timeSlice, boolean lookingAtBlock, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        if (VoxelMap.getInstance().getMapOptions().showBeacons || VoxelMap.getInstance().getMapOptions().showWaypoints) {
            if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
                GlStateManager._glBindFramebuffer(36008, this.translucentFramebuffer.fbo);
                GlStateManager._glBindFramebuffer(36009, framebuffer.fbo);
                GlStateManager._glBlitFrameBuffer(0, 0, this.translucentFramebuffer.textureWidth, this.translucentFramebuffer.textureHeight, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight, 256, 9728);
            }

            boolean drawSignForeground = !MinecraftClient.isFabulousGraphicsOrBetter();
            FabricModVoxelMap.onRenderHand(partialTicks, timeSlice, matrixStack, VoxelMap.getInstance().getMapOptions().showBeacons, VoxelMap.getInstance().getMapOptions().showWaypoints, drawSignForeground, true);
        }

    }

    @Inject(method = "renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDDLnet/minecraft/util/math/Matrix4f;)V", at = @At("RETURN"))
    private void postRenderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double x, double y, double z, Matrix4f matrix4f, CallbackInfo ci) {
        if (MinecraftClient.isFabulousGraphicsOrBetter() && VoxelMap.getInstance().getMapOptions().showWaypoints && renderLayer == RenderLayer.getTranslucent() && MinecraftClient.getInstance().worldRenderer.getTranslucentFramebuffer() != null) {
            MinecraftClient.getInstance().worldRenderer.getTranslucentFramebuffer().beginWrite(false);
            FabricModVoxelMap.onRenderHand(MinecraftClient.getInstance().getTickDelta(), 0L, matrixStack, false, true, true, false);
            MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
        }

    }

    @Inject(method = "scheduleChunkRender(IIIZ)V", at = @At("RETURN"))
    public void postScheduleChunkRender(int x, int y, int z, boolean dunno, CallbackInfo ci) {
        VoxelMap.instance.getWorldUpdateListener().notifyObservers(x, z);
    }
}
