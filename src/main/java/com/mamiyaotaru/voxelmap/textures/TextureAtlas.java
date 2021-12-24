package com.mamiyaotaru.voxelmap.textures;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.Identifier;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TextureAtlas extends AbstractTexture {
   private static final Logger logger = LogManager.getLogger();
   private final HashMap<String, Sprite> mapRegisteredSprites;
   private final HashMap<String, Sprite> mapUploadedSprites;
   private final String basePath;
   private final IIconCreator iconCreator;
   private final int mipmapLevels = 0;
   private final Sprite missingImage;
   private final Sprite failedImage;
   private Stitcher stitcher;

   public TextureAtlas(String basePath) {
      this(basePath, (IIconCreator)null);
   }

   public TextureAtlas(String basePath, IIconCreator iconCreator) {
      this.mapRegisteredSprites = Maps.newHashMap();
      this.mapUploadedSprites = Maps.newHashMap();
      this.missingImage = new Sprite("missingno");
      this.failedImage = new Sprite("notfound");
      this.basePath = basePath;
      this.iconCreator = iconCreator;
   }

   private void initMissingImage() {
      int[] missingTextureData = new int[1];
      Arrays.fill(missingTextureData, 0);
      this.missingImage.setIconWidth(1);
      this.missingImage.setIconHeight(1);
      this.missingImage.setTextureData(missingTextureData);
      this.failedImage.copyFrom(this.missingImage);
      this.failedImage.setTextureData(missingTextureData);
   }

   public void load(ResourceManager resourceManager) throws IOException {
      if (this.iconCreator != null) {
         this.loadTextureAtlas(this.iconCreator);
      }

   }

   public void reset() {
      this.mapRegisteredSprites.clear();
      this.mapUploadedSprites.clear();
      this.initMissingImage();
      int glMaxTextureSize = RenderSystem.maxSupportedTextureSize();
      this.stitcher = new Stitcher(glMaxTextureSize, glMaxTextureSize, 0);
   }

   public void loadTextureAtlas(IIconCreator iconCreator) {
      this.reset();
      iconCreator.addIcons(this);
      this.stitch();
   }

   public void stitch() {
      for(Entry entry : this.mapRegisteredSprites.entrySet()) {
         Sprite icon = (Sprite)entry.getValue();
         this.stitcher.addSprite(icon);
      }

      try {
         this.stitcher.doStitch();
      } catch (StitcherException var11) {
         throw var11;
      }

      logger.info("Created: {}x{} {}-atlas", new Object[]{this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight(), this.basePath});
      TextureUtilLegacy.allocateTextureImpl(this.getGlId(), 0, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
      int[] zeros = new int[this.stitcher.getCurrentImageWidth() * this.stitcher.getCurrentImageHeight()];
      Arrays.fill(zeros, 0);
      TextureUtilLegacy.uploadTexture(this.getGlId(), zeros, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
      HashMap<String, Sprite> tempMapRegisteredSprites = Maps.newHashMap(this.mapRegisteredSprites);

      for(Sprite icon : this.stitcher.getStitchSlots()) {
         String iconName = icon.getIconName();
         tempMapRegisteredSprites.remove(iconName);
         this.mapUploadedSprites.put(iconName, icon);

         try {
            TextureUtilLegacy.uploadTextureMipmap(
               new int[][]{icon.getTextureData()}, icon.getIconWidth(), icon.getIconHeight(), icon.getOriginX(), icon.getOriginY(), false, false
            );
         } catch (Throwable var10) {
            CrashReport crashReport = CrashReport.create(var10, "Stitching texture atlas");
            CrashReportSection crashReportCategory = crashReport.addElement("Texture being stitched together");
            crashReportCategory.add("Atlas path", this.basePath);
            crashReportCategory.add("Sprite", icon);
            throw new CrashException(crashReport);
         }
      }

      for(Sprite icon : tempMapRegisteredSprites.values()) {
         icon.copyFrom(this.missingImage);
      }

      this.mapRegisteredSprites.clear();
      this.missingImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
      this.failedImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
      ImageUtils.saveImage(
         this.basePath.replaceAll("/", "_"), this.getGlId(), 0, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight()
      );
   }

   public void stitchNew() {
      for(Entry entry : this.mapRegisteredSprites.entrySet()) {
         Sprite icon = (Sprite)entry.getValue();
         this.stitcher.addSprite(icon);
      }

      int oldWidth = this.stitcher.getCurrentImageWidth();
      int oldHeight = this.stitcher.getCurrentImageHeight();

      try {
         this.stitcher.doStitchNew();
      } catch (StitcherException var12) {
         throw var12;
      }

      if (oldWidth == this.stitcher.getCurrentImageWidth() && oldHeight == this.stitcher.getCurrentImageHeight()) {
         GLShim.glBindTexture(3553, this.glId);
      } else {
         logger.info("Resized to: {}x{} {}-atlas", new Object[]{this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight(), this.basePath});
         TextureUtilLegacy.allocateTextureImpl(this.getGlId(), 0, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
         int[] zeros = new int[this.stitcher.getCurrentImageWidth() * this.stitcher.getCurrentImageHeight()];
         Arrays.fill(zeros, 0);
         TextureUtilLegacy.uploadTexture(this.getGlId(), zeros, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
      }

      HashMap<String, Sprite> tempMapRegisteredSprites = Maps.newHashMap(this.mapRegisteredSprites);

      for(Sprite icon : this.stitcher.getStitchSlots()) {
         String iconName = icon.getIconName();
         tempMapRegisteredSprites.remove(iconName);
         this.mapUploadedSprites.put(iconName, icon);

         try {
            TextureUtilLegacy.uploadTextureMipmap(
               new int[][]{icon.getTextureData()}, icon.getIconWidth(), icon.getIconHeight(), icon.getOriginX(), icon.getOriginY(), false, false
            );
         } catch (Throwable var11) {
            CrashReport crashReport = CrashReport.create(var11, "Stitching texture atlas");
            CrashReportSection crashReportCategory = crashReport.addElement("Texture being stitched together");
            crashReportCategory.add("Atlas path", this.basePath);
            crashReportCategory.add("Sprite", icon);
            throw new CrashException(crashReport);
         }
      }

      for(Sprite icon : tempMapRegisteredSprites.values()) {
         icon.copyFrom(this.missingImage);
      }

      this.mapRegisteredSprites.clear();
      this.missingImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
      this.failedImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
      if (oldWidth != this.stitcher.getCurrentImageWidth() || oldHeight != this.stitcher.getCurrentImageHeight()) {
         ImageUtils.saveImage(
            this.basePath.replaceAll("/", "_"), this.getGlId(), 0, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight()
         );
      }

   }

   public Sprite getIconAt(float x, float y) {
      Iterator uploadedSpritesEntriesIterator = this.mapUploadedSprites.entrySet().iterator();

      while(uploadedSpritesEntriesIterator.hasNext()) {
         Sprite icon = (Sprite)((Entry)uploadedSpritesEntriesIterator.next()).getValue();
         if (x >= (float)icon.originX && x < (float)(icon.originX + icon.width) && y >= (float)icon.originY && y < (float)(icon.originY + icon.height)) {
            return icon;
         }
      }

      return this.missingImage;
   }

   public Sprite getAtlasSprite(String name) {
      Sprite icon = (Sprite)this.mapUploadedSprites.get(name);
      if (icon == null) {
         icon = this.missingImage;
      }

      return icon;
   }

   public Sprite getAtlasSpriteIncludingYetToBeStitched(String name) {
      Sprite icon = (Sprite)this.mapUploadedSprites.get(name);
      if (icon == null) {
         icon = (Sprite)this.mapRegisteredSprites.get(name);
      }

      if (icon == null) {
         icon = this.missingImage;
      }

      return icon;
   }

   public Sprite registerIconForResource(Identifier resourceLocation, ResourceManager resourceManager) {
      if (resourceLocation == null) {
         throw new IllegalArgumentException("Location cannot be null!");
      } else {
         Sprite icon = (Sprite)this.mapRegisteredSprites.get(resourceLocation.toString());
         if (icon == null) {
            icon = Sprite.spriteFromResourceLocation(resourceLocation);

            try {
               Resource entryResource = resourceManager.getResource(resourceLocation);
               BufferedImage entryBufferedImage = TextureUtilLegacy.readBufferedImage(entryResource.getInputStream());
               icon.bufferedImageToIntData(entryBufferedImage);
               entryBufferedImage.flush();
            } catch (RuntimeException var6) {
               logger.error("Unable to parse metadata from " + resourceLocation, var6);
            } catch (IOException var7) {
               logger.error("Using missing texture, unable to load " + resourceLocation, var7);
            }

            this.mapRegisteredSprites.put(resourceLocation.toString(), icon);
         }

         return icon;
      }
   }

   public Sprite registerIconForBufferedImage(String name, BufferedImage bufferedImage) {
      if (name != null && !name.equals("")) {
         Sprite icon = (Sprite)this.mapRegisteredSprites.get(name);
         if (icon == null) {
            icon = Sprite.spriteFromString(name);
            icon.bufferedImageToIntData(bufferedImage);
            bufferedImage.flush();

            for(Sprite existing : this.mapUploadedSprites.values()) {
               if (Arrays.equals(existing.imageData, icon.imageData)) {
                  this.registerMaskedIcon(name, existing);
                  return existing;
               }
            }

            for(Sprite existing : this.mapRegisteredSprites.values()) {
               if (Arrays.equals(existing.imageData, icon.imageData)) {
                  this.registerMaskedIcon(name, existing);
                  return existing;
               }
            }

            this.mapRegisteredSprites.put(name, icon);
         }

         return icon;
      } else {
         throw new IllegalArgumentException("Name cannot be null!");
      }
   }

   public void registerOrOverwriteSprite(String name, BufferedImage bufferedImage) {
      if (name != null && !name.equals("")) {
         Sprite icon = (Sprite)this.mapRegisteredSprites.get(name);
         if (icon != null) {
            icon.bufferedImageToIntData(bufferedImage);
         } else {
            icon = this.getAtlasSprite(name);
            if (icon != null) {
               icon.bufferedImageToIntData(bufferedImage);

               try {
                  GLShim.glBindTexture(3553, this.glId);
                  TextureUtilLegacy.uploadTextureMipmap(
                     new int[][]{icon.getTextureData()}, icon.getIconWidth(), icon.getIconHeight(), icon.getOriginX(), icon.getOriginY(), false, false
                  );
               } catch (Throwable var7) {
                  CrashReport crashReport = CrashReport.create(var7, "Stitching texture atlas");
                  CrashReportSection crashReportCategory = crashReport.addElement("Texture being stitched together");
                  crashReportCategory.add("Atlas path", this.basePath);
                  crashReportCategory.add("Sprite", icon);
                  throw new CrashException(crashReport);
               }
            }
         }

         bufferedImage.flush();
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
      Sprite existingIcon = (Sprite)this.mapUploadedSprites.get(name);
      if (existingIcon == null) {
         existingIcon = (Sprite)this.mapRegisteredSprites.get(name);
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
}
