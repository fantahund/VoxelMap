package com.mamiyaotaru.voxelmap.rendering;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.AllocatedTexture;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

public class VoxelMapRenderTarget extends RenderTarget {
    public final Identifier colorTexId;
    public final Identifier depthTexId;
    private AllocatedTexture colorTex;
    private AllocatedTexture depthTex;

    public VoxelMapRenderTarget(String path, GpuFormat format, int width, int height) {
        this(path, format);
        createBuffers(width, height);
    }

    public VoxelMapRenderTarget(String path, GpuFormat format) {
        super(path, true, format);
        colorTexId = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "render/" + path + "_color");
        depthTexId = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "render/" + path + "_depth");
    }

    @Override
    public void createBuffers(int width, int height) {
        super.createBuffers(width, height);
        colorTex = new AllocatedTexture(colorTexture, colorTextureView);
        depthTex = new AllocatedTexture(depthTexture, depthTextureView);
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        textureManager.register(colorTexId, colorTex);
        textureManager.register(depthTexId, depthTex);
    }

    @Override
    public void destroyBuffers() {
        super.destroyBuffers();
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        if (colorTex != null) {
            textureManager.release(colorTexId);
            colorTex = null;
        }
        if (depthTex != null) {
            textureManager.release(depthTexId);
            depthTex = null;
        }
    }
}
