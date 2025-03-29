package com.mamiyaotaru.voxelmap.textures;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class TextureAtlas extends AbstractTexture {
    private final HashMap<String, Sprite> mapRegisteredSprites;
    private final HashMap<String, Sprite> mapUploadedSprites;
    private final String basePath;
    private final IIconCreator iconCreator;
    private final Sprite missingImage;
    private final Sprite failedImage;
    private Stitcher stitcher;
    private boolean linearFilter;
    private boolean mipmap;
    private ResourceLocation resourceLocation;

    public TextureAtlas(String basePath, ResourceLocation resourceLocation) {
        this(basePath, resourceLocation, null);
    }

    public TextureAtlas(String basePath, ResourceLocation resourceLocation, IIconCreator iconCreator) {
        this.mapRegisteredSprites = Maps.newHashMap();
        this.mapUploadedSprites = Maps.newHashMap();
        this.missingImage = new Sprite("missingno", this);
        this.failedImage = new Sprite("notfound", this);
        this.basePath = basePath;
        this.iconCreator = iconCreator;
        this.resourceLocation = resourceLocation;
        Minecraft.getInstance().getTextureManager().register(resourceLocation, this);
    }

    @Override
    public void setFilter(boolean linearFilter, boolean mipmap) {
        this.linearFilter = linearFilter;
        this.mipmap = mipmap;
        if (texture != null) {
            super.setFilter(linearFilter, mipmap);
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
        for (Map.Entry<String, Sprite> entry : this.mapRegisteredSprites.entrySet()) {
            Sprite icon = entry.getValue();
            this.stitcher.addSprite(icon);
        }

        this.stitcher.doStitch();

        VoxelConstants.getLogger().info("Created: {}x{} {}-atlas", new Object[]{this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight(), this.basePath});

        texture = RenderSystem.getDevice().createTexture("voxelmap-atlas", TextureFormat.RGBA8, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight(), 1);
        super.setFilter(linearFilter, mipmap);
        HashMap<String, Sprite> tempMapRegisteredSprites = Maps.newHashMap(this.mapRegisteredSprites);
        for (Sprite icon : this.stitcher.getStitchSlots()) {
            String iconName = icon.getIconName();
            tempMapRegisteredSprites.remove(iconName);
            this.mapUploadedSprites.put(iconName, icon);

            try {
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, icon.getTextureData(), 0, icon.getOriginX(), icon.getOriginY(), icon.getIconWidth(), icon.getIconHeight(), 0, 0);
            } catch (Throwable var10) {
                CrashReport crashReport = CrashReport.forThrowable(var10, "Stitching texture atlas");
                CrashReportCategory crashReportCategory = crashReport.addCategory("Texture being stitched together");
                crashReportCategory.setDetail("Atlas path", this.basePath);
                crashReportCategory.setDetail("Sprite", icon);
                throw new ReportedException(crashReport);
            }
        }

        for (Sprite icon : tempMapRegisteredSprites.values()) {
            icon.copyFrom(this.missingImage);
        }

        this.mapRegisteredSprites.clear();
        this.missingImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
        this.failedImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
        if (VoxelConstants.DEBUG) {
            ImageUtils.saveImage(this.basePath.replaceAll("/", "_"), this.getTexture(), 0, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
        }
    }

    public void stitchNew() {
        for (Map.Entry<String, Sprite> entry : this.mapRegisteredSprites.entrySet()) {
            Sprite icon = entry.getValue();
            this.stitcher.addSprite(icon);
        }

        int oldWidth = this.stitcher.getCurrentImageWidth();
        int oldHeight = this.stitcher.getCurrentImageHeight();

        this.stitcher.doStitchNew();

        if (texture == null || oldWidth != this.stitcher.getCurrentImageWidth() || oldHeight != this.stitcher.getCurrentImageHeight()) {
            if (texture != null) {
                texture.close();
                texture = null;
            }
            VoxelConstants.getLogger().info("Resized to: {}x{} {}-atlas", new Object[]{this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight(), this.basePath});
            texture = RenderSystem.getDevice().createTexture("voxelmap-atlas", TextureFormat.RGBA8, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight(), 1);
            super.setFilter(linearFilter, mipmap);
        }

        HashMap<String, Sprite> tempMapRegisteredSprites = Maps.newHashMap(this.mapRegisteredSprites);
        for (Sprite icon : this.stitcher.getStitchSlots()) {
            String iconName = icon.getIconName();
            tempMapRegisteredSprites.remove(iconName);
            this.mapUploadedSprites.put(iconName, icon);

            try {
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, icon.getTextureData(), 0, icon.getOriginX(), icon.getOriginY(), icon.getIconWidth(), icon.getIconHeight(), 0, 0);
            } catch (Throwable var11) {
                CrashReport crashReport = CrashReport.forThrowable(var11, "Stitching texture atlas");
                CrashReportCategory crashReportCategory = crashReport.addCategory("Texture being stitched together");
                crashReportCategory.setDetail("Atlas path", this.basePath);
                crashReportCategory.setDetail("Sprite", icon);
                throw new ReportedException(crashReport);
            }
        }

        for (Sprite icon : tempMapRegisteredSprites.values()) {
            icon.copyFrom(this.missingImage);
        }

        this.mapRegisteredSprites.clear();
        this.missingImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
        this.failedImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
        if (VoxelConstants.DEBUG) {
            if (oldWidth != this.stitcher.getCurrentImageWidth() || oldHeight != this.stitcher.getCurrentImageHeight()) {
                ImageUtils.saveImage(this.basePath.replaceAll("/", "_"), this.getTexture(), 0, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
            }
        }
    }

    public Sprite getIconAt(float x, float y) {
        return this.mapUploadedSprites.entrySet().stream().map(stringSpriteEntry -> (Sprite) ((Map.Entry<?, ?>) stringSpriteEntry).getValue()).filter(icon -> x >= icon.originX && x < (icon.originX + icon.width) && y >= icon.originY && y < (icon.originY + icon.height)).findFirst().orElse(this.missingImage);
    }

    public Sprite getAtlasSprite(String name) {
        Sprite icon = this.mapUploadedSprites.get(name);
        if (icon == null) {
            icon = this.missingImage;
        }

        return icon;
    }

    public Sprite getAtlasSpriteIncludingYetToBeStitched(String name) {
        Sprite icon = this.mapUploadedSprites.get(name);
        if (icon == null) {
            icon = this.mapRegisteredSprites.get(name);
        }

        if (icon == null) {
            icon = this.missingImage;
        }

        return icon;
    }

    public Sprite registerIconForResource(ResourceLocation resourceLocation) {
        if (resourceLocation == null) {
            throw new IllegalArgumentException("Location cannot be null!");
        } else {
            Sprite icon = this.mapRegisteredSprites.get(resourceLocation.toString());
            if (icon == null) {
                icon = Sprite.spriteFromResourceLocation(resourceLocation, this);

                try {
                    TextureContents image = TextureContents.load(Minecraft.getInstance().getResourceManager(), resourceLocation);
                    icon.setTextureData(image.image());
                } catch (RuntimeException var6) {
                    VoxelConstants.getLogger().error("Unable to parse metadata from " + resourceLocation, var6);
                } catch (IOException var7) {
                    VoxelConstants.getLogger().error("Using missing texture, unable to load " + resourceLocation, var7);
                }

                this.mapRegisteredSprites.put(resourceLocation.toString(), icon);
            }

            return icon;
        }
    }

    public Sprite registerIconForBufferedImage(String name, BufferedImage bufferedImage) {
        NativeImage img = ImageUtils.nativeImageFromBufferedImage(bufferedImage);
        return registerIconForBufferedImage(name, img);
    }

    public Sprite registerIconForBufferedImage(String name, NativeImage bufferedImage) {
        if (name != null && !name.isEmpty()) {
            Sprite icon = this.mapRegisteredSprites.get(name);
            if (icon == null) {
                icon = Sprite.spriteFromString(name, this);
                icon.setTextureData(bufferedImage);
                // FIXME 1.21.5 Prevent duplicate images (Radar?)
                // for (Sprite existing : this.mapUploadedSprites.values()) {
                // if (Arrays.equals(existing.imageData, icon.imageData)) {
                // this.registerMaskedIcon(name, existing);
                // return existing;
                // }
                // }
                //
                // for (Sprite existing : this.mapRegisteredSprites.values()) {
                // if (Arrays.equals(existing.imageData, icon.imageData)) {
                // this.registerMaskedIcon(name, existing);
                // return existing;
                // }
                // }

                this.mapRegisteredSprites.put(name, icon);
            }

            return icon;
        } else {
            throw new IllegalArgumentException("Name cannot be null!");
        }
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

    public void registerMaskedIcon(String name, Sprite originalIcon) {
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

    public ResourceLocation getResourceLocation() {
        return resourceLocation;
    }
}
