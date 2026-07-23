package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.rendering.CachedProjectionMatrixBuffer;
import com.mamiyaotaru.voxelmap.rendering.RenderUtils;
import com.mamiyaotaru.voxelmap.rendering.VoxelMapPipelines;
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
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public class EntityGPURenderer extends AbstractEntityRenderer {
    private final StagedVertexBuffer stagedVertexBuffer;
    private final GpuBuffer lightingBuffer;
    private final CachedProjectionMatrixBuffer projection;
    private final VoxelMapRenderTarget renderTarget;

    public EntityGPURenderer() {
        stagedVertexBuffer = minecraft.gameRenderer.renderBuffers().stagedVertexBuffer();

        Vector3f fullBright = new Vector3f(1.0F, -1.0F, 1.0F).normalize();
        Vector3f fullBright2 = new Vector3f(-1.0F, -1.0F, 1.0F).normalize();
        lightingBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap Lighting UBO", GpuBuffer.USAGE_UNIFORM + GpuBuffer.USAGE_COPY_DST, Lighting.UBO_SIZE);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, Lighting.UBO_SIZE).putVec3(fullBright).putVec3(fullBright2).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(lightingBuffer.slice(), byteBuffer);
        }

        projection = CachedProjectionMatrixBuffer.orthographic("VoxelMap Entity Projection", 1000.0F, 21000.0F, true);
        renderTarget = new VoxelMapRenderTarget("VoxelMap Entity Target", GpuFormat.RGBA8_UNORM, true);
        renderTarget.createBuffers(TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    protected void setupMatrix() {
        poseStack.translate(256.0F, 256.0F, -3000.0F);
        poseStack.scale(64.0F, 64.0F, -64.0F);
    }

    @Override
    public void render(TextureSet textureSet, Consumer<BufferedImage> resultConsumer) {
        RenderPipeline pipeline = cullEnabled ? VoxelMapPipelines.ENTITY_ICON_CULLED : VoxelMapPipelines.ENTITY_ICON;

        AbstractTexture primaryTexture = textureSet.primaryTexture() == null ? null : minecraft.getTextureManager().getTexture(textureSet.primaryTexture());
        AbstractTexture secondaryTexture = textureSet.secondaryTexture() == null ? null : minecraft.getTextureManager().getTexture(textureSet.secondaryTexture());
        AbstractTexture tertiaryTexture = textureSet.tertiaryTexture() == null ? null : minecraft.getTextureManager().getTexture(textureSet.tertiaryTexture());
        AbstractTexture quaternaryTexture = textureSet.quaternaryTexture() == null ? null : minecraft.getTextureManager().getTexture(textureSet.quaternaryTexture());

        RenderUtils.setupProjectionMatrix(projection.getBuffer(512.0F, 512.0F), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.setShaderLights(lightingBuffer.slice());

        GpuBufferSlice primaryTransforms = dynamicTransformsWithColor(textureSet.primaryColor());
        GpuBufferSlice secondaryTransforms = dynamicTransformsWithColor(textureSet.secondaryColor());
        GpuBufferSlice tertiaryTransforms = dynamicTransformsWithColor(textureSet.tertiaryColor());
        GpuBufferSlice quaternaryTransforms = dynamicTransformsWithColor(textureSet.quaternaryColor());

        StagedVertexBuffer.Draw draw = stagedVertexBuffer.appendDraw(pipeline.getVertexFormatBinding(0), pipeline.getPrimitiveTopology());
        VertexConsumer buffer = stagedVertexBuffer.getVertexBuilder(draw);

        for (ModelPart modelPart : modelParts) {
            modelPart.render(poseStack, buffer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }

        for (BlockModelSet blockModel : blockModels) {
            for (BlockStateModelPart modelPart : blockModel.modelParts()) {
                drawBlockModelPart(modelPart, poseStack, buffer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            }
        }

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
                if (primaryTexture != null) {
                    renderPass.setUniform("DynamicTransforms", primaryTransforms);
                    renderPass.bindTexture("Sampler0", primaryTexture.getTextureView(), primaryTexture.getSampler());
                    renderPass.drawIndexed(meshInfo.indexCount(), 1, meshInfo.firstIndex(), meshInfo.baseVertex(), 0);
                }
                if (secondaryTexture != null) {
                    renderPass.setUniform("DynamicTransforms", secondaryTransforms);
                    renderPass.bindTexture("Sampler0", secondaryTexture.getTextureView(), secondaryTexture.getSampler());
                    renderPass.drawIndexed(meshInfo.indexCount(), 1, meshInfo.firstIndex(), meshInfo.baseVertex(), 0);
                }
                if (tertiaryTexture != null) {
                    renderPass.setUniform("DynamicTransforms", tertiaryTransforms);
                    renderPass.bindTexture("Sampler0", tertiaryTexture.getTextureView(), tertiaryTexture.getSampler());
                    renderPass.drawIndexed(meshInfo.indexCount(), 1, meshInfo.firstIndex(), meshInfo.baseVertex(), 0);
                }
                if (quaternaryTexture != null) {
                    renderPass.setUniform("DynamicTransforms", quaternaryTransforms);
                    renderPass.bindTexture("Sampler0", quaternaryTexture.getTextureView(), quaternaryTexture.getSampler());
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
