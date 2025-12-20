package com.mamiyaotaru.voxelmap.textures;

import com.mamiyaotaru.voxelmap.util.VoxelMapGuiGraphics;
// TODO: 1.20.1 Port - RenderPipeline doesn't exist in 1.20.1
// import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class Sprite {
    private final Object iconName;
    protected NativeImage imageData;
    protected int originX;
    protected int originY;
    protected int width;
    protected int height;
    private float minU;
    private float maxU;
    private float minV;
    private float maxV;
    private TextureAtlas textureAtlas;

    public Sprite(Object iconName, TextureAtlas textureAtlas) {
        this.iconName = iconName;
        this.textureAtlas = textureAtlas;
    }

    public static Sprite spriteFromIdentifier(ResourceLocation ResourceLocation, TextureAtlas textureAtlas) {
        String name = ResourceLocation.toString();
        return spriteFromString(name, textureAtlas);
    }

    public static Sprite spriteFromString(Object name, TextureAtlas textureAtlas) {
        return new Sprite(name, textureAtlas);
    }

    public void initSprite(int sheetWidth, int sheetHeight, int originX, int originY) {
        this.originX = originX;
        this.originY = originY;
        float var6 = (float) (0.01F / (double) sheetWidth);
        float var7 = (float) (0.01F / (double) sheetHeight);
        this.minU = originX / (float) (sheetWidth) + var6;
        this.maxU = (originX + this.width) / (float) (sheetWidth) - var6;
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

    public Object getIconName() {
        return this.iconName;
    }

    public NativeImage getTextureData() {
        return this.imageData;
    }

    public void setIconWidth(int width) {
        this.width = width;
    }

    public void setIconHeight(int height) {
        this.height = height;
    }

    public void setTextureData(NativeImage imageData) {
        if (this.imageData != null) {
            this.imageData.close();
        }
        this.imageData = imageData;
        if (imageData != null) {
            this.width = imageData.getWidth();
            this.height = imageData.getHeight();
        }
    }

    @Override
    public String toString() {
        return "Sprite{name='" + this.iconName + "', x=" + this.originX + ", y=" + this.originY + ", height=" + this.height + ", width=" + this.width + ", u0=" + this.minU + ", u1=" + this.maxU + ", v0=" + this.minV + ", v1=" + this.maxV + "}";
    }

    public void blit(GuiGraphics guiGraphics, Object renderTypeMap, float x, float y, float w, float h) {
        blit(guiGraphics, renderTypeMap, x, y, w, h, 0xffffffff);
    }

    public void blit(GuiGraphics guiGraphics, Object renderTypeMap, float x, float y, float w, float h, int color) {
        VoxelMapGuiGraphics.blitFloat(guiGraphics, renderTypeMap, getIdentifier(), x, y, w, h, minU, maxU, minV, maxV, color);
    }

    public ResourceLocation getIdentifier() {
        return textureAtlas.getIdentifier();
    }
}
