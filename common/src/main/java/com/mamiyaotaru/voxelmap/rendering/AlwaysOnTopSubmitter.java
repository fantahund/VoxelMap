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
    // Minecraft 26.3 schedules this pass from the vanilla gizmo state, before considering custom
    // features. MixinWorldRenderer includes this per-frame flag in that scheduling decision.
    private static boolean frameHasSubmissions;

    private final SimpleFeatureRenderPhase phase;

    private AlwaysOnTopSubmitter(SimpleFeatureRenderPhase phase) {
        this.phase = phase;
    }

    public static AlwaysOnTopSubmitter order(SubmitNodeCollector collector, int order) {
        OrderedSubmitNodeCollector orderedCollector = collector.order(order);
        if (!(orderedCollector instanceof SubmitNodeCollection collection)) {
            throw new IllegalStateException("Unsupported submit node collector: " + orderedCollector.getClass().getName());
        }

        return new AlwaysOnTopSubmitter(collection.alwaysOnTopGizmos);
    }

    public static void beginFrame() {
        frameHasSubmissions = false;
    }

    public static boolean hasSubmissions() {
        return frameHasSubmissions;
    }

    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        phase.submit(new CustomFeatureRenderer.Submit(poseStack.last().copy(), renderType, renderer));
        frameHasSubmissions = true;
    }

    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        TextFeatureRenderer.Content content = new TextFeatureRenderer.Content.Text(x, y, text, dropShadow, color, backgroundColor, outlineColor);
        phase.submit(new TextFeatureRenderer.Submit(new Matrix4f(poseStack.last().pose()), displayMode, lightCoords, content));
        frameHasSubmissions = true;
    }
}
