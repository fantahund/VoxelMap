package com.mamiyaotaru.voxelmap.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;

public class VoxelMapSamplers {
    public static final GpuSampler NEAREST_CLAMP = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

    public static final GpuSampler NEAREST_REPEAT = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);

    public static final GpuSampler LINEAR_CLAMP = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

    public static final GpuSampler LINEAR_REPEAT = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
}
