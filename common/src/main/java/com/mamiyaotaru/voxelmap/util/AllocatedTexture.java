package com.mamiyaotaru.voxelmap.util;

import net.minecraft.client.renderer.texture.AbstractTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.systems.RenderSystem;

public class AllocatedTexture extends AbstractTexture {
    public AllocatedTexture(GpuTexture texture) {
        this.texture = texture;
        this.textureView = RenderSystem.getDevice().createTextureView(texture);
    }
}
