package com.mamiyaotaru.voxelmap.util;

public class LiveScaledGLBufferedImage extends LiveGLBufferedImage {
   private int scale = 1;

   public LiveScaledGLBufferedImage(int width, int height, int imageType) {
      super(512, 512, imageType);
      this.scale = 512 / width;
   }

   @Override
   public void setRGB(int x, int y, int color24) {
      int alpha = color24 >> 24 & 0xFF;
      byte r = (byte)((color24 >> 0 & 0xFF) * alpha / 255);
      byte g = (byte)((color24 >> 8 & 0xFF) * alpha / 255);
      byte b = (byte)((color24 >> 16 & 0xFF) * alpha / 255);
      synchronized(this.bufferLock) {
         for(int t = 0; t < this.scale; ++t) {
            for(int s = 0; s < this.scale; ++s) {
               int index = (x * this.scale + t + (y * this.scale + s) * this.getWidth()) * 4;
               this.bytes[index] = -1;
               this.bytes[index + 1] = r;
               this.bytes[index + 2] = g;
               this.bytes[index + 3] = b;
            }
         }

      }
   }

   @Override
   public void moveX(int offset) {
      super.moveX(offset * this.scale);
   }

   @Override
   public void moveY(int offset) {
      super.moveY(offset * this.scale);
   }
}
