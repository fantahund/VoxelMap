package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

public class VoxelMapGuiGraphics {
    private static final GpuSampler DEFAULT_SAMPLER = RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.LINEAR, false);

    public static void blitFloatGradient(GuiGraphics graphics, RenderPipeline pipeline, GpuTextureView texture, GpuSampler sampler, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color, int color2) {
        graphics.guiRenderState.submitGuiElement(new FloatBlitRenderState(pipeline, TextureSetup.singleTexture(texture, sampler),
                new Matrix3x2f(graphics.pose()), x, y, x + w, y + h,
                minu, maxu, minv, maxv, color, color2, graphics.scissorStack.peek()));
    }

    public static void blitFloatGradient(GuiGraphics graphics, RenderPipeline pipeline, AbstractTexture texture, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color, int color2) {
        try {
            GpuTextureView textureView = texture.getTextureView();
            GpuSampler sampler = texture.getSampler();
            if (textureView != null && sampler != null) {
                blitFloatGradient(graphics, pipeline, textureView, sampler, x, y, w, h, minu, maxu, minv, maxv, color, color2);
            }
        } catch (IllegalStateException ignored) {
        }
    }

    public static void blitFloat(GuiGraphics graphics, RenderPipeline pipeline, AbstractTexture texture, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color) {
        blitFloatGradient(graphics, pipeline, texture, x, y, w, h, minu, maxu, minv, maxv, color, color);
    }

    public static void blitFloat(GuiGraphics graphics, RenderPipeline pipeline, GpuTextureView texture, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color) {
        blitFloatGradient(graphics, pipeline, texture, DEFAULT_SAMPLER, x, y, w, h, minu, maxu, minv, maxv, color, color);
    }

    public static void blitFloatGradient(GuiGraphics graphics, RenderPipeline pipeline, Identifier texture, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color, int color2) {
        blitFloatGradient(graphics, pipeline, Minecraft.getInstance().getTextureManager().getTexture(texture), x, y, w, h, minu, maxu, minv, maxv, color, color2);
    }

    public static void blitFloat(GuiGraphics graphics, RenderPipeline pipeline, Identifier texture, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color) {
        blitFloatGradient(graphics, pipeline, Minecraft.getInstance().getTextureManager().getTexture(texture), x, y, w, h, minu, maxu, minv, maxv, color, color);
    }

    public static void fillGradient(GuiGraphics graphics, float x0, float y0, float x1, float y1, int color00, int color10, int color01, int color11) {
        graphics.guiRenderState.submitGuiElement(new FourColoredRectangleRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()), x0, y0, x1, y1, color00, color10, color01, color11, graphics.scissorStack.peek()));
    }
}