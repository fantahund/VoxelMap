package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3x2f;

public class VoxelmapGuiGraphics {
    public static void blitFloat(GuiGraphics graphics, RenderPipeline pipeline, GpuTextureView textureView, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color) {
        graphics.guiRenderState.submitGuiElement(new FloatBlitRenderState(pipeline, TextureSetup.singleTexture(textureView),
                new Matrix3x2f(graphics.pose()), x, y, x + w, y + h,
                minu, maxu, minv, maxv, color, graphics.scissorStack.peek()));
    }

    public static void blitFloat(GuiGraphics graphics, RenderPipeline pipeline, ResourceLocation texture, float x, float y, float w, float h, float minu, float maxu, float minv, float maxv, int color) {
        blitFloat(graphics, pipeline, Minecraft.getInstance().getTextureManager().getTexture(texture).getTextureView(), x, y, w, h, minu, maxu, minv, maxv, color);
    }
}
