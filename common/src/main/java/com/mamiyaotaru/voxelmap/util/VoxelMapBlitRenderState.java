package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;

public record VoxelMapBlitRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x0,
        float y0,
        float x1,
        float y1,
        float u0,
        float u1,
        float v0,
        float v1,
        int color,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
    public VoxelMapBlitRenderState(
            RenderPipeline renderPipeline,
            TextureSetup textureSetup,
            Matrix3x2f matrix3x2f,
            float x0,
            float y0,
            float x1,
            float y1,
            float u0,
            float u1,
            float v0,
            float v1,
            int color,
            @Nullable ScreenRectangle screenRectangle
        ) {
            this(renderPipeline, textureSetup, matrix3x2f, x0, y0, x1, y1, u0, u1, v0, v1, color, screenRectangle, getBounds(x0, y0, x1, y1, matrix3x2f, screenRectangle));
        }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer, float f) {
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y0(), f).setUv(this.u0(), this.v0()).setColor(this.color());
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y1(), f).setUv(this.u0(), this.v1()).setColor(this.color());
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y1(), f).setUv(this.u1(), this.v1()).setColor(this.color());
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y0(), f).setUv(this.u1(), this.v0()).setColor(this.color());
    }

    @Nullable
    private static ScreenRectangle getBounds(float x0, float y0, float x1, float y1, Matrix3x2f matrix3x2f, @Nullable ScreenRectangle screenRectangle) {
        ScreenRectangle screenRectangle2 = new ScreenRectangle(Mth.floor(x0), Mth.floor(y0), Mth.ceil(x1 - x0), Mth.ceil(y1 - y0)).transformMaxBounds(matrix3x2f);
        return screenRectangle != null ? screenRectangle.intersection(screenRectangle2) : screenRectangle2;
    }
}