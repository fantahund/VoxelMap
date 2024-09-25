package com.mamiyaotaru.voxelmap.textures;

import java.awt.image.BufferedImage;
import net.minecraft.resources.ResourceLocation;

public class Sprite {
    private final String iconName;
    protected int[] imageData;
    protected int originX;
    protected int originY;
    protected int width;
    protected int height;
    private float minU;
    private float maxU;
    private float minV;
    private float maxV;

    public Sprite(String iconName) {
        this.iconName = iconName;
    }

    public static Sprite spriteFromResourceLocation(ResourceLocation resourceLocation) {
        String name = resourceLocation.toString();
        return spriteFromString(name);
    }

    public static Sprite spriteFromString(String name) {
        return new Sprite(name);
    }

    public void initSprite(int sheetWidth, int sheetHeight, int originX, int originY) {
        this.originX = originX;
        this.originY = originY;
        float var6 = (float) (0.01F / (double) sheetWidth);
        float var7 = (float) (0.01F / (double) sheetHeight);
        this.minU = originX / (float) ((double) sheetWidth) + var6;
        this.maxU = (originX + this.width) / (float) ((double) sheetWidth) - var6;
        this.minV = (float) originY / sheetHeight + var7;
        this.maxV = (float) (originY + this.height) / sheetHeight - var7;
    }

    public void copyFrom(Sprite sourceSprite) {
        this.originX = sourceSprite.originX;
        this.originY = sourceSprite.originY;
        this.width = sourceSprite.width;
        this.height = sourceSprite.height;
        this.minU = sourceSprite.minU;
        this.maxU = sourceSprite.maxU;
        this.minV = sourceSprite.minV;
        this.maxV = sourceSprite.maxV;
    }

    public int getOriginX() {
        return this.originX;
    }

    public int getOriginY() {
        return this.originY;
    }

    public int getIconWidth() {
        return this.width;
    }

    public int getIconHeight() {
        return this.height;
    }

    public float getMinU() {
        return this.minU;
    }

    public float getMaxU() {
        return this.maxU;
    }

    public float getMinV() {
        return this.minV;
    }

    public float getMaxV() {
        return this.maxV;
    }

    public String getIconName() {
        return this.iconName;
    }

    public int[] getTextureData() {
        return this.imageData;
    }

    public void setIconWidth(int width) {
        this.width = width;
    }

    public void setIconHeight(int height) {
        this.height = height;
    }

    public void bufferedImageToIntData(BufferedImage bufferedImage) {
        this.resetSprite();
        int var3 = bufferedImage.getWidth();
        int var4 = bufferedImage.getHeight();
        this.width = var3;
        this.height = var4;
        int[] imageData;
        imageData = new int[bufferedImage.getWidth() * bufferedImage.getHeight()];
        bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), imageData, 0, bufferedImage.getWidth());

        if (var4 != var3) {
            throw new RuntimeException("broken aspect ratio");
        } else {
            this.imageData = imageData;
        }
    }

    public void setTextureData(int[] imageData) {
        this.imageData = imageData;
    }

    private void resetSprite() {
        this.imageData = new int[0];
    }

    public String toString() {
        return "Sprite{name='" + this.iconName + "', x=" + this.originX + ", y=" + this.originY + ", height=" + this.height + ", width=" + this.width + ", u0=" + this.minU + ", u1=" + this.maxU + ", v0=" + this.minV + ", v1=" + this.maxV + "}";
    }
}
