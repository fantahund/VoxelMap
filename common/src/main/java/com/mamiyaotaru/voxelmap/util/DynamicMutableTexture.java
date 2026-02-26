package com.mamiyaotaru.voxelmap.util;

import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.system.MemoryUtil;

public class DynamicMutableTexture extends DynamicTexture {
    private final Object bufferLock = new Object();

    public DynamicMutableTexture(String label, int width, int height, boolean clear) {
        super(label, width, height, clear);
    }

    public int getWidth() {
        return this.getPixels().getWidth();
    }

    public int getHeight() {
        return this.getPixels().getHeight();
    }

    public void moveX(int offset) {
        synchronized (this.bufferLock) {
            long pointer = this.getPixels().getPointer();
            int size = this.getWidth() * this.getHeight() * 4;
            if (offset > 0) {
                MemoryUtil.memCopy(pointer + (offset * 4L), pointer, size - offset * 4L);
            } else if (offset < 0) {
                MemoryUtil.memCopy(pointer, pointer - (offset * 4L), size + offset * 4L);
            }

        }
    }

    public void moveY(int offset) {
        synchronized (this.bufferLock) {
            long pointer = this.getPixels().getPointer();
            int size = this.getPixels().getHeight() * this.getPixels().getWidth() * 4;
            int width = this.getPixels().getWidth();
            if (offset > 0) {
                MemoryUtil.memCopy(pointer + ((long) offset * width * 4), pointer, size - (long) offset * width * 4);
            } else if (offset < 0) {
                MemoryUtil.memCopy(pointer, pointer - ((long) offset * width * 4), size + (long) offset * width * 4);
            }

        }
    }

    public void setRGB(int x, int y, int color24) {
        this.getPixels().setPixelABGR(x, y, ColorUtils.premultiplyWithAlpha(color24));
    }
}
