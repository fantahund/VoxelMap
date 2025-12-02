package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.CompressionUtils;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.UUID;
import java.util.zip.DataFormatException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.Level;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

public class CompressibleGLBufferedImage {
    private static final HashMap<Integer, ByteBuffer> byteBuffers = new HashMap<>(4);
    private static final int DEFAULT_SIZE = 256;
    private static final ByteBuffer defaultSizeBuffer = ByteBuffer.allocateDirect(DEFAULT_SIZE * DEFAULT_SIZE * 4).order(ByteOrder.nativeOrder());

    private byte[] bytes;
    private final int width;
    private final int height;
    private final Object bufferLock = new Object();
    private boolean isCompressed;
    private final boolean compressNotDelete;
    private final Identifier location = Identifier.fromNamespaceAndPath("voxelmap", "mapimage/" + UUID.randomUUID());
    private DynamicTexture texture;

    public CompressibleGLBufferedImage(int width, int height, int imageType) {
        this.width = width;
        this.height = height;
        this.bytes = new byte[width * height * 4];
        this.compressNotDelete = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions().outputImages;
    }

    public byte[] getData() {
        if (this.isCompressed) {
            this.decompress();
        }

        return this.bytes;
    }

    public Identifier getTextureLocation() {
        return this.texture != null ? this.location : null;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void deleteTexture() {
        if (!RenderSystem.isOnRenderThread()) {
            VoxelConstants.getLogger().log(Level.WARN, "Texture unload call from wrong thread", new Exception());
            return;
        }
        if (this.texture != null) {
            Minecraft.getInstance().getTextureManager().release(location);
            this.texture = null;
        }
    }

    public void uploadToTexture() {
        if (!RenderSystem.isOnRenderThread()) {
            VoxelConstants.getLogger().log(Level.WARN, "Texture upload call from wrong thread", new Exception());
            return;
        }

        if (this.isCompressed) {
            this.decompress();
        }

        if (this.texture == null) {
            this.texture = new DynamicTexture(() -> "", new NativeImage(Format.RGBA, width, height, false));
            this.texture.setClamp(true);
            Minecraft.getInstance().getTextureManager().register(location, texture);
        }

        ByteBuffer buffer = byteBuffers.get(this.width * this.height);
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(this.width * this.height * 4).order(ByteOrder.nativeOrder());
            byteBuffers.put(this.width * this.height, buffer);
        }
        buffer.clear();
        synchronized (this.bufferLock) {
            buffer.put(this.bytes);
            buffer.position(0).limit(this.bytes.length);
        }

        int imageBytes = width * height * this.texture.getPixels().format().components();
        ByteBuffer outBuffer = MemoryUtil.memByteBuffer(this.texture.getPixels().getPointer(), imageBytes);
        MemoryUtil.memCopy(buffer, outBuffer);
        this.texture.upload();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, ((GlTexture) this.texture.getTexture()).glId());
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        this.compress();
    }

    public void setRGB(int x, int y, int color) {
        if (this.isCompressed) {
            this.decompress();
        }

        int index = (x + y * this.getWidth()) * 4;
        synchronized (this.bufferLock) {
            int alpha = color >> 24 & 0xFF;
            this.bytes[index + 0] = (byte) ((color >> 16 & 0xFF) * alpha / 255);
            this.bytes[index + 1] = (byte) ((color >> 8 & 0xFF) * alpha / 255);
            this.bytes[index + 2] = (byte) ((color & 0xFF) * alpha / 255);
            this.bytes[index + 3] = -1;
        }
    }

    private synchronized void compress() {
        if (!this.isCompressed) {
            if (this.compressNotDelete) {
                this.bytes = CompressionUtils.compress(this.bytes);
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
                } catch (DataFormatException ignored) {
                }
            } else {
                this.bytes = new byte[this.width * this.height * 4];
                this.isCompressed = false;
            }

        }
    }

    static {
        byteBuffers.put(DEFAULT_SIZE * DEFAULT_SIZE, defaultSizeBuffer);
    }
}
