package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.CompressionUtils;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.Level;
import org.lwjgl.system.MemoryUtil;

import java.util.UUID;
import java.util.zip.DataFormatException;

public class CompressibleMapRegionTexture extends AbstractTexture {
    private final static int MIP_LEVELS = 7;

    private NativeImage pixels;
    private NativeImage[] pixelsMipmapped;

    private final boolean compressNotDelete;
    private final Identifier location = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "mapimage/" + UUID.randomUUID());

    private final GpuSampler samplerSmall;
    private final GpuSampler samplerLarge;

    private byte[] bytes;

    public CompressibleMapRegionTexture() {
        this.compressNotDelete = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions().outputImages;

        this.pixels = new NativeImage(CachedRegion.REGION_WIDTH, CachedRegion.REGION_WIDTH, false);
        this.samplerSmall = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, true);
        this.samplerLarge = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, true);
        this.sampler = samplerLarge;
    }

    public NativeImage getData() {
        if (pixels == null) {
            this.decompress();
        }
        return pixels;
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
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(location);
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

            Minecraft.getInstance().getTextureManager().register(location, this);
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

    public void setRGB(int x, int y, int color) {
        if (pixels == null) {
            this.decompress();
        }
        NativeImage localPixels = pixels;
        if (localPixels != null) {
            localPixels.setPixel(x, y, ColorUtils.premultiplyWithAlpha(color));
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

    public void generateMipmaps() {
        clearMipmaps();
        pixelsMipmapped = MipmapGenerator.generateMipLevels(location, new NativeImage[] { pixels }, MIP_LEVELS, MipmapStrategy.MEAN, 0.0F, Transparency.TRANSPARENT_AND_TRANSLUCENT);
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

    @Override
    public void close() {
        clearMipmaps();
        if (this.pixels != null) {
            this.pixels.close();
            this.pixels = null;
        }
        super.close();
    }
}
