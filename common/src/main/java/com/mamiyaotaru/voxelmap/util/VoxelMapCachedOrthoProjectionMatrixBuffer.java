package com.mamiyaotaru.voxelmap.util;

import java.nio.ByteBuffer;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * See {@link net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer}
 */
public class VoxelMapCachedOrthoProjectionMatrixBuffer implements AutoCloseable {
	private final GpuBuffer buffer;
	private final GpuBufferSlice bufferSlice;

	public VoxelMapCachedOrthoProjectionMatrixBuffer(String string, float left, float right, float bottom, float top, float zNear, float zFar) {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		this.buffer = gpuDevice.createBuffer(() -> "Projection matrix UBO " + string, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
		this.bufferSlice = this.buffer.slice(0, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
		
		Matrix4f matrix4f = new Matrix4f().ortho(left, right, bottom, top, zNear, zFar);

		try (MemoryStack memoryStack = MemoryStack.stackPush()) {
			ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE)
					.putMat4f(matrix4f).get();
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), byteBuffer);
		}
	}

	public GpuBufferSlice getBuffer() {
		return this.bufferSlice;
	}

	public void close() {
		this.buffer.close();
	}
}
