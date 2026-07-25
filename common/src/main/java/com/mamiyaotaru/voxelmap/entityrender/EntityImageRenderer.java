package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.rendering.CachedProjectionMatrixBuffer;
import com.mamiyaotaru.voxelmap.rendering.RenderUtils;
import com.mamiyaotaru.voxelmap.rendering.VoxelMapRenderTarget;
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
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public class EntityImageRenderer {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final PoseStack poseStack = new PoseStack();
    private final GpuBuffer lightingBuffer;
    private final StagedVertexBuffer stagedVertexBuffer;
    private final CachedProjectionMatrixBuffer projection;
    private final VoxelMapRenderTarget renderTarget;

    private boolean isBatching = false;
    private RenderPipeline pipeline;
    private TextureSet textureSet;
    private StagedVertexBuffer.Draw draw;
    private VertexConsumer vertexBuffer;

    public EntityImageRenderer() {
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

    public void setupMatrix(Properties iconConfig) {
        poseStack.setIdentity();

        poseStack.translate(256.0F, 256.0F, -3000.0F);
        poseStack.scale(64.0F, 64.0F, -64.0F);

        String rotation = iconConfig.getProperty("rotation", "");
        if (rotation.startsWith("{") && rotation.endsWith("}")) {
            for (String entry : rotation.substring(1, rotation.length() - 1).split(",")) {
                String[] kv = entry.split(":", 2);
                if (kv.length < 2) continue;

                float value = Float.parseFloat(kv[1].trim());
                switch (kv[0].trim().toLowerCase()) {
                    case "x" -> poseStack.mulPose(Axis.XP.rotationDegrees(value));
                    case "y" -> poseStack.mulPose(Axis.YP.rotationDegrees(value));
                    case "z" -> poseStack.mulPose(Axis.ZP.rotationDegrees(value));
                }
            }
        }
    }

    public void beginBatch(RenderPipeline pipeline, TextureSet textureSet) {
        if (isBatching) {
            throw new IllegalStateException("Cannot begin batch! Call endBatch() first.");
        }
        isBatching = true;

        this.pipeline = pipeline;
        this.textureSet = textureSet;
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

        AbstractTexture texture0 = textureSet.tex0() == null ? null : minecraft.getTextureManager().getTexture(textureSet.tex0());
        AbstractTexture texture1 = textureSet.tex1() == null ? null : minecraft.getTextureManager().getTexture(textureSet.tex1());
        AbstractTexture texture2 = textureSet.tex2() == null ? null : minecraft.getTextureManager().getTexture(textureSet.tex2());
        AbstractTexture texture3 = textureSet.tex3() == null ? null : minecraft.getTextureManager().getTexture(textureSet.tex3());

        RenderUtils.setupProjectionMatrix(projection.getBuffer(512.0F, 512.0F), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.setShaderLights(lightingBuffer.slice());

        GpuBufferSlice uniforms0 = getUniforms(textureSet.col0());
        GpuBufferSlice uniforms1 = getUniforms(textureSet.col1());
        GpuBufferSlice uniforms2 = getUniforms(textureSet.col2());
        GpuBufferSlice uniforms3 = getUniforms(textureSet.col3());

        stagedVertexBuffer.upload();
        StagedVertexBuffer.ExecuteInfo meshInfo = stagedVertexBuffer.getExecuteInfo(draw);

        if (meshInfo != null) {
            try (RenderPass renderPass = RenderUtils.createRenderPass("VoxelMap Entity Render", renderTarget, new Vector4f(0.0F, 0.0F, 0.0F, 0.0F), 0.0)) {
                renderPass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.bindTexture("Sampler1", minecraft.gameRenderer.overlayTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
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

        RenderUtils.readTextureContentsToBufferedImage(renderTarget.getColorTexture(), (image) -> {
            resultConsumer.accept(RenderUtils.hasFlippedV() ? ImageUtils.flipVertical(image) : image);
        });
    }

    public record TextureSet(Identifier tex0, int col0, Identifier tex1, int col1, Identifier tex2, int col2, Identifier tex3, int col3) {
    }
}
