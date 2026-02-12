package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class RegisterableGPUTexture extends AbstractTexture {
    public RegisterableGPUTexture() {
    }

    public RegisterableGPUTexture(GpuTexture texture) {
        setTexture(texture);
    }

    public RegisterableGPUTexture(GpuTexture texture, GpuTextureView textureView) {
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