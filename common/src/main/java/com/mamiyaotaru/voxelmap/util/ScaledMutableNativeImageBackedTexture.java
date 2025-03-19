package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.platform.NativeImage;

public class ScaledMutableNativeImageBackedTexture extends MutableNativeImageBackedTexture {
    private final NativeImage image;
    private final int scale;

    public ScaledMutableNativeImageBackedTexture(String label, int width, int height, boolean useStb) {
        super(label, 512, 512, useStb);
        this.scale = 512 / width;
        this.image = this.getPixels();
        String info = this.image.toString();
        String pointerString = info.substring(info.indexOf("@") + 1, info.indexOf("]") - 1);
        long pointer = Long.parseLong(pointerString);
    }

    @Override
    public int getWidth() {
        return this.image.getHeight();
    }

    @Override
    public int getHeight() {
        return this.image.getHeight();
    }

    @Override
    public void moveX(int offset) {
        super.moveX(offset * this.scale);
    }

    @Override
    public void moveY(int offset) {
        super.moveY(offset * this.scale);
    }

    @Override
    public void setRGB(int x, int y, int color24) {
        int alpha = color24 >> 24 & 0xFF;
        byte a = -1;
        byte r = (byte) ((color24 & 0xFF) * alpha / 255);
        byte g = (byte) ((color24 >> 8 & 0xFF) * alpha / 255);
        byte b = (byte) ((color24 >> 16 & 0xFF) * alpha / 255);
        int color = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;

        for (int t = 0; t < this.scale; ++t) {
            for (int s = 0; s < this.scale; ++s) {
                this.image.setPixel(x * this.scale + t, y * this.scale + s, color);
            }
        }

    }
}
