package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public class RenderUtils {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final Projection FULLSCREEN_PROJECTION  = new Projection();
    private static final ProjectionMatrixBuffer FULLSCREEN_PROJECTION_MATRIX = new ProjectionMatrixBuffer("VoxelMap Fullscreen Projection Matrix");
    static { FULLSCREEN_PROJECTION.setupOrtho(1000.0F, 3000.0F, 0.0F, 0.0F, true); }

    public static float getGuiWidth() {
        return (float) MINECRAFT.getWindow().getWidth() / MINECRAFT.getWindow().getGuiScale();
    }

    public static float getGuiHeight() {
        return (float) MINECRAFT.getWindow().getHeight() / MINECRAFT.getWindow().getGuiScale();
    }

    public static void submitTexturedModalRect(OrderedSubmitNodeCollector submitNodeCollector, Matrix4fStack matrixStack, RenderType renderType, float x, float y, float z, float width, float height, int color) {
        submitTexturedModalRect(submitNodeCollector, matrixStack, renderType, x, y, z, width, height, 0.0F, 1.0F, 0.0F, 1.0F, color);
    }

    public static void submitTexturedModalRect(OrderedSubmitNodeCollector submitNodeCollector, Matrix4fStack matrixStack, RenderType renderType, Sprite sprite, float x, float y, float z, float width, float height, int color) {
        submitTexturedModalRect(submitNodeCollector, matrixStack, renderType, x, y, z, width, height, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), color);
    }

    public static void submitTexturedModalRect(OrderedSubmitNodeCollector submitNodeCollector, Matrix4fStack matrixStack, RenderType renderType, float x, float y, float z, float width, float height, float u0, float u1, float v0, float v1, int color) {
        PoseStack poseStack = poseStackFor(matrixStack);
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
            vertexConsumer.addVertex(pose, x + 0.0F, y + 0.0F, z).setUv(u0, v0).setColor(color);
            vertexConsumer.addVertex(pose, x + 0.0F, y + height, z).setUv(u0, v1).setColor(color);
            vertexConsumer.addVertex(pose, x + width, y + height, z).setUv(u1, v1).setColor(color);
            vertexConsumer.addVertex(pose, x + width, y + 0.0F, z).setUv(u1, v0).setColor(color);
        });
    }

    public static void submitString(OrderedSubmitNodeCollector submitNodeCollector, Matrix4fStack matrixStack, String text, float x, float y, float z, int color, boolean shadow) {
        submitString(submitNodeCollector, matrixStack, Component.nullToEmpty(text), x, y, z, color, shadow);
    }

    public static void submitString(OrderedSubmitNodeCollector submitNodeCollector, Matrix4fStack matrixStack, Component text, float x, float y, float z, int color, boolean shadow) {
        matrixStack.pushMatrix();
        matrixStack.translate(x, y, z);
        submitPreparedText(submitNodeCollector, matrixStack, text.getVisualOrderText(), 0.0F, 0.0F, color, shadow, Font.DisplayMode.SEE_THROUGH, 0, 0x00F000F0);

        matrixStack.popMatrix();
    }

    public static void submitCenteredString(OrderedSubmitNodeCollector submitNodeCollector, Matrix4fStack matrixStack, String text, float x, float y, float z, int color, boolean shadow) {
        submitCenteredString(submitNodeCollector, matrixStack, Component.nullToEmpty(text), x, y, z, color, shadow);
    }

    public static void submitCenteredString(OrderedSubmitNodeCollector submitNodeCollector, Matrix4fStack matrixStack, Component text, float x, float y, float z, int color, boolean shadow) {
        submitString(submitNodeCollector, matrixStack, text, x - (MINECRAFT.font.width(text) / 2.0F), y, z, color, shadow);
    }

    public static void submitPreparedText(OrderedSubmitNodeCollector submitNodeCollector, Matrix4fStack matrixStack, FormattedCharSequence text, float x, float y, int color, boolean shadow, Font.DisplayMode displayMode, int backgroundColor, int light) {
        submitNodeCollector.submitText(poseStackFor(matrixStack), x, y, text, shadow, displayMode, light, color, backgroundColor, 0);
    }

    private static PoseStack poseStackFor(Matrix4fStack matrixStack) {
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(matrixStack);
        return poseStack;
    }

    public static void renderWithCustomProjection(RenderTarget renderTarget, GpuBufferSlice projection, float initialDepth, Consumer<SubmitContext> submitter) {
        RenderSystem.assertOnRenderThread();

        if (renderTarget.getColorTexture() == null || renderTarget.getDepthTexture() == null) {
            return;
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(renderTarget.getColorTexture(), new Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(renderTarget.getDepthTexture(), 1.0);

        GpuBufferSlice lastProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType lastProjectionType = RenderSystem.getProjectionType();
        GpuTextureView lastColorTexture = RenderSystem.outputColorTextureOverride;
        GpuTextureView lastDepthTexture = RenderSystem.outputDepthTextureOverride;

        try {
            RenderSystem.setProjectionMatrix(projection, ProjectionType.ORTHOGRAPHIC);
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            RenderSystem.getModelViewStack().translate(0.0F, 0.0F, initialDepth);
            RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();

            SubmitContext context = new SubmitContext();
            submitter.accept(context);
            context.flush();
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Failed to render with custom projection. Exception: " + e);
        } finally {
            RenderSystem.outputColorTextureOverride = lastColorTexture;
            RenderSystem.outputDepthTextureOverride = lastDepthTexture;
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(lastProjectionMatrix, lastProjectionType);
        }

        GLUtils.flipTexture(renderTarget.getColorTextureView(), false, true);
    }

    public static void renderWithFullscreenProjection(RenderTarget renderTarget, Consumer<SubmitContext> submitter) {
        RenderSystem.assertOnRenderThread();

        if (renderTarget.getColorTexture() == null || renderTarget.getDepthTexture() == null) {
            return;
        }

        int windowWidth = MINECRAFT.getWindow().getWidth();
        int windowHeight = MINECRAFT.getWindow().getHeight();

        if (renderTarget.width != windowWidth || renderTarget.height != windowHeight) {
            renderTarget.resize(windowWidth, windowHeight);
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(renderTarget.getColorTexture(), new Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(renderTarget.getDepthTexture(), 1.0);

        GpuBufferSlice lastProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType lastProjectionType = RenderSystem.getProjectionType();
        GpuTextureView lastColorTexture = RenderSystem.outputColorTextureOverride;
        GpuTextureView lastDepthTexture = RenderSystem.outputDepthTextureOverride;

        try {
            FULLSCREEN_PROJECTION.setSize(getGuiWidth(), getGuiHeight());
            RenderSystem.setProjectionMatrix(FULLSCREEN_PROJECTION_MATRIX.getBuffer(FULLSCREEN_PROJECTION), ProjectionType.ORTHOGRAPHIC);
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            RenderSystem.getModelViewStack().translate(0.0F, 0.0F, -2000.0F);
            RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();

            SubmitContext context = new SubmitContext();
            submitter.accept(context);
            context.flush();
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Failed to render with fullscreen projection. Exception: " + e);
        } finally {
            RenderSystem.outputColorTextureOverride = lastColorTexture;
            RenderSystem.outputDepthTextureOverride = lastDepthTexture;
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(lastProjectionMatrix, lastProjectionType);
        }

        GLUtils.flipTexture(renderTarget.getColorTextureView(), false, true);
    }

    public static void drawMeshWithTexture(GpuTextureView colorTexture, GpuTextureView depthTexture, GpuBufferSlice projection, float initialDepth, MeshData meshData, RenderPipeline pipeline, TextureSetup textureSetup) {
        if (meshData == null) {
            return;
        }

        GpuBufferSlice lastProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType lastProjectionType = RenderSystem.getProjectionType();
        try {
            RenderSystem.setProjectionMatrix(projection, ProjectionType.ORTHOGRAPHIC);
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            RenderSystem.getModelViewStack().translate(0.0F, 0.0F, initialDepth);
            GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().writeTransform(
                    RenderSystem.getModelViewMatrixCopy(),
                    new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                    new Vector3f(),
                    new Matrix4f());
            GpuBuffer vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap Immediate Vertex Buffer", GpuBuffer.USAGE_VERTEX, meshData.vertexBuffer());
            GpuBuffer indexBuffer;
            boolean closeIndexBuffer = false;
            IndexType indexType;
            if (meshData.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().primitiveTopology());
                indexBuffer = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
                indexType = autoStorageIndexBuffer.type();
            } else {
                indexBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap Immediate Index Buffer", GpuBuffer.USAGE_INDEX, meshData.indexBuffer());
                indexType = meshData.drawState().indexType();
                closeIndexBuffer = true;
            }
            Optional<Vector4fc> colorClear = Optional.of(new Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
            OptionalDouble depthClear = depthTexture == null ? OptionalDouble.empty() : OptionalDouble.of(1.0);
            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "VoxelMap Immediate Draw", colorTexture, colorClear, depthTexture, depthClear)) {
                renderPass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                renderPass.setVertexBuffer(0, vertexBuffer.slice());
                renderPass.setIndexBuffer(indexBuffer, indexType);
                renderPass.bindTexture("Sampler0", textureSetup.texure0(), textureSetup.sampler0());
                renderPass.bindTexture("Sampler1", textureSetup.texure1(), textureSetup.sampler1());
                renderPass.bindTexture("Sampler2", textureSetup.texure2(), textureSetup.sampler2());
                renderPass.drawIndexed(meshData.drawState().indexCount(), 1, 0, 0, 0);
            } finally {
                vertexBuffer.close();
                if (closeIndexBuffer) {
                    indexBuffer.close();
                }
            }
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Immediate draw failed. Exception: " + e);
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(lastProjectionMatrix, lastProjectionType);
        }
    }

    public static class SubmitContext {
        private SubmitNodeStorage storage = new SubmitNodeStorage();

        public SubmitNodeCollector collector() {
            return storage;
        }

        public OrderedSubmitNodeCollector order(int order) {
            return storage.order(order);
        }

        public void flush() {
            MINECRAFT.gameRenderer.featureRenderDispatcher().renderAllFeatures(storage);
            storage = new SubmitNodeStorage();
        }
    }
}
