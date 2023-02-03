package com.mamiyaotaru.voxelmap.interfaces;

public interface IGLBufferedImage {
    int getIndex();

    int getWidth();

    int getHeight();

    void baleet();

    void write();

    void setRGB(int x, int y, int color);
}
