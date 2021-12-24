package com.mamiyaotaru.voxelmap.interfaces;

public interface IGLBufferedImage {
   int getIndex();

   int getWidth();

   int getHeight();

   void baleet();

   void write();

   void blank();

   void setRGB(int var1, int var2, int var3);

   void moveX(int var1);

   void moveY(int var1);
}
