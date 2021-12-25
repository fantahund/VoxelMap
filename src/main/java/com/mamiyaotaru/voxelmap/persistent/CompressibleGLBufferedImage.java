package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.interfaces.IGLBufferedImage;
import com.mamiyaotaru.voxelmap.util.CompressionUtils;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mojang.blaze3d.systems.RenderSystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.DataFormatException;

public class CompressibleGLBufferedImage implements IGLBufferedImage {
    private byte[] bytes;
    private int index = 0;
    private int width;
    private int height;
    private int imageType;
    private Object bufferLock = new Object();
    private boolean isCompressed = false;
    private static HashMap byteBuffers = new HashMap(4);
    private static ByteBuffer defaultSizeBuffer = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder());
    private boolean compressNotDelete = false;

    public CompressibleGLBufferedImage(int width, int height, int imageType) {
        this.width = width;
        this.height = height;
        this.imageType = imageType;
        this.bytes = new byte[width * height * 4];
        this.compressNotDelete = VoxelMap.getInstance().getPersistentMapOptions().outputImages;
    }

    public byte[] getData() {
        if (this.isCompressed) {
            this.decompress();
        }

        return this.bytes;
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void baleet() {
        int currentIndex = this.index;
        this.index = 0;
        if (currentIndex != 0 && RenderSystem.isOnRenderThreadOrInit()) {
            GLShim.glDeleteTextures(currentIndex);
        }

    }

    @Override
    public void write() {
        if (this.isCompressed) {
            this.decompress();
        }

        if (this.index == 0) {
            this.index = GLShim.glGenTextures();
        }

        ByteBuffer buffer = (ByteBuffer) byteBuffers.get(this.width * this.height);
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(this.width * this.height * 4).order(ByteOrder.nativeOrder());
            byteBuffers.put(this.width * this.height, buffer);
        }

        buffer.clear();
        synchronized (this.bufferLock) {
            buffer.put(this.bytes);
        }

        buffer.position(0).limit(this.bytes.length);
        GLShim.glBindTexture(3553, this.index);
        GLShim.glTexParameteri(3553, 10241, 9728);
        GLShim.glTexParameteri(3553, 10240, 9728);
        GLShim.glTexParameteri(3553, 10242, 33071);
        GLShim.glTexParameteri(3553, 10243, 33071);
        GLShim.glPixelStorei(3314, 0);
        GLShim.glPixelStorei(3316, 0);
        GLShim.glPixelStorei(3315, 0);
        GLShim.glTexImage2D(3553, 0, 6408, this.getWidth(), this.getHeight(), 0, 6408, 32821, buffer);
        GLShim.glGenerateMipmap(3553);
        this.compress();
    }

    @Override
    public void blank() {
        if (this.isCompressed) {
            this.decompress();
        }

        Arrays.fill(this.bytes, (byte) 0);
        this.write();
    }

    @Override
    public void setRGB(int x, int y, int color24) {
        if (this.isCompressed) {
            this.decompress();
        }

        int index = (x + y * this.getWidth()) * 4;
        synchronized (this.bufferLock) {
            int alpha = color24 >> 24 & 0xFF;
            this.bytes[index] = -1;
            this.bytes[index + 1] = (byte) ((color24 >> 0 & 0xFF) * alpha / 255);
            this.bytes[index + 2] = (byte) ((color24 >> 8 & 0xFF) * alpha / 255);
            this.bytes[index + 3] = (byte) ((color24 >> 16 & 0xFF) * alpha / 255);
        }
    }

    @Override
    public void moveX(int offset) {
        synchronized (this.bufferLock) {
            if (offset > 0) {
                System.arraycopy(this.bytes, offset * 4, this.bytes, 0, this.bytes.length - offset * 4);
            } else if (offset < 0) {
                System.arraycopy(this.bytes, 0, this.bytes, -offset * 4, this.bytes.length + offset * 4);
            }

        }
    }

    @Override
    public void moveY(int offset) {
        synchronized (this.bufferLock) {
            if (offset > 0) {
                System.arraycopy(this.bytes, offset * this.getWidth() * 4, this.bytes, 0, this.bytes.length - offset * this.getWidth() * 4);
            } else if (offset < 0) {
                System.arraycopy(this.bytes, 0, this.bytes, -offset * this.getWidth() * 4, this.bytes.length + offset * this.getWidth() * 4);
            }

        }
    }

    private synchronized void compress() {
        if (!this.isCompressed) {
            if (this.compressNotDelete) {
                try {
                    this.bytes = CompressionUtils.compress(this.bytes);
                } catch (IOException var2) {
                }
            } else {
                this.bytes = null;
            }

            this.isCompressed = true;
        }
    }

    private synchronized void decompress() {
        if (this.isCompressed) {
            if (this.compressNotDelete) {
                try {
                    this.bytes = CompressionUtils.decompress(this.bytes);
                } catch (IOException var2) {
                } catch (DataFormatException var3) {
                }
            } else {
                this.bytes = new byte[this.width * this.height * 4];
                this.isCompressed = false;
            }

        }
    }

    static {
        byteBuffers.put(65536, defaultSizeBuffer);
    }
}
