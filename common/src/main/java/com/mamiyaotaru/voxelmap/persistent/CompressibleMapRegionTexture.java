package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.CompressionUtils;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import java.util.UUID;
import java.util.zip.DataFormatException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.Level;
import org.lwjgl.system.MemoryUtil;

public class CompressibleMapRegionTexture extends AbstractTexture {
    private NativeImage pixels;
    private NativeImage[] pixelsMipmapped;
    private int imageSize;
    private volatile int uploadedSize;
    private boolean registered;

    private boolean retainCompressedPixels;
    private boolean contentValid;
    private final Identifier location = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "mapimage/" + UUID.randomUUID());

    private final GpuSampler samplerSmall;
    private final GpuSampler samplerLarge;

    private byte[] bytes;

    public CompressibleMapRegionTexture() {
        this(CachedRegion.REGION_WIDTH);
    }

    CompressibleMapRegionTexture(int imageSize) {
        this.retainCompressedPixels = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions().outputImages;
        validateImageSize(imageSize);
        this.imageSize = imageSize;
        this.pixels = new NativeImage(imageSize, imageSize, false);
        this.samplerSmall = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, true);
        this.samplerLarge = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, true);
        this.sampler = samplerLarge;
    }

    public synchronized NativeImage getData() {
        if (pixels == null) {
            this.decompress();
        }
        return pixels;
    }

    synchronized void enableRetainedBacking() {
        this.retainCompressedPixels = true;
    }

    synchronized boolean canPartiallyUpdate() {
        if (this.contentValid && this.pixels == null && this.bytes != null) {
            this.decompress();
        }
        return this.contentValid && this.pixels != null;
    }

    synchronized void markContentValid() {
        this.contentValid = true;
    }

    synchronized int getImageSize() {
        return this.imageSize;
    }

    int getDisplaySize() {
        int uploaded = this.uploadedSize;
        return uploaded == 0 ? this.imageSize : uploaded;
    }

    synchronized void prepareSize(int imageSize) {
        validateImageSize(imageSize);
        if (this.imageSize == imageSize && this.pixels != null) {
            return;
        }
        clearCpuImages();
        this.imageSize = imageSize;
        this.pixels = new NativeImage(imageSize, imageSize, false);
        this.bytes = null;
        this.contentValid = false;
    }

    synchronized void replacePixels(int imageSize, byte[] rawPixels) {
        validateImageSize(imageSize);
        if (rawPixels.length != imageSize * imageSize * 4) {
            throw new IllegalArgumentException("Invalid image size, expected " + (imageSize * imageSize * 4) + ", got " + rawPixels.length);
        }
        clearCpuImages();
        this.imageSize = imageSize;
        this.pixels = new NativeImage(imageSize, imageSize, false);
        MemoryUtil.memByteBuffer(this.pixels.getPointer(), rawPixels.length).put(rawPixels);
        this.bytes = null;
        this.contentValid = true;
    }

    synchronized byte[] copyMipLevelBytes(int level) {
        if (this.pixels == null) {
            this.decompress();
        }
        if (this.pixelsMipmapped == null || level < 0 || level >= this.pixelsMipmapped.length) {
            throw new IllegalStateException("Mipmap level " + level + " has not been generated");
        }
        NativeImage mip = this.pixelsMipmapped[level];
        byte[] result = new byte[mip.getWidth() * mip.getHeight() * 4];
        MemoryUtil.memByteBuffer(mip.getPointer(), result.length).get(result);
        return result;
    }

    public Identifier getTextureLocation(float zoom) {
        if (zoom < 2) {
            this.sampler = samplerSmall;
        } else {
            this.sampler = samplerLarge;
        }
        return texture != null ? this.location : null;
    }

    public void deleteTexture() {
        if (!RenderSystem.isOnRenderThread()) {
            VoxelConstants.getLogger().log(Level.WARN, "Texture unload call from wrong thread", new Exception());
            return;
        }
        if (registered) {
            Minecraft.getInstance().getTextureManager().release(location);
            registered = false;
        }
        close();
    }

    public void uploadToTexture() {
        if (!RenderSystem.isOnRenderThread()) {
            VoxelConstants.getLogger().log(Level.WARN, "Texture upload call from wrong thread", new Exception());
            return;
        }

        long startedNanos = PersistentMapProfiler.startTimer();
        boolean textureCreated = texture == null || texture.getWidth(0) != this.imageSize;
        try {
            if (pixels == null) {
                this.decompress();
            }

            if (texture != null && texture.getWidth(0) != this.pixels.getWidth()) {
                this.releaseTextures();
            }

            if (texture == null) {
                int mipLevels = maxMipLevel(this.pixels.getWidth());
                GpuDevice gpuDevice = RenderSystem.getDevice();
                this.texture = gpuDevice.createTexture("compressibleMapRegionTexture", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, GpuFormat.RGBA8_UNORM, this.pixels.getWidth(), this.pixels.getHeight(), 1, mipLevels + 1);
                this.textureView = gpuDevice.createTextureView(this.texture, 0, mipLevels + 1);

                if (!registered) {
                    Minecraft.getInstance().getTextureManager().register(location, this);
                    registered = true;
                }
            }

            if (pixelsMipmapped == null) {
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.pixels, 0, 0, 0, 0);
            } else {
                for (int i = 0; i < pixelsMipmapped.length; i++) {
                    RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.pixelsMipmapped[i], i, 0, 0, 0);
                }
            }

            this.uploadedSize = this.pixels.getWidth();
            this.compress();
        } finally {
            PersistentMapProfiler.recordTextureUpload(startedNanos, textureCreated);
        }
    }

    public synchronized void setRGB(int x, int y, int color) {
        if (pixels == null) {
            this.decompress();
        }
        if (pixels != null) {
            pixels.setPixel(x, y, ColorUtils.premultiplyWithAlpha(color));
        }
    }

    private synchronized void compress() {
        if (pixels != null) {
            clearMipmaps();
            if (this.pixels != null) {
                if (this.retainCompressedPixels) {
                    byte[] is = new byte[this.pixels.getHeight() * this.pixels.getWidth() * 4];
                    MemoryUtil.memByteBuffer(this.pixels.getPointer(), is.length).get(is);
                    this.bytes = CompressionUtils.compress(is);
                } else {
                    this.bytes = null;
                }
                this.pixels.close();
                this.pixels = null;
            }
        }
    }

    public synchronized void generateMipmaps() {
        if (pixels == null) return;
        clearMipmaps();
        pixelsMipmapped = MipmapGenerator.generateMipLevels(location, new NativeImage[]{pixels}, maxMipLevel(this.imageSize), MipmapStrategy.MEAN, 0.0F, Transparency.TRANSPARENT_AND_TRANSLUCENT);
    }

    private synchronized void decompress() {
        if (pixels == null) {
            this.pixels = new NativeImage(this.imageSize, this.imageSize, false);
            if (this.bytes != null) {
                try {
                    byte[] is = CompressionUtils.decompress(this.bytes);
                    if (is.length != this.pixels.getHeight() * this.pixels.getWidth() * 4) {
                        throw new RuntimeException("Invalid image size, expected " + (this.pixels.getHeight() * this.pixels.getWidth() * 4) + ", got " + is.length);
                    }
                    this.bytes = null;
                    MemoryUtil.memByteBuffer(this.pixels.getPointer(), is.length).put(is);
                } catch (DataFormatException ignored) {
                    this.bytes = null;
                    this.contentValid = false;
                }
            }
        }
    }

    private void clearMipmaps() {
        if (pixelsMipmapped != null) {
            for (int i = 1; i < pixelsMipmapped.length; i++) { // first is original
                pixelsMipmapped[i].close();
            }
            pixelsMipmapped = null;
        }
    }

    private void clearCpuImages() {
        clearMipmaps();
        if (this.pixels != null) {
            this.pixels.close();
            this.pixels = null;
        }
    }

    private static int maxMipLevel(int imageSize) {
        return Math.max(0, Integer.numberOfTrailingZeros(imageSize) - 1);
    }

    private static void validateImageSize(int imageSize) {
        if (imageSize <= 0 || (imageSize & imageSize - 1) != 0) {
            throw new IllegalArgumentException("Image size must be a positive power of two: " + imageSize);
        }
    }

    @Override
    public synchronized void close() {
        clearCpuImages();
        this.bytes = null;
        this.contentValid = false;
        this.uploadedSize = 0;
        super.close();
    }
}
