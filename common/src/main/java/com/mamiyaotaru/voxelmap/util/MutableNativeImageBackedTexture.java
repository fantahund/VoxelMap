package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.system.MemoryUtil;

public class MutableNativeImageBackedTexture extends DynamicTexture {
    private final Object bufferLock = new Object();
    private final NativeImage image;
    private final long pointer;

    public MutableNativeImageBackedTexture(String label, int width, int height, boolean useStb) {
        super(label, width, height, useStb);
        this.image = this.getPixels();
        String info = this.image.toString();
        String pointerString = info.substring(info.indexOf('@') + 1, info.indexOf(']') - 1);
        this.pointer = Long.parseLong(pointerString);
    }

    public void write() {
        this.upload();
    }

    public int getWidth() {
        return this.image.getHeight();
    }

    public int getHeight() {
        return this.image.getHeight();
    }

    public int getIndex() {
        return ((GlTexture) this.getTexture()).glId();
    }

    public void moveX(int offset) {
        synchronized (this.bufferLock) {
            int size = this.image.getHeight() * this.image.getWidth() * 4;
            if (offset > 0) {
                MemoryUtil.memCopy(this.pointer + (offset * 4L), this.pointer, size - offset * 4L);
            } else if (offset < 0) {
                MemoryUtil.memCopy(this.pointer, this.pointer - (offset * 4L), size + offset * 4L);
            }

        }
    }

    public void moveY(int offset) {
        synchronized (this.bufferLock) {
            int size = this.image.getHeight() * this.image.getWidth() * 4;
            int width = this.image.getWidth();
            if (offset > 0) {
                MemoryUtil.memCopy(this.pointer + ((long) offset * width * 4), this.pointer, size - (long) offset * width * 4);
            } else if (offset < 0) {
                MemoryUtil.memCopy(this.pointer, this.pointer - ((long) offset * width * 4), size + (long) offset * width * 4);
            }

        }
    }

    public void setRGB(int x, int y, int color24) {
        int alpha = color24 >> 24 & 0xFF;
        byte a = -1;
        byte r = (byte) ((color24 & 0xFF) * alpha / 255);
        byte g = (byte) ((color24 >> 8 & 0xFF) * alpha / 255);
        byte b = (byte) ((color24 >> 16 & 0xFF) * alpha / 255);
        int color = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
        this.image.setPixel(x, y, color);
    }
}
