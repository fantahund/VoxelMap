package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class DynamicAllocatedTexture extends AbstractTexture {
    public DynamicAllocatedTexture(GpuTexture texture) {
        setTexture(texture);
    }

    public DynamicAllocatedTexture(GpuTexture texture, GpuTextureView textureView) {
        setTexture(texture, textureView);
    }

    public int getWidth(int i) {
        if (this.texture != null) {
            return this.texture.getWidth(i);
        }
        return 0;
    }

    public int getHeight(int i) {
        if (this.texture != null) {
            return this.texture.getHeight(i);
        }
        return 0;
    }

    public void setTexture(GpuTexture texture) {
        RenderSystem.assertOnRenderThread();
        setTexture(texture, RenderSystem.getDevice().createTextureView(texture));
    }

    public void setTexture(GpuTexture texture, GpuTextureView textureView) {
        if (this.texture != null) {
            this.texture.close();
        }
        if (this.textureView != null) {
            this.textureView.close();
        }

        this.texture = texture;
        this.textureView = textureView;
    }
}