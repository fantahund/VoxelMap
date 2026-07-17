package com.mamiyaotaru.voxelmap.rendering;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;

public class CachedProjectionMatrixBuffer {
    public static CachedProjectionMatrixBuffer perspective(String name, float zNear, float zFar, float fov) {
        Projection projection = new Projection();
        projection.setupPerspective(zNear, zFar, fov, 0.0F, 0.0F);

        return new CachedProjectionMatrixBuffer(name, projection);
    }

    public static CachedProjectionMatrixBuffer orthographic(String name, float zNear, float zFar, boolean invertY) {
        Projection projection = new Projection();
        projection.setupOrtho(zNear, zFar, 0.0F, 0.0F, invertY);

        return new CachedProjectionMatrixBuffer(name, projection);
    }

    private final Projection projection;
    private final ProjectionMatrixBuffer buffer;

    public CachedProjectionMatrixBuffer(String name, Projection projection) {
        this.projection = projection;
        buffer = new ProjectionMatrixBuffer(name);
    }

    public GpuBufferSlice getBuffer(float width, float height) {
        if (projection.width() != width || projection.height() != height) {
            projection.setSize(width, height);
        }
        return buffer.getBuffer(projection);
    }
}
