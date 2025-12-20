package com.mamiyaotaru.voxelmap.util;

// TODO: 1.20.1 Port - RenderPipeline doesn't exist in 1.20.1
// import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
// TODO: 1.20.1 Port - AddressMode, FilterMode don't exist in 1.20.1
// import com.mojang.blaze3d.textures.AddressMode;
// import com.mojang.blaze3d.textures.FilterMode;
// TODO: 1.20.1 Port - GpuSampler doesn't exist in 1.20.1
// import com.mojang.blaze3d.textures.GpuSampler;
// TODO: 1.20.1 Port - GpuTextureView doesn't exist in 1.20.1
// import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
// TODO: 1.20.1 Port - TextureSetup doesn't exist in 1.20.1
// import net.minecraft.client.gui.render.TextureSetup;
// TODO: 1.20.1 Port - RenderPipelines doesn't exist in 1.20.1
// import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3x2f;

public class VoxelMapGuiGraphics {
    // TODO: 1.20.1 Port - GpuSampler doesn't exist in 1.20.1
    // private static final GpuSampler DEFAULT_SAMPLER = RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.LINEAR, false);

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 rendering type
    // TODO: 1.20.1 Port - GpuTextureView and GpuSampler don't exist in 1.20.1
    // public static void blitFloatGradient(GuiGraphics graphics, Object pipeline, GpuTextureView texture, GpuSampler sampler, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color, int color2) {
        // TODO: 1.20.1 Port - guiRenderState and scissorStack don't exist in 1.20.1
        // graphics.guiRenderState.submitGuiElement(new FloatBlitRenderState(pipeline, TextureSetup.singleTexture(texture, sampler),
        //         new Matrix3x2f(graphics.pose()), x, y, x + w, y + h,
        //         minu, maxu, minv, maxv, color, color2, graphics.scissorStack.peek()));
    // }

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 rendering type
    public static void blitFloatGradient(GuiGraphics graphics, Object pipeline, AbstractTexture texture, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color, int color2) {
        // TODO: 1.20.1 Port - getTextureView() and getSampler() don't exist in 1.20.1
        // blitFloatGradient(graphics, pipeline, texture.getTextureView(), texture.getSampler(), x, y, w, h, minu, maxu, minv, maxv, color, color2);
    }

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 rendering type
    public static void blitFloat(GuiGraphics graphics, Object pipeline, AbstractTexture texture, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color) {
        blitFloatGradient(graphics, pipeline, texture, x, y, w, h, minu, maxu, minv, maxv, color, color);
    }

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 rendering type
    // TODO: 1.20.1 Port - GpuTextureView doesn't exist in 1.20.1
    // public static void blitFloat(GuiGraphics graphics, Object pipeline, GpuTextureView texture, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color) {
        // blitFloatGradient(graphics, pipeline, texture, DEFAULT_SAMPLER, x, y, w, h, minu, maxu, minv, maxv, color, color);
    // }

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 rendering type
    public static void blitFloatGradient(GuiGraphics graphics, Object pipeline, ResourceLocation texture, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color, int color2) {
        blitFloatGradient(graphics, pipeline, Minecraft.getInstance().getTextureManager().getTexture(texture), x, y, w, h, minu, maxu, minv, maxv, color, color2);
    }

    // TODO: 1.20.1 Port - Replace with proper 1.20.1 rendering type
    public static void blitFloat(GuiGraphics graphics, Object pipeline, ResourceLocation texture, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color) {
        blitFloatGradient(graphics, pipeline, Minecraft.getInstance().getTextureManager().getTexture(texture), x, y, w, h, minu, maxu, minv, maxv, color, color);
    }

    public static void fillGradient(GuiGraphics graphics, float x0, float y0, float x1, float y1, int color00, int color10, int color01, int color11) {
        // TODO: 1.20.1 Port - guiRenderState and scissorStack don't exist in 1.20.1
        // graphics.guiRenderState.submitGuiElement(new FourColoredRectangleRenderState(
        //         // TODO: 1.20.1 Port - RenderPipelines.GUI doesn't exist in 1.20.1
        //         null, // RenderPipelines.GUI
        //         TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()), x0, y0, x1, y1, color00, color10, color01, color11, graphics.scissorStack.peek()));
    }
}
