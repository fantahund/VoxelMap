package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class AllocatedTexture extends AbstractTexture {
    public AllocatedTexture(GpuTexture texture) {
        this.texture = texture;
        // TODO: 1.20.1 Port - RenderSystem.getDevice() doesn't exist in 1.20.1
        // this.textureView = RenderSystem.getDevice().createTextureView(texture);
    }
}
