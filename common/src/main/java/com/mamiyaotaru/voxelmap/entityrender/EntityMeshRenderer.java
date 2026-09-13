package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.rendering.CachedProjectionMatrixBuffer;
import com.mamiyaotaru.voxelmap.rendering.RenderUtils;
import com.mamiyaotaru.voxelmap.rendering.VoxelMapRenderTarget;
import com.mamiyaotaru.voxelmap.rendering.VoxelMapSamplers;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public class EntityMeshRenderer {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final PoseStack poseStack = new PoseStack();
    private final GpuBuffer lightingBuffer;
    private final StagedVertexBuffer stagedVertexBuffer;
    private final CachedProjectionMatrixBuffer projection;
    private final ScissorState scissorState = new ScissorState();
    private final VoxelMapRenderTarget renderTarget;

    private boolean isBatching = false;
    private RenderPipeline pipeline;
    private VariantDataHolder variant;
    private StagedVertexBuffer.Draw draw;
    private VertexConsumer vertexBuffer;

    public EntityMeshRenderer() {
        Vector3f fullBright = new Vector3f(1.0F, -1.0F, 1.0F).normalize();
        Vector3f fullBright2 = new Vector3f(-1.0F, -1.0F, 1.0F).normalize();
        lightingBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap Lighting UBO", GpuBuffer.USAGE_UNIFORM + GpuBuffer.USAGE_COPY_DST, Lighting.UBO_SIZE);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, Lighting.UBO_SIZE).putVec3(fullBright).putVec3(fullBright2).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(lightingBuffer.slice(), byteBuffer);
        }

        stagedVertexBuffer = new StagedVertexBuffer(() -> "VoxelMap Entity Buffer", 4096);
        projection = CachedProjectionMatrixBuffer.orthographic("VoxelMap Entity Projection", 1000.0F, 21000.0F, true);

        final int fboTextureSize = 512;
        renderTarget = new VoxelMapRenderTarget("VoxelMap Entity Target", GpuFormat.RGBA8_UNORM, true);
        renderTarget.createBuffers(fboTextureSize, fboTextureSize);
    }

    public void setupMatrix(float baseScale, Properties iconConfig) {
        poseStack.setIdentity();
        poseStack.translate(256.0F, 256.0F, -3000.0F);
        poseStack.scale(64.0F, 64.0F, -64.0F);

        float scale = baseScale * Float.parseFloat(iconConfig.getProperty("scale", "1.0"));
        poseStack.scale(scale, scale, scale);

        String rotation = iconConfig.getProperty("rotation", "");
        parseDoubleArray(rotation, (key, value) -> {
            float f = value.floatValue();
            switch (key.toLowerCase(Locale.ROOT)) {
                case "x" -> poseStack.mulPose(Axis.XP.rotationDegrees(f));
                case "y" -> poseStack.mulPose(Axis.YP.rotationDegrees(f));
                case "z" -> poseStack.mulPose(Axis.ZP.rotationDegrees(f));
            }
        });

        String trim = iconConfig.getProperty("trim", "");
        int[] trimRect = new int[]{0, 0, 512, 512};
        parseDoubleArray(trim, (key, value) -> {
            int i = value.intValue();
            switch (key.toLowerCase(Locale.ROOT)) {
                case "x0" -> trimRect[0] = i;
                case "y0" -> trimRect[1] = i;
                case "x1" -> trimRect[2] = i;
                case "y1" -> trimRect[3] = i;
            }
        });
        setupScissorArea(scale, trimRect[0], trimRect[1], trimRect[2], trimRect[3]);
    }

    private void parseDoubleArray(String array, BiConsumer<String, Double> consumer) {
        if (array.startsWith("{") && array.endsWith("}")) {
            for (String entry : array.substring(1, array.length() - 1).split(",")) {
                String[] kv = entry.split(":", 2);
                if (kv.length < 2) continue;

                consumer.accept(kv[0].trim(), Double.parseDouble(kv[1].trim()));
            }
        }
    }

    private void setupScissorArea(float scale, int x0, int y0, int x1, int y1) {
        x0 = (int) ((x0 - 256) * scale) + 256;
        x1 = (int) ((x1 - 256) * scale) + 256;
        y0 = 256 - (int) ((y0 - 256) * scale);
        y1 = 256 - (int) ((y1 - 256) * scale);

        int x = Math.max(0, Math.min(511, x0));
        int y = Math.max(0, Math.min(511, y1));
        int w = Math.max(1, Math.min(512 - x, x1 - x0));
        int h = Math.max(1, Math.min(512 - y, y0 - y1));

        scissorState.enable(x, y, w, h);
    }

    public void beginBatch(RenderPipeline pipeline, VariantDataHolder variant) {
        if (isBatching) {
            throw new IllegalStateException("Cannot begin batch! Call endBatch() first.");
        }
        isBatching = true;

        this.pipeline = pipeline;
        this.variant = variant;
        draw = stagedVertexBuffer.appendDraw(pipeline.getVertexFormatBinding(0), pipeline.getPrimitiveTopology());
        vertexBuffer = stagedVertexBuffer.getVertexBuilder(draw);
    }

    public PoseStack matrix() {
        return poseStack;
    }

    public VertexConsumer vertexBuffer() {
        return vertexBuffer;
    }

    private GpuBufferSlice getUniforms(int color) {
        Vector4f colorModulator = new Vector4f(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color));
        Vector3f modelOffset = new Vector3f();
        Matrix4f textureMatrix = new Matrix4f();

        return RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), colorModulator, modelOffset, textureMatrix);
    }

    public void endBatch(Consumer<BufferedImage> resultConsumer) {
        if (!isBatching) {
            throw new IllegalStateException("Cannot end batch! Call beginBatch() first.");
        }
        isBatching = false;

        AbstractTexture texture0 = variant.tex0() == null ? null : minecraft.getTextureManager().getTexture(variant.tex0());
        AbstractTexture texture1 = variant.tex1() == null ? null : minecraft.getTextureManager().getTexture(variant.tex1());
        AbstractTexture texture2 = variant.tex2() == null ? null : minecraft.getTextureManager().getTexture(variant.tex2());
        AbstractTexture texture3 = variant.tex3() == null ? null : minecraft.getTextureManager().getTexture(variant.tex3());

        RenderUtils.setupProjectionMatrix(projection.getBuffer(512.0F, 512.0F), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.setShaderLights(lightingBuffer.slice());

        GpuBufferSlice uniforms0 = getUniforms(variant.col0());
        GpuBufferSlice uniforms1 = getUniforms(variant.col1());
        GpuBufferSlice uniforms2 = getUniforms(variant.col2());
        GpuBufferSlice uniforms3 = getUniforms(variant.col3());

        stagedVertexBuffer.upload();
        StagedVertexBuffer.ExecuteInfo meshInfo = stagedVertexBuffer.getExecuteInfo(draw);

        try (RenderPass renderPass = RenderUtils.createRenderPass("VoxelMap Entity Render", renderTarget, new Vector4f(0.0F, 0.0F, 0.0F, 0.0F), 0.0)) {
            if (meshInfo != null) {
                renderPass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
                renderPass.bindTexture("Sampler1", minecraft.gameRenderer.overlayTexture().getTextureView(), VoxelMapSamplers.LINEAR_CLAMP);
                renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightmap(), VoxelMapSamplers.LINEAR_CLAMP);
                renderPass.setVertexBuffer(0, meshInfo.vertexBuffer().slice());
                renderPass.setIndexBuffer(meshInfo.indexBuffer(), meshInfo.indexType());
                if (texture0 != null) {
                    renderPass.setUniform("DynamicTransforms", uniforms0);
                    renderPass.bindTexture("Sampler0", texture0.getTextureView(), texture0.getSampler());
                    renderPass.drawIndexed(meshInfo.indexCount(), 1, meshInfo.firstIndex(), meshInfo.baseVertex(), 0);
                }
                if (texture1 != null) {
                    renderPass.setUniform("DynamicTransforms", uniforms1);
                    renderPass.bindTexture("Sampler0", texture1.getTextureView(), texture1.getSampler());
                    renderPass.drawIndexed(meshInfo.indexCount(), 1, meshInfo.firstIndex(), meshInfo.baseVertex(), 0);
                }
                if (texture2 != null) {
                    renderPass.setUniform("DynamicTransforms", uniforms2);
                    renderPass.bindTexture("Sampler0", texture2.getTextureView(), texture2.getSampler());
                    renderPass.drawIndexed(meshInfo.indexCount(), 1, meshInfo.firstIndex(), meshInfo.baseVertex(), 0);
                }
                if (texture3 != null) {
                    renderPass.setUniform("DynamicTransforms", uniforms3);
                    renderPass.bindTexture("Sampler0", texture3.getTextureView(), texture3.getSampler());
                    renderPass.drawIndexed(meshInfo.indexCount(), 1, meshInfo.firstIndex(), meshInfo.baseVertex(), 0);
                }
            }
        }

        stagedVertexBuffer.endFrame();
        RenderUtils.flushCmds();
        RenderUtils.restoreProjectionMatrix();

        pipeline = null;
        variant = null;
        draw = null;
        vertexBuffer = null;

        RenderUtils.readTextureContentsToBufferedImage(renderTarget.getColorTexture(), (image) -> {
            resultConsumer.accept(RenderUtils.hasFlippedV() ? ImageUtils.flipVertical(image) : image);
        });
    }
}
