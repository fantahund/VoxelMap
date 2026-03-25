package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class AllocatedTexture extends AbstractTexture {
    public AllocatedTexture(GpuTexture texture) {
        this(texture, RenderSystem.getDevice().createTextureView(texture));
    }

    public AllocatedTexture(GpuTexture texture, GpuTextureView textureView) {
        this.texture = texture;
        this.textureView = textureView;
    }
}
