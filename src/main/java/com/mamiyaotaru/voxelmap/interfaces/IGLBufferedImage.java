package com.mamiyaotaru.voxelmap.interfaces;

public interface IGLBufferedImage {
    int getIndex();

    int getWidth();

    int getHeight();

    void baleet();

    void write();

    void blank();

    void setRGB(int x, int y, int color);

    void moveX(int x);

    void moveY(int y);
}
