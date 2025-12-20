package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.function.Function;

public class VoxelMapGuiGraphics {

    // Renders a textured quad with gradient colors using 1.20.1 API
    public static void blitFloatGradient(GuiGraphics graphics, Object pipelineObj, AbstractTexture texture,
                                         float x, float y, float w, float h,
                                         float minu, float maxu, float minv, float maxv,
                                         int color, int color2) {
        // Extract the texture ID from the AbstractTexture
        int textureId = texture.getId();
        RenderSystem.setShaderTexture(0, textureId);

        innerBlit(graphics, x, x + w, y, y + h, 0, minu, maxu, minv, maxv, color, color2);
    }

    public static void blitFloat(GuiGraphics graphics, Object pipeline, AbstractTexture texture,
                                 float x, float y, float w, float h,
                                 float minu, float maxu, float minv, float maxv, int color) {
        blitFloatGradient(graphics, pipeline, texture, x, y, w, h, minu, maxu, minv, maxv, color, color);
    }

    public static void blitFloatGradient(GuiGraphics graphics, Object pipeline, ResourceLocation texture,
                                         float x, float y, float w, float h,
                                         float minu, float maxu, float minv, float maxv,
                                         int color, int color2) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        innerBlit(graphics, x, x + w, y, y + h, 0, minu, maxu, minv, maxv, color, color2);
    }

    public static void blitFloat(GuiGraphics graphics, Object pipeline, ResourceLocation texture,
                                 float x, float y, float w, float h,
                                 float minu, float maxu, float minv, float maxv, int color) {
        blitFloatGradient(graphics, pipeline, texture, x, y, w, h, minu, maxu, minv, maxv, color, color);
    }

    // Inner blit method that handles the actual vertex building
    private static void innerBlit(GuiGraphics graphics, float x1, float x2, float y1, float y2, float blitOffset,
                                   float minU, float maxU, float minV, float maxV, int color1, int color2) {
        Matrix4f matrix = graphics.pose().last().pose();

        // Extract RGBA components
        float a1 = (float)(color1 >> 24 & 255) / 255.0F;
        float r1 = (float)(color1 >> 16 & 255) / 255.0F;
        float g1 = (float)(color1 >> 8 & 255) / 255.0F;
        float b1 = (float)(color1 & 255) / 255.0F;

        float a2 = (float)(color2 >> 24 & 255) / 255.0F;
        float r2 = (float)(color2 >> 16 & 255) / 255.0F;
        float g2 = (float)(color2 >> 8 & 255) / 255.0F;
        float b2 = (float)(color2 & 255) / 255.0F;

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.vertex(matrix, x1, y2, blitOffset).uv(minU, maxV).color(r2, g2, b2, a2).endVertex();
        bufferBuilder.vertex(matrix, x2, y2, blitOffset).uv(maxU, maxV).color(r2, g2, b2, a2).endVertex();
        bufferBuilder.vertex(matrix, x2, y1, blitOffset).uv(maxU, minV).color(r1, g1, b1, a1).endVertex();
        bufferBuilder.vertex(matrix, x1, y1, blitOffset).uv(minU, minV).color(r1, g1, b1, a1).endVertex();
        Tesselator.getInstance().end();
    }

    // Renders a gradient-filled rectangle
    public static void fillGradient(GuiGraphics graphics, float x0, float y0, float x1, float y1,
                                     int color00, int color10, int color01, int color11) {
        Matrix4f matrix = graphics.pose().last().pose();

        // Extract RGBA components for each corner
        float a00 = (float)(color00 >> 24 & 255) / 255.0F;
        float r00 = (float)(color00 >> 16 & 255) / 255.0F;
        float g00 = (float)(color00 >> 8 & 255) / 255.0F;
        float b00 = (float)(color00 & 255) / 255.0F;

        float a10 = (float)(color10 >> 24 & 255) / 255.0F;
        float r10 = (float)(color10 >> 16 & 255) / 255.0F;
        float g10 = (float)(color10 >> 8 & 255) / 255.0F;
        float b10 = (float)(color10 & 255) / 255.0F;

        float a01 = (float)(color01 >> 24 & 255) / 255.0F;
        float r01 = (float)(color01 >> 16 & 255) / 255.0F;
        float g01 = (float)(color01 >> 8 & 255) / 255.0F;
        float b01 = (float)(color01 & 255) / 255.0F;

        float a11 = (float)(color11 >> 24 & 255) / 255.0F;
        float r11 = (float)(color11 >> 16 & 255) / 255.0F;
        float g11 = (float)(color11 >> 8 & 255) / 255.0F;
        float b11 = (float)(color11 >> 8 & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x0, y1, 0).color(r01, g01, b01, a01).endVertex();
        bufferBuilder.vertex(matrix, x1, y1, 0).color(r11, g11, b11, a11).endVertex();
        bufferBuilder.vertex(matrix, x1, y0, 0).color(r10, g10, b10, a10).endVertex();
        bufferBuilder.vertex(matrix, x0, y0, 0).color(r00, g00, b00, a00).endVertex();
        Tesselator.getInstance().end();

        RenderSystem.disableBlend();
    }
}
