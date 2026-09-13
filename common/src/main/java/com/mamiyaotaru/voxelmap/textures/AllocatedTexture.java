package com.mamiyaotaru.voxelmap.textures;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class AllocatedTexture extends AbstractTexture implements VoxelMapTexture {
    public AllocatedTexture(GpuTexture texture) {
        this(texture, RenderSystem.getDevice().createTextureView(texture));
    }

    public AllocatedTexture(GpuTexture texture, GpuTextureView textureView) {
        this.texture = texture;
        this.textureView = textureView;
    }

    @Override
    public void setSampler(GpuSampler sampler) {
        this.sampler = sampler;
    }
}
