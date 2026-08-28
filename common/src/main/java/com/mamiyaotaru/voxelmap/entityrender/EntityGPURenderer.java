package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.rendering.GLUtils;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.rendering.VoxelMapCachedOrthoProjectionMatrixBuffer;
import com.mamiyaotaru.voxelmap.rendering.VoxelMapPipelines;
import com.mamiyaotaru.voxelmap.rendering.VoxelMapRenderTarget;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public class EntityGPURenderer extends AbstractEntityRenderer {
    private final GpuBuffer lightingBuffer;
    private final VoxelMapCachedOrthoProjectionMatrixBuffer projection;
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
        boolean renderedMesh = false;

        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(4096)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, renderPipeline.getVertexFormatBinding(0));

            for (ModelPartRenderTask modelPart : modelParts) {
                visitModelPart(modelPart, (pose, path, cubeIndex, cube) ->
                        cube.compile(pose, bufferBuilder, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF));
            }

            for (BlockModelSet blockModel : blockModels) {
                for (BlockStateModelPart modelPart : blockModel.modelParts()) {
                    drawBlockModelPart(modelPart, poseStack, bufferBuilder, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                }
            }

            try (MeshData meshData = bufferBuilder.build()) {
                if (meshData != null) {
                    renderedMesh = true;
                    drawMesh(meshData, renderPipeline,
                            primaryTexture, primaryTransforms, secondaryTexture, secondaryTransforms,
                            tertiaryTexture, tertiaryTransforms, quaternaryTexture, quaternaryTransforms);
                }
            }
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(originalProjectionMatrix, originalProjectionType);
        }

        if (!renderedMesh) {
            resultConsumer.accept(new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_4BYTE_ABGR));
            return;
        }

        GLUtils.readTextureContentsToBufferedImage(renderTarget.getColorTexture(), (output) -> {
            resultConsumer.accept(ImageUtils.flipHorizontal(output));
        });
    }

    private void drawMesh(MeshData meshData, RenderPipeline renderPipeline,
                          AbstractTexture primaryTexture, GpuBufferSlice primaryTransforms,
                          AbstractTexture secondaryTexture, GpuBufferSlice secondaryTransforms,
                          AbstractTexture tertiaryTexture, GpuBufferSlice tertiaryTransforms,
                          AbstractTexture quaternaryTexture, GpuBufferSlice quaternaryTransforms) {
        GpuBuffer vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap Entity Icon Vertex Buffer", GpuBuffer.USAGE_VERTEX, meshData.vertexBuffer());
        GpuBuffer indexBuffer;
        boolean closeIndexBuffer = false;
        IndexType indexType;
        if (meshData.indexBuffer() == null) {
            RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().primitiveTopology());
            indexBuffer = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
            indexType = autoStorageIndexBuffer.type();
        } else {
            indexBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap Entity Icon Index Buffer", GpuBuffer.USAGE_INDEX, meshData.indexBuffer());
            indexType = meshData.drawState().indexType();
            closeIndexBuffer = true;
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.clearColorAndDepthTextures(renderTarget.getColorTexture(), new Vector4f(0.0F, 0.0F, 0.0F, 0.0F), renderTarget.getDepthTexture(), 0.0);
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "VoxelMap entity image renderer", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(renderPipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("Sampler1", minecraft.gameRenderer.overlayTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.setVertexBuffer(0, vertexBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, indexType);
            drawTexture(renderPass, meshData, primaryTexture, primaryTransforms);
            drawTexture(renderPass, meshData, secondaryTexture, secondaryTransforms);
            drawTexture(renderPass, meshData, tertiaryTexture, tertiaryTransforms);
            drawTexture(renderPass, meshData, quaternaryTexture, quaternaryTransforms);
        } finally {
            vertexBuffer.close();
            if (closeIndexBuffer) {
                indexBuffer.close();
            }
        }
    }

    private void drawTexture(RenderPass renderPass, MeshData meshData, AbstractTexture texture, GpuBufferSlice transforms) {
        if (texture != null) {
            renderPass.setUniform("DynamicTransforms", transforms);
            renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
            renderPass.drawIndexed(meshData.drawState().indexCount(), 1, 0, 0, 0);
        }
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

        return RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), colorModulator, modelOffset, textureMatrix);
    }
}
