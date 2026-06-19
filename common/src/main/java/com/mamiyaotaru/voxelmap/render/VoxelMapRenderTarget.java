package com.mamiyaotaru.voxelmap.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;

public class VoxelMapRenderTarget extends RenderTarget {
    public VoxelMapRenderTarget(String string, boolean depth, GpuFormat format) {
        super(string, depth, format);
    }
}
