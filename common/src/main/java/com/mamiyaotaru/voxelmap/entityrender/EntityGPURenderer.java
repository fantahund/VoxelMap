package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapCachedOrthoProjectionMatrixBuffer;
import com.mamiyaotaru.voxelmap.util.VoxelMapPipelines;
import com.mamiyaotaru.voxelmap.util.VoxelMapRenderTarget;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

public class EntityGPURenderer extends AbstractEntityRenderer {
    private final GpuBuffer lightingBuffer;
    private final VoxelMapCachedOrthoProjectionMatrixBuffer projection;
    private final Tesselator tessellator = new Tesselator(4096);
    private final VoxelMapRenderTarget renderTarget;

    public EntityGPURenderer() {
        Vector3f fullBright = new Vector3f(1.0F, -1.0F, 1.0F).normalize();
        Vector3f fullBright2 = new Vector3f(-1.0F, -1.0F, 1.0F).normalize();
        lightingBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap Lighting UBO", GpuBuffer.USAGE_UNIFORM + GpuBuffer.USAGE_COPY_DST, Lighting.UBO_SIZE);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, Lighting.UBO_SIZE).putVec3(fullBright).putVec3(fullBright2).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(lightingBuffer.slice(), byteBuffer);
        }

        projection = new VoxelMapCachedOrthoProjectionMatrixBuffer("VoxelMap Entity Map Image Proj", 256.0F, -256.0F, -256.0F, 256.0F, 1000.0F, 21000.0F);
        renderTarget = new VoxelMapRenderTarget(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "render_target/voxelmap_radar"));
        renderTarget.createBuffers(TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    protected void setupMatrix() {
        poseStack.translate(0.0F, 0.0F, -3000.0F);
        poseStack.scale(64.0F, 64.0F, -64.0F);
    }

    @Override
    public void render(TextureSet textureSet, Consumer<BufferedImage> resultConsumer) {
        RenderPipeline renderPipeline = cullEnabled ? VoxelMapPipelines.ENTITY_ICON_CULLED : VoxelMapPipelines.ENTITY_ICON;
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.QUADS, renderPipeline.getVertexFormat());

        for (ModelPart modelPart : modelParts) {
            modelPart.render(poseStack, bufferBuilder, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }

        for (BlockModelSet blockModel : blockModels) {
            for (BlockStateModelPart modelPart : blockModel.modelParts()) {
                drawBlockModelPart(modelPart, poseStack, bufferBuilder, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            }
        }

        AbstractTexture primaryTexture = textureSet.primaryTexture() == null ? null : minecraft.getTextureManager().getTexture(textureSet.primaryTexture());
        AbstractTexture secondaryTexture = textureSet.secondaryTexture() == null ? null : minecraft.getTextureManager().getTexture(textureSet.secondaryTexture());
        AbstractTexture tertiaryTexture = textureSet.tertiaryTexture() == null ? null : minecraft.getTextureManager().getTexture(textureSet.tertiaryTexture());
        AbstractTexture quaternaryTexture =  textureSet.quaternaryTexture() == null ? null : minecraft.getTextureManager().getTexture(textureSet.quaternaryTexture());

        ProjectionType originalProjectionType = RenderSystem.getProjectionType();
        GpuBufferSlice originalProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
        RenderSystem.setProjectionMatrix(projection.getBuffer(), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.setShaderLights(lightingBuffer.slice());
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();

        GpuBufferSlice primaryTransforms = dynamicTransformsWithColor(textureSet.primaryColor());
        GpuBufferSlice secondaryTransforms = dynamicTransformsWithColor(textureSet.secondaryColor());
        GpuBufferSlice tertiaryTransforms = dynamicTransformsWithColor(textureSet.tertiaryColor());
        GpuBufferSlice quaternaryTransforms = dynamicTransformsWithColor(textureSet.quaternaryColor());

        try (MeshData meshData = bufferBuilder.build()) {
            // no mesh? might happen with some mods
            if (meshData == null) return;

            GpuBuffer vertexBuffer = renderPipeline.getVertexFormat().uploadImmediateVertexBuffer(meshData.vertexBuffer());
            GpuBuffer indexBuffer;
            VertexFormat.IndexType indexType;
            if (meshData.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
                indexBuffer = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
                indexType = autoStorageIndexBuffer.type();
            } else {
                indexBuffer = renderPipeline.getVertexFormat().uploadImmediateIndexBuffer(meshData.indexBuffer());
                indexType = meshData.drawState().indexType();
            }

            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "VoxelMap entity image renderer", renderTarget.getColorTextureView(), OptionalInt.of(0x00000000), renderTarget.getDepthTextureView(), OptionalDouble.of(1.0))) {
                renderPass.setPipeline(renderPipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.bindTexture("Sampler1", minecraft.gameRenderer.overlayTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
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
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(originalProjectionMatrix, originalProjectionType);

            tessellator.clear();
        }

        GLUtils.readTextureContentsToBufferedImage(renderTarget.getColorTexture(), (output) -> {
            resultConsumer.accept(ImageUtils.flipHorizontal(output));
        });
    }

    private void drawBlockModelPart(BlockStateModelPart modelPart, PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        QuadInstance quadData = new QuadInstance();
        quadData.setLightCoords(light);
        quadData.setOverlayCoords(overlay);
        quadData.setColor(color);
        for (Direction direction : ALL_DIRECTIONS) {
            for (BakedQuad quad : modelPart.getQuads(direction)) {
                vertexConsumer.putBakedQuad(poseStack.last(), quad, quadData);
            }
        }
    }

    private GpuBufferSlice dynamicTransformsWithColor(int color) {
        Vector4f colorModulator = new Vector4f(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color));
        Vector3f modelOffset = new Vector3f();
        Matrix4f textureMatrix = new Matrix4f();

        return RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrix(), colorModulator, modelOffset, textureMatrix);
    }
}
