package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.CompressionUtils;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.Level;
import org.lwjgl.system.MemoryUtil;

import java.util.zip.DataFormatException;

public class CompressibleMapRegionTexture extends AbstractTexture {
    private static final int MIP_LEVELS = 7;
    private static final Identifier EMPTY_ID = Identifier.parse("");

    private NativeImage pixels;
    private NativeImage[] pixelsMipmapped;

    private final boolean compressNotDelete;

    private GpuSampler samplerSmall;
    private GpuSampler samplerLarge;

    private byte[] bytes;

    public CompressibleMapRegionTexture() {
        this.compressNotDelete = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions().outputImages.get();

        this.pixels = new NativeImage(CachedRegion.REGION_WIDTH, CachedRegion.REGION_WIDTH, false);
        this.updateSamplers();
        this.sampler = samplerLarge;
    }

    public NativeImage getData() {
        if (pixels == null) {
            this.decompress();
        }
        return pixels;
    }

    public AbstractTexture getTexture(float zoom) {
        if (zoom < 2) {
            this.sampler = samplerSmall;
        } else {
            this.sampler = samplerLarge;
        }
        return this.texture != null ? this : null;
    }

    public void deleteTexture() {
        if (!RenderSystem.isOnRenderThread()) {
            VoxelConstants.getLogger().log(Level.WARN, "Texture unload call from wrong thread", new Exception());
            return;
        }
        close();
    }

    public void uploadToTexture() {
        if (!RenderSystem.isOnRenderThread()) {
            VoxelConstants.getLogger().log(Level.WARN, "Texture upload call from wrong thread", new Exception());
            return;
        }

        if (pixels == null) {
            this.decompress();
        }

        if (texture == null) {
            GpuDevice gpuDevice = RenderSystem.getDevice();
            this.texture = gpuDevice.createTexture("compressibleMapRegionTexture", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, MIP_LEVELS + 1);
            this.textureView = gpuDevice.createTextureView(this.texture, 0, MIP_LEVELS + 1);
        }

        int w = texture.getWidth(0);
        int h = texture.getHeight(0);
        if (pixelsMipmapped == null) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.pixels, 0, 0, 0, 0, w, h, 0, 0);
        } else {
            for (int i = 0; i < pixelsMipmapped.length; i++) {
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.pixelsMipmapped[i], i, 0, 0, 0, w >> i, h >> i, 0, 0);
            }
        }

        this.compress();
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
                if (this.compressNotDelete) {
                    byte[] is = new byte[this.pixels.getHeight() * this.pixels.getWidth() * 4];
                    MemoryUtil.memByteBuffer(this.pixels.getPointer(), is.length).get(is);
                    this.bytes = CompressionUtils.compress(is);
                }
                this.pixels.close();
                this.pixels = null;
            }
        }
    }

    public synchronized void generateMipmaps() {
        if (pixels == null) {
            return;
        }
        clearMipmaps();
        pixelsMipmapped = MipmapGenerator.generateMipLevels(EMPTY_ID, new NativeImage[]{pixels}, MIP_LEVELS, MipmapStrategy.MEAN, 0.0f);
    }

    private synchronized void decompress() {
        if (pixels == null) {
            this.pixels = new NativeImage(CachedRegion.REGION_WIDTH, CachedRegion.REGION_WIDTH, false);
            if (this.compressNotDelete && this.bytes != null) {
                try {
                    byte[] is = CompressionUtils.decompress(this.bytes);
                    if (is.length != this.pixels.getHeight() * this.pixels.getWidth() * 4) {
                        throw new RuntimeException("Invalid image size, expected " + (this.pixels.getHeight() * this.pixels.getWidth() * 4) + ", got " + is.length);
                    }
                    this.bytes = null;
                    MemoryUtil.memByteBuffer(this.pixels.getPointer(), is.length).put(is);
                } catch (DataFormatException ignored) {
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

    public void updateSamplers() {
        boolean filtering = VoxelConstants.getVoxelMapInstance().getMapOptions().filtering.get();
        if (filtering) {
            this.samplerSmall = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, true);
            this.samplerLarge = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, true);
        } else {
            this.samplerSmall = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, true);
            this.samplerLarge = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, true);
        }
    }

    @Override
    public synchronized void close() {
        clearMipmaps();
        if (this.pixels != null) {
            this.pixels.close();
            this.pixels = null;
        }
        super.close();
    }
}
