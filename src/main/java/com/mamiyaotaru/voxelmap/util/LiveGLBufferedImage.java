package com.mamiyaotaru.voxelmap.util;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class LiveGLBufferedImage extends GLBufferedImage {
    public LiveGLBufferedImage(int width, int height, int imageType) {
        super(width, height, imageType);
    }

    @Override
    public void write() {
        if (this.index == 0) {
            this.index = GLShim.glGenTextures();
        }

        this.buffer.clear();
        synchronized (this.bufferLock) {
            this.buffer.put(this.bytes);
        }

        this.buffer.position(0).limit(this.bytes.length);
        GLShim.glBindTexture(GL11.GL_TEXTURE_2D, this.index);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GLShim.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GLShim.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GLShim.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GLShim.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, this.getWidth(), this.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, this.buffer);
        GLShim.glGenerateMipmap(GL11.GL_TEXTURE_2D);
    }

    @Override
    public void setRGB(int x, int y, int color24) {
        int index = (x + y * this.getWidth()) * 4;
        synchronized (this.bufferLock) {
            int alpha = color24 >> 24 & 0xFF;
            this.bytes[index] = -1;
            this.bytes[index + 1] = (byte) ((color24 & 0xFF) * alpha / 255);
            this.bytes[index + 2] = (byte) ((color24 >> 8 & 0xFF) * alpha / 255);
            this.bytes[index + 3] = (byte) ((color24 >> 16 & 0xFF) * alpha / 255);
        }
    }
}
