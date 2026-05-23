package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.render.RenderUtils;
import com.mamiyaotaru.voxelmap.render.VoxelMapRenderTarget;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Properties;

public class EntityImageRenderer {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final Tesselator tesselator = Tesselator.getInstance();
    private final PoseStack poseStack = new PoseStack();
    private final CachedOrthoProjectionMatrixBuffer projection;
    private final GpuBuffer lightingBuffer;
    private final VoxelMapRenderTarget renderTarget;
    private VariantDataHolder dataHolder;
    private RenderPipeline pipeline;
    private BufferBuilder bufferBuilder;

    public EntityImageRenderer() {
        projection = new CachedOrthoProjectionMatrixBuffer("VoxelMap Entity Map Image Proj", 1000.0F, 21000.0F, true);

        Vector3f fullBright = new Vector3f(1.0F, -1.0F, 1.0F).normalize();
        Vector3f fullBright2 = new Vector3f(-1.0F, -1.0F, 1.0F).normalize();
        lightingBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap Lighting UBO", GpuBuffer.USAGE_UNIFORM + GpuBuffer.USAGE_COPY_DST, Lighting.UBO_SIZE);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, Lighting.UBO_SIZE).putVec3(fullBright).putVec3(fullBright2).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(lightingBuffer.slice(), byteBuffer);
        }

        final int fboTextureSize = 512;
        renderTarget = new VoxelMapRenderTarget("VoxelMap Entity Map Image Target", true);
        renderTarget.createBuffers(fboTextureSize, fboTextureSize);
    }

    public void setup(float baseScale, Properties iconConfig) {
        poseStack.setIdentity();
        poseStack.translate(256.0F, 256.0F, -3000.0F);

        // Apply Scale
        float scale = 64.0F * baseScale * Float.parseFloat(iconConfig.getProperty("scale", "1.0"));
        poseStack.scale(scale, scale, -scale);

        // Apply Rotation
        String rotation = iconConfig.getProperty("rotation", "");
        if (rotation.startsWith("{") && rotation.endsWith("}")) {
            for (String entry : rotation.substring(1, rotation.length() - 1).split(",")) {
                String[] keyValue = entry.split(":", 2);
                float value = Float.parseFloat(keyValue[1]);
                switch (keyValue[0].trim().toLowerCase()) {
                    case "x" -> poseStack.mulPose(Axis.XP.rotationDegrees(value));
                    case "y" -> poseStack.mulPose(Axis.YP.rotationDegrees(value));
                    case "z" -> poseStack.mulPose(Axis.ZP.rotationDegrees(value));
                }
            }
        }
    }

    public PoseStack pose() {
        return poseStack;
    }

    public void beginBatch(RenderPipeline pipeline, VariantDataHolder dataHolder) {
        this.dataHolder = dataHolder;
        this.pipeline = pipeline;
        bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, pipeline.getVertexFormat());
    }

    public VertexConsumer vertexBuffer() {
        return bufferBuilder;
    }

    public BufferedImage endBatch() {
        AbstractTexture primaryTexture = dataHolder.getTexture0() != null ? minecraft.getTextureManager().getTexture(dataHolder.getTexture0()) : null;
        AbstractTexture secondaryTexture = dataHolder.getTexture1() != null ? minecraft.getTextureManager().getTexture(dataHolder.getTexture1()) : null;
        AbstractTexture tertiaryTexture = dataHolder.getTexture2() != null ? minecraft.getTextureManager().getTexture(dataHolder.getTexture2()) : null;
        AbstractTexture quaternaryTexture = dataHolder.getTexture3() != null ? minecraft.getTextureManager().getTexture(dataHolder.getTexture3()): null;

        ProjectionType originalProjectionType = RenderSystem.getProjectionType();
        GpuBufferSlice originalProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
        RenderSystem.setProjectionMatrix(projection.getBuffer(512.0F, 512.0F), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.setShaderLights(lightingBuffer.slice());
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();

        GpuBufferSlice primaryTransforms = dynamicTransformsWithColor(dataHolder.getColor0());
        GpuBufferSlice secondaryTransforms = dynamicTransformsWithColor(dataHolder.getColor1());
        GpuBufferSlice tertiaryTransforms = dynamicTransformsWithColor(dataHolder.getColor2());
        GpuBufferSlice quaternaryTransforms = dynamicTransformsWithColor(dataHolder.getColor3());

        try (MeshData meshData = bufferBuilder.build()) {
            // no mesh? might happen with some mods
            if (meshData == null) {
                return null;
            }

            GpuBuffer vertexBuffer = RenderUtils.uploadImmediateVertexBuffer(pipeline.getVertexFormat(), meshData.vertexBuffer());
            GpuBuffer indexBuffer;
            VertexFormat.IndexType indexType;
            if (meshData.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
                indexBuffer = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
                indexType = autoStorageIndexBuffer.type();
            } else {
                indexBuffer = RenderUtils.uploadImmediateIndexBuffer(pipeline.getVertexFormat(), meshData.indexBuffer());
                indexType = meshData.drawState().indexType();
            }

            if (VoxelConstants.hasVulkanMod()) {
                renderTarget.destroyBuffers();
                renderTarget.createBuffers(renderTarget.width, renderTarget.height);
            }

            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "VoxelMap entity image renderer", renderTarget.getColorTextureView(), OptionalInt.of(0x00000000), renderTarget.getDepthTextureView(), OptionalDouble.of(1.0))) {
                renderPass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.bindTexture("Sampler1", minecraft.gameRenderer.overlayTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indexBuffer, indexType);
                if (primaryTexture != null) {
                    renderPass.setUniform("DynamicTransforms", primaryTransforms);
                    renderPass.bindTexture("Sampler0", primaryTexture.getTextureView(), primaryTexture.getSampler());
                    renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
                }
                if (secondaryTexture != null) {
                    renderPass.setUniform("DynamicTransforms", secondaryTransforms);
                    renderPass.bindTexture("Sampler0", secondaryTexture.getTextureView(), secondaryTexture.getSampler());
                    renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
                }
                if (tertiaryTexture != null) {
                    renderPass.setUniform("DynamicTransforms", tertiaryTransforms);
                    renderPass.bindTexture("Sampler0", tertiaryTexture.getTextureView(), tertiaryTexture.getSampler());
                    renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
                }
                if (quaternaryTexture != null) {
                    renderPass.setUniform("DynamicTransforms", quaternaryTransforms);
                    renderPass.bindTexture("Sampler0", quaternaryTexture.getTextureView(), quaternaryTexture.getSampler());
                    renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
                }
            }
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Entity draw failed! ", e);
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(originalProjectionMatrix, originalProjectionType);

            tesselator.clear();
        }

        RenderUtils.fenceAndWait();

        BufferedImage output = RenderUtils.readTextureContentsToBufferedImage(renderTarget.getColorTexture());
        return RenderUtils.hasFlippedTexture() ? ImageUtils.flipVertical(output) : output;
    }

    private GpuBufferSlice dynamicTransformsWithColor(int color) {
        Vector4f colorModulator = new Vector4f(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color));
        Vector3f modelOffset = new Vector3f();
        Matrix4f textureMatrix = new Matrix4f();

        return RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrix(), colorModulator, modelOffset, textureMatrix);
    }
}
