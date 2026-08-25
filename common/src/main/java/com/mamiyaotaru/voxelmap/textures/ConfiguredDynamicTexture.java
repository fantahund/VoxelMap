package com.mamiyaotaru.voxelmap.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.renderpearl.api.textures.GpuSampler;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.DynamicTexture;

public final class ConfiguredDynamicTexture extends DynamicTexture {
    public ConfiguredDynamicTexture(Supplier<String> label, NativeImage image, GpuSampler sampler) {
        super(label, image);
        this.sampler = sampler;
    }
}
