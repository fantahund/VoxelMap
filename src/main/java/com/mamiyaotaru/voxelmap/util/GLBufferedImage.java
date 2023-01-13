package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.interfaces.IGLBufferedImage;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class GLBufferedImage extends BufferedImage implements IGLBufferedImage {
    protected final ByteBuffer buffer;
    protected final byte[] bytes;
    protected int index;
    protected final Object bufferLock = new Object();

    public GLBufferedImage(int width, int height, int imageType) {
        super(width, height, imageType);
        this.bytes = ((DataBufferByte) this.getRaster().getDataBuffer()).getData();
        this.buffer = ByteBuffer.allocateDirect(this.bytes.length).order(ByteOrder.nativeOrder());
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public void baleet() {
        int currentIndex = this.index;
        this.index = 0;
        if (currentIndex != 0) {
            OpenGL.glDeleteTexture(currentIndex);
        }

    }

    @Override
    public void write() {
        if (this.index == 0) {
            this.index = OpenGL.glGenTextures();
        }

        this.buffer.clear();
        synchronized (this.bufferLock) {
            this.buffer.put(this.bytes);
        }

        this.buffer.position(0).limit(this.bytes.length);
        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, this.index);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_NEAREST);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_NEAREST);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_S, OpenGL.GL12_GL_CLAMP_TO_EDGE);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_T, OpenGL.GL12_GL_CLAMP_TO_EDGE);
        OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_ROW_LENGTH, 0);
        OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_SKIP_PIXELS, 0);
        OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_SKIP_ROWS, 0);
        OpenGL.glTexImage2D(OpenGL.GL11_GL_TEXTURE_2D, 0, OpenGL.GL11_GL_RGBA, this.getWidth(), this.getHeight(), 0, OpenGL.GL11_GL_RGBA, OpenGL.GL12_GL_UNSIGNED_INT_8_8_8_8, this.buffer);
    }

    @Override
    public void blank() {
        Arrays.fill(this.bytes, (byte) 0);
        this.write();
    }

    @Override
    public void setRGB(int x, int y, int rgb) {
        int index = (x + y * this.getWidth()) * 4;
        synchronized (this.bufferLock) {
            this.bytes[index] = (byte) (rgb >> 24);
            this.bytes[index + 1] = (byte) (rgb);
            this.bytes[index + 2] = (byte) (rgb >> 8);
            this.bytes[index + 3] = (byte) (rgb >> 16);
        }
    }

    @Override
    public void moveX(int x) {
        synchronized (this.bufferLock) {
            if (x > 0) {
                System.arraycopy(this.bytes, x * 4, this.bytes, 0, this.bytes.length - x * 4);
            } else if (x < 0) {
                System.arraycopy(this.bytes, 0, this.bytes, -x * 4, this.bytes.length + x * 4);
            }

        }
    }

    @Override
    public void moveY(int y) {
        synchronized (this.bufferLock) {
            if (y > 0) {
                System.arraycopy(this.bytes, y * this.getWidth() * 4, this.bytes, 0, this.bytes.length - y * this.getWidth() * 4);
            } else if (y < 0) {
                System.arraycopy(this.bytes, 0, this.bytes, -y * this.getWidth() * 4, this.bytes.length + y * this.getWidth() * 4);
            }

        }
    }
}
