package com.mamiyaotaru.voxelmap.util;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.lwjgl.system.MemoryUtil;

public class MutableNativeImageBackedTexture extends NativeImageBackedTexture {
   private Object bufferLock = new Object();
   private NativeImage image;
   private long pointer;

   public MutableNativeImageBackedTexture(NativeImage image) {
      super(image);
      this.image = image;
      String info = image.toString();
      String pointerString = info.substring(info.indexOf("(") + 1, info.indexOf("]") - 1);
      this.pointer = Long.parseLong(pointerString);
   }

   public MutableNativeImageBackedTexture(int width, int height, boolean b) {
      super(width, height, b);
      this.image = this.getImage();
      String info = this.image.toString();
      String pointerString = info.substring(info.indexOf("@") + 1, info.indexOf("]") - 1);
      this.pointer = Long.parseLong(pointerString);
   }

   public void blank() {
   }

   public void write() {
      this.upload();
   }

   public int getWidth() {
      return this.image.getHeight();
   }

   public int getHeight() {
      return this.image.getHeight();
   }

   public int getIndex() {
      return this.getGlId();
   }

   public void moveX(int offset) {
      synchronized(this.bufferLock) {
         int size = this.image.getHeight() * this.image.getWidth() * 4;
         if (offset > 0) {
            MemoryUtil.memCopy(this.pointer + (long)(offset * 4), this.pointer, (long)(size - offset * 4));
         } else if (offset < 0) {
            MemoryUtil.memCopy(this.pointer, this.pointer - (long)(offset * 4), (long)(size + offset * 4));
         }

      }
   }

   public void moveY(int offset) {
      synchronized(this.bufferLock) {
         int size = this.image.getHeight() * this.image.getWidth() * 4;
         int width = this.image.getWidth();
         if (offset > 0) {
            MemoryUtil.memCopy(this.pointer + (long)(offset * width * 4), this.pointer, (long)(size - offset * width * 4));
         } else if (offset < 0) {
            MemoryUtil.memCopy(this.pointer, this.pointer - (long)(offset * width * 4), (long)(size + offset * width * 4));
         }

      }
   }

   public void setRGB(int x, int y, int color24) {
      int alpha = color24 >> 24 & 0xFF;
      byte a = -1;
      byte r = (byte)((color24 >> 0 & 0xFF) * alpha / 255);
      byte g = (byte)((color24 >> 8 & 0xFF) * alpha / 255);
      byte b = (byte)((color24 >> 16 & 0xFF) * alpha / 255);
      int color = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
      this.image.setColor(x, y, color);
   }
}
