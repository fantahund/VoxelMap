package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

public class VoxelMapRenderTarget extends RenderTarget {
    public final Identifier colorTextureId;
    public final Identifier depthTextureId;
    private AllocatedTexture allocatedColorTexture;
    private AllocatedTexture allocatedDepthTexture;

    public VoxelMapRenderTarget(Identifier baseId) {
        super(baseId.toString(), true);

        colorTextureId = baseId.withSuffix("_color");
        depthTextureId = baseId.withSuffix("_depth");
    }

    @Override
    public void createBuffers(int w, int h) {
        super.createBuffers(w, h);

        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        allocatedColorTexture = new AllocatedTexture(colorTexture, colorTextureView);
        allocatedDepthTexture = new AllocatedTexture(depthTexture, depthTextureView);
        textureManager.register(colorTextureId, allocatedColorTexture);
        textureManager.register(depthTextureId, allocatedDepthTexture);
    }

    @Override
    public void destroyBuffers() {
        super.destroyBuffers();

        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        if (allocatedColorTexture != null) {
            textureManager.release(colorTextureId);
            allocatedColorTexture = null;
        }
        if (allocatedDepthTexture != null) {
            textureManager.release(depthTextureId);
            allocatedDepthTexture = null;
        }
    }
}
