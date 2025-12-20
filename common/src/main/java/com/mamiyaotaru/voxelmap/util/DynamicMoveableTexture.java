package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.system.MemoryUtil;

public class DynamicMoveableTexture extends DynamicTexture {
    private final Object bufferLock = new Object();

    public DynamicMoveableTexture(int width, int height, boolean clear) {
        super(width, height, clear);
    }

    public int getWidth() {
        return this.getPixelsRGBA().getWidth();
    }

    public int getHeight() {
        return this.getPixelsRGBA().getHeight();
    }

    public int getIndex() {
        return ((GlTexture) this.getId()).glId();
    }

    public void moveX(int offset) {
        synchronized (this.bufferLock) {
            // TODO: 1.20.1 Port - getPointer() may not exist in 1.20.1 NativeImage API
            // This method needs to be rewritten for 1.20.1 compatibility
            // For now, commenting out to allow compilation
            /* Commented out for compilation - needs 1.20.1 compatible implementation
            long pointer = this.getPixelsRGBA().getPointer();
            int size = this.getWidth() * this.getHeight() * 4;
            if (offset > 0) {
                MemoryUtil.memCopy(pointer + (offset * 4L), pointer, size - offset * 4L);
            } else if (offset < 0) {
                MemoryUtil.memCopy(pointer, pointer - (offset * 4L), size + offset * 4L);
            }
            */
        }
    }

    public void moveY(int offset) {
        synchronized (this.bufferLock) {
            // TODO: 1.20.1 Port - getPointer() may not exist in 1.20.1 NativeImage API
            // This method needs to be rewritten for 1.20.1 compatibility
            // For now, commenting out to allow compilation
            /* Commented out for compilation - needs 1.20.1 compatible implementation
            long pointer = this.getPixelsRGBA().getPointer();
            int size = this.getPixelsRGBA().getHeight() * this.getPixelsRGBA().getWidth() * 4;
            int width = this.getPixelsRGBA().getWidth();
            if (offset > 0) {
                MemoryUtil.memCopy(pointer + ((long) offset * width * 4), pointer, size - (long) offset * width * 4);
            } else if (offset < 0) {
                MemoryUtil.memCopy(pointer, pointer - ((long) offset * width * 4), size + (long) offset * width * 4);
            }
            */
        }
    }

    public void setRGB(int x, int y, int color24) {
        int alpha = color24 >> 24 & 0xFF;
        byte a = -1;
        byte r = (byte) ((color24 & 0xFF) * alpha / 255);
        byte g = (byte) ((color24 >> 8 & 0xFF) * alpha / 255);
        byte b = (byte) ((color24 >> 16 & 0xFF) * alpha / 255);
        int color = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
        this.getPixelsRGBA().setPixel(x, y, color);
    }
}
