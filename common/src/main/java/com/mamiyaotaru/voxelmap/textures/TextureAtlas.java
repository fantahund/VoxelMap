package com.mamiyaotaru.voxelmap.textures;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class TextureAtlas extends AbstractTexture {
    private final HashMap<Object, Sprite> mapRegisteredSprites;
    private final HashMap<Object, Sprite> mapUploadedSprites;
    private final String basePath;
    private final IIconCreator iconCreator;
    private final Sprite missingImage;
    private final Sprite failedImage;
    private Stitcher stitcher;
    private boolean linearFilter;
    private boolean mipmap;
    private Identifier Identifier;

    public TextureAtlas(String basePath, Identifier Identifier) {
        this(basePath, Identifier, null);
    }

    public TextureAtlas(String basePath, Identifier Identifier, IIconCreator iconCreator) {
        this.mapRegisteredSprites = Maps.newHashMap();
        this.mapUploadedSprites = Maps.newHashMap();
        this.missingImage = new Sprite("missingno", this);
        this.failedImage = new Sprite("notfound", this);
        this.basePath = basePath;
        this.iconCreator = iconCreator;
        this.Identifier = Identifier;
        Minecraft.getInstance().getTextureManager().register(Identifier, this);
    }

    public void setFilter(boolean linearFilter, boolean mipmap) {
        this.linearFilter = linearFilter;
        this.mipmap = mipmap;
        if (texture != null) {
            sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,  linearFilter ? FilterMode.LINEAR : FilterMode.NEAREST, linearFilter ? FilterMode.LINEAR : FilterMode.NEAREST, false);
        }
    }

    private void initMissingImage() {
        this.missingImage.setTextureData(new NativeImage(1, 1, true));
        this.failedImage.copyFrom(this.missingImage);
        this.failedImage.setTextureData(new NativeImage(1, 1, true));
    }

    public void load(ResourceManager manager) {
        if (this.iconCreator != null) {
            this.loadTextureAtlas(this.iconCreator);
        }

    }

    public void reset() {
        for (Sprite e : this.mapRegisteredSprites.values()) {
            e.setTextureData(null);
        }
        for (Sprite e : this.mapUploadedSprites.values()) {
            e.setTextureData(null);
        }
        this.mapRegisteredSprites.clear();
        this.mapUploadedSprites.clear();
        this.initMissingImage();
        int glMaxTextureSize = RenderSystem.getDevice().getMaxTextureSize();
        this.stitcher = new Stitcher(glMaxTextureSize, glMaxTextureSize, 0);
    }

    public void loadTextureAtlas(IIconCreator iconCreator) {
        this.reset();
        iconCreator.addIcons(this);
        this.stitch();
    }

    public void stitch() {
        for (Map.Entry<Object, Sprite> entry : this.mapRegisteredSprites.entrySet()) {
            Sprite icon = entry.getValue();
            if (icon.getTextureData() != null) {
                this.stitcher.addSprite(icon);
            }
        }

        this.stitcher.doStitch();

        VoxelConstants.getLogger().info("Created: {}x{} {}-atlas", new Object[] { this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight(), this.basePath });

        texture = RenderSystem.getDevice().createTexture("voxelmap-atlas", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RGBA8, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight(), 1, 1);
        textureView = RenderSystem.getDevice().createTextureView(texture);
        // super.setFilter(linearFilter, mipmap);
        HashMap<Object, Sprite> tempMapRegisteredSprites = Maps.newHashMap(this.mapRegisteredSprites);
        for (Sprite icon : this.stitcher.getStitchSlots()) {
            Object iconName = icon.getIconName();
            tempMapRegisteredSprites.remove(iconName);
            this.mapUploadedSprites.put(iconName, icon);
            this.mapRegisteredSprites.remove(iconName);

            try {
                if (icon.getTextureData() != null) {
                    RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, icon.getTextureData(), 0, 0, icon.getOriginX(), icon.getOriginY(), icon.getIconWidth(), icon.getIconHeight(), 0, 0);
                }
            } catch (Throwable var10) {
                CrashReport crashReport = CrashReport.forThrowable(var10, "Stitching texture atlas");
                CrashReportCategory crashReportCategory = crashReport.addCategory("Texture being stitched together");
                crashReportCategory.setDetail("Atlas path", this.basePath);
                crashReportCategory.setDetail("Sprite", icon);
                throw new ReportedException(crashReport);
            }
        }

        for (Sprite icon : tempMapRegisteredSprites.values()) {
            if (icon.getTextureData() != null) {
                icon.copyFrom(this.missingImage);
                this.mapRegisteredSprites.remove(icon.getIconName());
            }
        }

        this.missingImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
        this.failedImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
        if (VoxelConstants.DEBUG) {
            saveDebugImage();
        }
    }

    public void stitchNew() {
        for (Map.Entry<Object, Sprite> entry : this.mapRegisteredSprites.entrySet()) {
            Sprite icon = entry.getValue();
            if (icon.getTextureData() != null) {
                this.stitcher.addSprite(icon);
            }
        }

        int oldWidth = this.stitcher.getCurrentImageWidth();
        int oldHeight = this.stitcher.getCurrentImageHeight();

        this.stitcher.doStitchNew();

        if (texture == null || oldWidth != this.stitcher.getCurrentImageWidth() || oldHeight != this.stitcher.getCurrentImageHeight()) {
            if (texture != null) {
                texture.close();
                texture = null;
            }
            VoxelConstants.getLogger().info("Resized to: {}x{} {}-atlas", new Object[] { this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight(), this.basePath });
            texture = RenderSystem.getDevice().createTexture("voxelmap-atlas", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RGBA8, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight(), 1, 1);
            textureView = RenderSystem.getDevice().createTextureView(texture);
            // super.setFilter(linearFilter, mipmap);
        }

        HashMap<Object, Sprite> tempMapRegisteredSprites = Maps.newHashMap(this.mapRegisteredSprites);
        for (Sprite icon : this.stitcher.getStitchSlots()) {
            Object iconName = icon.getIconName();
            tempMapRegisteredSprites.remove(iconName);
            this.mapUploadedSprites.put(iconName, icon);
            this.mapRegisteredSprites.remove(iconName);

            try {
                if (icon.getTextureData() != null) {
                    RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, icon.getTextureData(), 0, 0, icon.getOriginX(), icon.getOriginY(), icon.getIconWidth(), icon.getIconHeight(), 0, 0);
                }
            } catch (Throwable var11) {
                CrashReport crashReport = CrashReport.forThrowable(var11, "Stitching texture atlas");
                CrashReportCategory crashReportCategory = crashReport.addCategory("Texture being stitched together");
                crashReportCategory.setDetail("Atlas path", this.basePath);
                crashReportCategory.setDetail("Sprite", icon);
                throw new ReportedException(crashReport);
            }
        }

        for (Sprite icon : tempMapRegisteredSprites.values()) {
            if (icon.getTextureData() != null) {
                icon.copyFrom(this.missingImage);
                this.mapRegisteredSprites.remove(icon.getIconName());
            }
        }

        this.missingImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
        this.failedImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
        if (VoxelConstants.DEBUG) {
            if (oldWidth != this.stitcher.getCurrentImageWidth() || oldHeight != this.stitcher.getCurrentImageHeight()) {
                saveDebugImage();
            }
        }
    }

    public void saveDebugImage() {
        ImageUtils.saveImage(this.basePath.replaceAll("/", "_"), this.getTexture(), 0, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
    }

    public Sprite getIconAt(float x, float y) {
        return this.mapUploadedSprites.entrySet().stream().map(stringSpriteEntry -> (Sprite) ((Map.Entry<?, ?>) stringSpriteEntry).getValue()).filter(icon -> x >= icon.originX && x < (icon.originX + icon.width) && y >= icon.originY && y < (icon.originY + icon.height)).findFirst()
                .orElse(this.missingImage);
    }

    public Sprite getAtlasSprite(Object name) {
        Sprite icon = this.mapUploadedSprites.get(name);
        if (icon == null) {
            icon = this.missingImage;
        }

        return icon;
    }

    public Sprite getAtlasSpriteIncludingYetToBeStitched(Object name) {
        Sprite icon = this.mapUploadedSprites.get(name);
        if (icon == null) {
            icon = this.mapRegisteredSprites.get(name);
        }

        if (icon == null) {
            icon = this.missingImage;
        }

        return icon;
    }

    public Sprite registerIconForResource(Identifier Identifier) {
        if (Identifier == null) {
            throw new IllegalArgumentException("Location cannot be null!");
        } else {
            Sprite icon = this.mapRegisteredSprites.get(Identifier.toString());
            if (icon == null) {
                icon = Sprite.spriteFromIdentifier(Identifier, this);

                try {
                    TextureContents image = TextureContents.load(Minecraft.getInstance().getResourceManager(), Identifier);
                    icon.setTextureData(image.image());
                } catch (RuntimeException var6) {
                    VoxelConstants.getLogger().error("Unable to parse metadata from " + Identifier, var6);
                } catch (IOException var7) {
                    VoxelConstants.getLogger().error("Using missing texture, unable to load " + Identifier, var7);
                }

                this.mapRegisteredSprites.put(Identifier.toString(), icon);
            }

            return icon;
        }
    }

    public Sprite registerIconForBufferedImage(Object name, BufferedImage bufferedImage) {
        NativeImage img = ImageUtils.nativeImageFromBufferedImage(bufferedImage);
        return registerIconForBufferedImage(name, img);
    }

    public Sprite registerIconForBufferedImage(Object name, NativeImage bufferedImage) {
        if (name != null) {
            Sprite icon = this.mapRegisteredSprites.get(name);
            if (icon == null) {
                icon = Sprite.spriteFromString(name, this);
                icon.setTextureData(bufferedImage);
                this.mapRegisteredSprites.put(name, icon);
            }

            return icon;
        } else {
            throw new IllegalArgumentException("Name cannot be null!");
        }
    }

    public Sprite registerEmptyIcon(Object name) {
        Sprite icon = this.mapRegisteredSprites.get(name);
        if (icon == null) {
            icon = Sprite.spriteFromString(name, this);
            this.mapRegisteredSprites.put(name, icon);
        }
        return icon;
    }

    public Sprite getMissingImage() {
        return this.missingImage;
    }

    public Sprite getFailedImage() {
        return this.failedImage;
    }

    public void registerFailedIcon(String name) {
        this.mapUploadedSprites.put(name, this.failedImage);
    }

    public void registerMaskedIcon(Object name, Sprite originalIcon) {
        Sprite existingIcon = this.mapUploadedSprites.get(name);
        if (existingIcon == null) {
            existingIcon = this.mapRegisteredSprites.get(name);
        }

        if (existingIcon == null) {
            this.mapUploadedSprites.put(name, originalIcon);
        }

    }

    public int getWidth() {
        return this.stitcher.getCurrentWidth();
    }

    public int getHeight() {
        return this.stitcher.getCurrentHeight();
    }

    public int getImageWidth() {
        return this.stitcher.getCurrentImageWidth();
    }

    public int getImageHeight() {
        return this.stitcher.getCurrentImageHeight();
    }

    public Identifier getIdentifier() {
        return Identifier;
    }
}
