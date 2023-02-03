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

    public BackgroundImageInfo(BufferedImage image, int left, int top, float scale) { this (image, left, top, (int) (image.getWidth() * scale), (int) (image.getHeight() * scale)); }

    public BackgroundImageInfo(BufferedImage image, int left, int top, int width, int height) {
        this.image = image;
        this.glid = GLUtils.tex(image);
        this.left = left;
        this.top = top;
        this.right = left + width;
        this.bottom = top + height;
        this.width = width;
        this.height = height;
        this.scale = width / (float) image.getWidth();
    }

    public boolean isInRange(int x, int z) { return x >= left && x < right && z >= top && z < bottom; }
}
