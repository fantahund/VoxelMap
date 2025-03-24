package com.mamiyaotaru.voxelmap.util;

public class ScaledDynamicMutableTexture extends DynamicMoveableTexture {
    private final int scale;

    public ScaledDynamicMutableTexture(String label, int width, int height, boolean clear) {
        super(label, 512, 512, clear);
        this.scale = 512 / width;
    }

    @Override
    public int getWidth() {
        return this.getPixels().getWidth();
    }

    @Override
    public int getHeight() {
        return this.getPixels().getHeight();
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
                this.getPixels().setPixel(x * this.scale + t, y * this.scale + s, color);
            }
        }

    }
}
