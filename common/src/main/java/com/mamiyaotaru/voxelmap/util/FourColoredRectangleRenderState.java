package com.mamiyaotaru.voxelmap.util;

// TODO: 1.20.1 Port - RenderPipeline doesn't exist in 1.20.1
// import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record FourColoredRectangleRenderState(
        // TODO: 1.20.1 Port - Replace with proper 1.20.1 rendering type
        Object pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x0,
        float y0,
        float x1,
        float y1,
        int color00,
        int color10,
        int color01,
        int color11,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
    public FourColoredRectangleRenderState(
            RenderPipeline renderPipeline,
            TextureSetup textureSetup,
            Matrix3x2f matrix3x2f,
            float x0,
            float y0,
            float x1,
            float y1,
            int color00,
            int color10,
            int color01,
            int color11,
            @Nullable ScreenRectangle screenRectangle) {
        this(renderPipeline, textureSetup, matrix3x2f, x0, y0, x1, y1, color00, color10, color01, color11, screenRectangle, getBounds(x0, y0, x1, y1, matrix3x2f, screenRectangle));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setColor(this.color00());
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setColor(this.color01());
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setColor(this.color11());
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setColor(this.color10());
    }

    @Nullable
    private static ScreenRectangle getBounds(float i, float j, float k, float l, Matrix3x2f matrix3x2f, @Nullable ScreenRectangle screenRectangle) {
        ScreenRectangle screenRectangle2 = new ScreenRectangle(Mth.floor(i), Mth.floor(j), Mth.ceil(k - i), Mth.ceil(l - j)).transformMaxBounds(matrix3x2f);
        return screenRectangle != null ? screenRectangle.intersection(screenRectangle2) : screenRectangle2;
    }
}