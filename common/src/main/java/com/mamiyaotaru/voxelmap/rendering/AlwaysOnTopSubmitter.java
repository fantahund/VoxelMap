package com.mamiyaotaru.voxelmap.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

/**
 * Submits features to Minecraft's final world-rendering pass, after translucent terrain and
 * transparency compositing.
 */
public final class AlwaysOnTopSubmitter {
    private final SimpleFeatureRenderPhase phase;

    private AlwaysOnTopSubmitter(SimpleFeatureRenderPhase phase) {
        this.phase = phase;
    }

    public static AlwaysOnTopSubmitter order(SubmitNodeCollector collector, int order) {
        OrderedSubmitNodeCollector orderedCollector = collector.order(order);
        if (!(orderedCollector instanceof SubmitNodeCollection collection)) {
            throw new IllegalStateException("Unsupported submit node collector: " + orderedCollector.getClass().getName());
        }

        return new AlwaysOnTopSubmitter(collection.alwaysOnTop);
    }

    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        phase.submit(new CustomFeatureRenderer.Submit(poseStack.last().copy(), renderType, renderer));
    }

    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        phase.submit(new TextFeatureRenderer.Submit(new Matrix4f(poseStack.last().pose()), x, y, text, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor));
    }
}
