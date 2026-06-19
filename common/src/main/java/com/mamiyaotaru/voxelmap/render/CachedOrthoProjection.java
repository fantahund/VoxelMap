package com.mamiyaotaru.voxelmap.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;

public class CachedOrthoProjection {
    private final Projection projection;
    private final ProjectionMatrixBuffer matrix;

    public CachedOrthoProjection(String name, float zNear, float zFar, boolean invert) {
        projection = new Projection();
        projection.setupOrtho(zNear, zFar, 0.0F, 0.0F, invert);
        matrix = new ProjectionMatrixBuffer(name);
    }

    public GpuBufferSlice getBuffer(float width, float height) {
        projection.setSize(width, height);
        return matrix.getBuffer(projection);
    }
}
