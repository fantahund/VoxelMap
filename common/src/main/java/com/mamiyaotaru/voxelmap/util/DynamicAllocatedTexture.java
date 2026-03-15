package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class DynamicAllocatedTexture extends AbstractTexture {
    public DynamicAllocatedTexture(GpuTexture texture) {
        setTexture(texture);
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
        GpuTextureView textureView = RenderSystem.getDevice().createTextureView(texture);

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