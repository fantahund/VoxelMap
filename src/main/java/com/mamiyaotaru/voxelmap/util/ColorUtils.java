package com.mamiyaotaru.voxelmap.util;

public class ColorUtils {
    public static int colorMultiplier(int color1, int color2) {
        int alpha1 = color1 >> 24 & 0xFF;
        int red1 = color1 >> 16 & 0xFF;
        int green1 = color1 >> 8 & 0xFF;
        int blue1 = color1 >> 0 & 0xFF;
        int alpha2 = color2 >> 24 & 0xFF;
        int red2 = color2 >> 16 & 0xFF;
        int green2 = color2 >> 8 & 0xFF;
        int blue2 = color2 >> 0 & 0xFF;
        int alpha = alpha1 * alpha2 / 255;
        int red = red1 * red2 / 255;
        int green = green1 * green2 / 255;
        int blue = blue1 * blue2 / 255;
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }

    public static int colorAdder(int color1, int color2) {
        float topAlpha = (float) (color1 >> 24 & 0xFF) / 255.0F;
        float red1 = (float) (color1 >> 16 & 0xFF) * topAlpha;
        float green1 = (float) (color1 >> 8 & 0xFF) * topAlpha;
        float blue1 = (float) (color1 >> 0 & 0xFF) * topAlpha;
        float bottomAlpha = (float) (color2 >> 24 & 0xFF) / 255.0F;
        float red2 = (float) (color2 >> 16 & 0xFF) * bottomAlpha * (1.0F - topAlpha);
        float green2 = (float) (color2 >> 8 & 0xFF) * bottomAlpha * (1.0F - topAlpha);
        float blue2 = (float) (color2 >> 0 & 0xFF) * bottomAlpha * (1.0F - topAlpha);
        float alpha = topAlpha + bottomAlpha * (1.0F - topAlpha);
        float red = (red1 + red2) / alpha;
        float green = (green1 + green2) / alpha;
        float blue = (blue1 + blue2) / alpha;
        return ((int) (alpha * 255.0F) & 0xFF) << 24 | ((int) red & 0xFF) << 16 | ((int) green & 0xFF) << 8 | (int) blue & 0xFF;
    }
}
