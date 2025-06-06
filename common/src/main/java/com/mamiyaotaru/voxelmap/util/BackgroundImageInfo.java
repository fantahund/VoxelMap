package com.mamiyaotaru.voxelmap.util;

import java.awt.image.BufferedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class BackgroundImageInfo {
    private ResourceLocation imageLocation;
    public final int left;
    public final int top;
    private final int right;
    private final int bottom;
    public final int width;
    public final int height;
    public final float scale;

    public BackgroundImageInfo(ResourceLocation imageLocation, BufferedImage image, int left, int top, float scale) {
        this(imageLocation, image, left, top, (int) (image.getWidth() * scale), (int) (image.getHeight() * scale));
    }

    public BackgroundImageInfo(ResourceLocation imageLocation, BufferedImage image, int left, int top, int width, int height) {
        this.imageLocation = imageLocation;
        this.left = left;
        this.top = top;
        this.right = left + width;
        this.bottom = top + height;
        this.width = width;
        this.height = height;
        this.scale = width / (float) image.getWidth();
    }

    public boolean isInRange(int x, int z) { return x >= left && x < right && z >= top && z < bottom; }

    public void unregister() {
        Minecraft.getInstance().getTextureManager().release(imageLocation);
    }

    public ResourceLocation getImageLocation() {
        return imageLocation;
    }
}
