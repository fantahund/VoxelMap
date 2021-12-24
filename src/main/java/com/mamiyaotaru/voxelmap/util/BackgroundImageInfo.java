package com.mamiyaotaru.voxelmap.util;

import java.awt.image.BufferedImage;

public class BackgroundImageInfo {
   final BufferedImage image;
   public final int glid;
   public final int left;
   public final int top;
   private final int right;
   private final int bottom;
   public final int width;
   public final int height;
   public final float scale;

   public BackgroundImageInfo(BufferedImage image, int left, int top, float scale) {
      this(image, left, top, (int)((float)image.getWidth() * scale), (int)((float)image.getHeight() * scale));
   }

   public BackgroundImageInfo(BufferedImage image, int left, int top, int width, int height) {
      this.image = image;
      this.glid = GLUtils.tex(image);
      this.left = left;
      this.top = top;
      this.right = left + width;
      this.bottom = top + height;
      this.width = width;
      this.height = height;
      this.scale = (float)width / (float)image.getWidth();
   }

   public boolean isInRange(int x, int z) {
      return x >= this.left && x < this.right && z >= this.top && z < this.bottom;
   }

   public boolean isGroundAt(int x, int z) {
      int imageX = (int)((float)(x - this.left) / this.scale);
      int imageY = (int)((float)(z - this.top) / this.scale);
      if (imageX >= 0 && imageX < this.image.getWidth() && imageY >= 0 && imageY < this.image.getHeight()) {
         int color = this.image.getRGB(imageX, imageY);
         return (color >> 24 & 0xFF) > 0;
      } else {
         return false;
      }
   }
}
