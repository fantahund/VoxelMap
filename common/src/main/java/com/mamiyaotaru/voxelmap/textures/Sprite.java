package com.mamiyaotaru.voxelmap.textures;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

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

    public static Sprite spriteFromResourceLocation(ResourceLocation resourceLocation, TextureAtlas textureAtlas) {
        String name = resourceLocation.toString();
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

    public String toString() {
        return "Sprite{name='" + this.iconName + "', x=" + this.originX + ", y=" + this.originY + ", height=" + this.height + ", width=" + this.width + ", u0=" + this.minU + ", u1=" + this.maxU + ", v0=" + this.minV + ", v1=" + this.maxV + "}";
    }

    public void blit(GuiGraphics guiGraphics, RenderPipeline renderTypeMap, float x, float y, float w, float h) {
        blit(guiGraphics, renderTypeMap, x, y, w, h, 0xffffffff);
    }

    public void blit(GuiGraphics guiGraphics, RenderPipeline renderTypeMap, float x, float y, float w, float h, int color) {
        // FIXME 1.21.6 Draw Sprite
        guiGraphics.blit(getResourceLocation(), (int) x, (int) y, (int) (x + w), (int) (y + h), getMinU(), getMinV(), getMaxU(), getMaxV());
        // guiGraphics.drawSpecial(bufferSource -> {
        // RenderType renderType = renderTypeMap.apply(textureAtlas.getResourceLocation());
        // Matrix4f matrix4f = guiGraphics.pose().last().pose();
        // VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        // vertexConsumer.addVertex(matrix4f, x, y, 0.0F).setUv(getMinU(), getMinV()).setColor(color);
        // vertexConsumer.addVertex(matrix4f, x, y + h, 0.0F).setUv(getMinU(), getMaxV()).setColor(color);
        // vertexConsumer.addVertex(matrix4f, x + w, y + h, 0.0F).setUv(getMaxU(), getMaxV()).setColor(color);
        // vertexConsumer.addVertex(matrix4f, x + w, y, 0.0F).setUv(getMaxU(), getMinV()).setColor(color);
        // });
    }

    public ResourceLocation getResourceLocation() {
        return textureAtlas.getResourceLocation();
    }
}
