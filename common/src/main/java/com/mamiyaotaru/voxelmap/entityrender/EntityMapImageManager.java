package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantData;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.HorseVariantDataFactory;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.AllocatedTexture;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapCachedOrthoProjectionMatrixBuffer;
import com.mamiyaotaru.voxelmap.util.VoxelMapPipelines;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.AbstractEquineModel;
import net.minecraft.client.model.CamelModel;
import net.minecraft.client.model.CodModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.LavaSlimeModel;
import net.minecraft.client.model.LlamaModel;
import net.minecraft.client.model.SalmonModel;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.TropicalFishModelA;
import net.minecraft.client.model.TropicalFishModelB;
import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.WitchModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.CodRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.client.renderer.entity.HoglinRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.SalmonRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.TropicalFishRenderer;
import net.minecraft.client.renderer.entity.ZoglinRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class EntityMapImageManager {
    public static final ResourceLocation resourceTextureAtlasMarker = ResourceLocation.fromNamespaceAndPath("voxelmap", "atlas/mobs");
    private final TextureAtlas textureAtlas;
    private final Minecraft minecraft = Minecraft.getInstance();
    private GpuTexture fboDepthTexture;
    private GpuTexture fboTexture;
    private final ResourceLocation resourceFboTexture = ResourceLocation.fromNamespaceAndPath("voxelmap", "entityimagemanager/fbo");
    private Tesselator fboTessellator = new Tesselator(4096);
    private int imageCreationRequests;
    private int fulfilledImageCreationRequests;
    private final HashMap<EntityType<?>, EntityVariantDataFactory> variantDataFactories = new HashMap<>();
    private ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final Camera fakeCamera = new Camera();
    private GpuTextureView fboTextureView;
    private GpuTextureView fboDepthTextureView;
    private VoxelMapCachedOrthoProjectionMatrixBuffer projection;

    public EntityMapImageManager() {
        this.textureAtlas = new TextureAtlas("mobsmap", resourceTextureAtlasMarker);
        this.textureAtlas.setFilter(true, false);
        this.textureAtlas.reset();
        this.textureAtlas.registerIconForBufferedImage("neutral", ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/neutral.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.stitch();
        final int fboTextureSize = 512;
        this.fboTexture = RenderSystem.getDevice().createTexture("voxelmap-radarfbotexture", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, fboTextureSize, fboTextureSize, 1, 1);
        this.fboDepthTexture = RenderSystem.getDevice().createTexture("voxelmap-radarfbodepth", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.DEPTH32, fboTextureSize, fboTextureSize, 1, 1);
        Minecraft.getInstance().getTextureManager().register(resourceFboTexture, new AllocatedTexture(fboTexture));

        // this.fboTexture = fboTexture.getTexture();
        fboTextureView = RenderSystem.getDevice().createTextureView(this.fboTexture);
        fboDepthTextureView = RenderSystem.getDevice().createTextureView(this.fboDepthTexture);

        projection = new VoxelMapCachedOrthoProjectionMatrixBuffer("VoxelMap Entity Map Image Proj", 256.0F, -256.0F, -256.0F, 256.0F, 1000.0F, 21000.0F);

    }

    public void reset() {
        if (VoxelConstants.DEBUG) {
            VoxelConstants.getLogger().info("EntityMapImageManager: Resetting");
        }
        variantDataFactories.clear();
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.BOGGED, ResourceLocation.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png")));
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.ENDERMAN, ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png")));
        // addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.TROPICAL_FISH, ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png")));
        addVariantDataFactory(new HorseVariantDataFactory(EntityType.HORSE));
        if (VoxelConstants.DEBUG) {
            BuiltInRegistries.ENTITY_TYPE.forEach(t -> {
                requestImageForMobType(t, 32, true);
            });
        }
    }

    private void addVariantDataFactory(EntityVariantDataFactory factory) {
        variantDataFactories.put(factory.getType(), factory);
    }

    public Sprite requestImageForMobType(EntityType<?> type) {
        return requestImageForMobType(type, -1, true);
    }

    public Sprite requestImageForMobType(EntityType<?> type, int size, boolean addBorder) {
        if (minecraft.level != null && type.create(minecraft.level, EntitySpawnReason.LOAD) instanceof LivingEntity le) {
            return requestImageForMob(le, size, addBorder);
        }
        return null;
    }

    public Sprite requestImageForMob(LivingEntity e) {
        return requestImageForMob(e, -1, true);
    }

    private EntityVariantData getVariantData(Entity entity, @SuppressWarnings("rawtypes") EntityRenderer renderer, EntityRenderState state, int size, boolean addBorder) {
        EntityVariantDataFactory factory = variantDataFactories.get(entity.getType());
        if (factory != null) {
            EntityVariantData data = factory.createVariantData(entity, renderer, state, size, addBorder);
            if (data != null) {
                return data;
            }
        }
        return DefaultEntityVariantDataFactory.createSimpleVariantData(entity, renderer, state, size, addBorder);
    }

    @SuppressWarnings("unchecked")
    public Sprite requestImageForMob(Entity entity, int size, boolean addBorder) {
        EntityRenderer<?, ?> baseRenderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        EntityVariantData variant = null;
        EntityRenderState renderState = null;
        if (entity instanceof AbstractClientPlayer player) {
            variant = new DefaultEntityVariantData(entity.getType(), player.getSkin().texture(), null, size, addBorder);
        } else if (entity instanceof LivingEntity && baseRenderer instanceof LivingEntityRenderer renderer) {
            if (minecraft.getEntityRenderDispatcher().camera == null) {
                minecraft.getEntityRenderDispatcher().camera = fakeCamera;
            }

            renderState = renderer.createRenderState(entity, 0.5f);

            if (minecraft.getEntityRenderDispatcher().camera == fakeCamera) {
                minecraft.getEntityRenderDispatcher().camera = null;
            }

            variant = getVariantData(entity, renderer, renderState, size, addBorder);
        }
        if (variant == null) {
            return null;
        }
        Sprite existing = textureAtlas.getAtlasSpriteIncludingYetToBeStitched(variant);
        if (existing != null && existing != textureAtlas.getMissingImage()) {
            // VoxelConstants.getLogger().info("EntityMapImageManager: Existing type " + entity.getType().getDescriptionId());
            return existing;
        }
        if (VoxelConstants.DEBUG) {
            VoxelConstants.getLogger().info("EntityMapImageManager: Rendering Mob of type " + entity.getType().getDescriptionId());
        }

        Sprite sprite = textureAtlas.registerEmptyIcon(variant);
        if (entity instanceof AbstractClientPlayer player) {
            BufferedImage playerImage = getPlayerIcon(player, size, addBorder);
            postProcessRenderedMobImage(entity, sprite, null, playerImage);
            return sprite;
        }

        ResourceLocation resourceLocation = variant.getPrimaryTexture();
        ResourceLocation resourceLocation2 = variant.getSecondaryTexture();

        // VoxelConstants.getLogger().info(" -> " + resourceLocation);
        RenderPipeline renderPipeline = VoxelMapPipelines.ENTITY_ICON_PIPELINE;
        BufferBuilder bufferBuilder = fboTessellator.begin(Mode.QUADS, renderPipeline.getVertexFormat());

        PoseStack pose = new PoseStack();

        pose.pushPose();
        float scale = 64;
        pose.translate(0.0f, 0.0f, -3000.0f);
        pose.scale(scale, scale, -scale);
        // pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        // pose.mulPose(Axis.YP.rotationDegrees(180.0F));

        // if (facing == Direction.EAST) {
        // pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        // } else if (facing == Direction.UP) {
        // pose.mulPose(Axis.XP.rotationDegrees(90.0F));

        if (baseRenderer instanceof AbstractHorseRenderer<?, ?, ?>) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.XP.rotationDegrees(35.0F));
        }
        if (baseRenderer instanceof SalmonRenderer || baseRenderer instanceof CodRenderer || baseRenderer instanceof TropicalFishRenderer) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        }
        if (baseRenderer instanceof ParrotRenderer) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        }
        if (baseRenderer instanceof HoglinRenderer || baseRenderer instanceof ZoglinRenderer) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        }
        if (baseRenderer instanceof GoatRenderer) {
            pose.mulPose(Axis.XP.rotationDegrees(-30.0F));
        }

        @SuppressWarnings("rawtypes")
        EntityModel model = ((LivingEntityRenderer) baseRenderer).getModel();
        model.setupAnim((LivingEntityRenderState) renderState);
        for (ModelPart part : getPartToRender(model)) {
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            part.render(pose, bufferBuilder, 15, 0, 0xffffffff); // light, overlay, color //TODO set model tint
        }
        if (baseRenderer instanceof SlimeRenderer slimeRenderer) {
            SlimeOuterLayer slimeOuter = (SlimeOuterLayer) slimeRenderer.layers.get(0);
            slimeOuter.model.setupAnim(renderState);
            slimeOuter.model.root().render(pose, bufferBuilder, 15, 0, 0xffffffff); // light, overlay, color
        }

        AbstractTexture texture = minecraft.getTextureManager().getTexture(resourceLocation);
        AbstractTexture texture2 = resourceLocation2 == null ? null : minecraft.getTextureManager().getTexture(resourceLocation2);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
                .writeTransform(
                        RenderSystem.getModelViewMatrix(),
                        new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                        new Vector3f(),
                        RenderSystem.getTextureMatrix(),
                        RenderSystem.getShaderLineWidth());

        try (MeshData meshData = bufferBuilder.build()) {
            // no mesh? might happen with some mods
            if (meshData == null) {
                return sprite;
            }
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

            // float size = 64.0F * scale;
            // int width = fboTexture.getWidth(0);
            // int height = fboTexture.getHeight(0);
            ProjectionType originalProjectionType = RenderSystem.getProjectionType();
            GpuBufferSlice originalProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
            RenderSystem.setProjectionMatrix(projection.getBuffer(), ProjectionType.ORTHOGRAPHIC);

            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "VoxelMap entity image renderer", fboTextureView, OptionalInt.of(0x00000000), fboDepthTextureView, OptionalDouble.of(1.0))) {
                renderPass.setPipeline(renderPipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                renderPass.bindSampler("Sampler0", texture.getTextureView());
                // renderPass.bindSampler("Sampler1", texture.getTexture()); // overlay
                // minecraft.gameRenderer.overlayTexture().setupOverlayColor();
                // renderPass.bindSampler("Sampler2", texture.getTexture()); // lightmap
                // minecraft.gameRenderer.lightTexture().turnOnLightLayer();
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indexBuffer, indexType);
                renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);

                if (texture2 != null) {
                    renderPass.bindSampler("Sampler0", texture2.getTextureView());
                    renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
                }
            }
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(originalProjectionMatrix, originalProjectionType);

        }
        // if (VoxelConstants.DEBUG) {
        // ImageUtils.saveImage("mob_" + entity.getType().getDescriptionId(), fboTexture, 0, fboTexture.getWidth(0), fboTexture.getHeight(0));
        // }
        imageCreationRequests++;
        GLUtils.readTextureContentsToBufferedImage(fboTexture, image2 -> {
            postProcessRenderedMobImage(entity, sprite, model, image2);
        });

        return sprite;
    }

    private void postProcessRenderedMobImage(Entity entity, Sprite sprite, @SuppressWarnings("rawtypes") EntityModel model, BufferedImage image2) {
        Util.backgroundExecutor().execute(() -> {
            BufferedImage image = image2;
            image = ImageUtils.flipHorizontal(image);
            // try {
            // ImageIO.write(image, "png", new File(entity.getType().getDescriptionId() + ".png"));
            // } catch (IOException e) {
            // e.printStackTrace();
            // }
            if (model instanceof CamelModel) {
                Graphics2D g = image.createGraphics();
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 192, image.getWidth(), image.getHeight());
            }
            if (model instanceof LlamaModel) {
                Graphics2D g = image.createGraphics();
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 248, image.getWidth(), image.getHeight());
            }
            image = ImageUtils.trim(image);
            int maxSize = Math.max(image.getHeight(), image.getWidth());
            int targetSize = model instanceof AbstractEquineModel<?> || model instanceof WitchModel || model instanceof WardenModel ? 50 : 32;
            if (maxSize > 0 && maxSize != targetSize) {
                image = ImageUtils.scaleImage(image, (float) targetSize / maxSize);
            }
            image = ImageUtils.fillOutline(ImageUtils.pad(image), true, 2);

            BufferedImage image3 = image;
            taskQueue.add(() -> {
                fulfilledImageCreationRequests++;

                sprite.setTextureData(ImageUtils.nativeImageFromBufferedImage(image3));
                if (VoxelConstants.DEBUG) {
                    VoxelConstants.getLogger().info("EntityMapImageManager: Buffered Image (" + fulfilledImageCreationRequests + "/" + imageCreationRequests + ") added to texture atlas " + entity.getType().getDescriptionId() + " (" + image3.getWidth() + " * " + image3.getHeight() + ")");
                }
                if (fulfilledImageCreationRequests == imageCreationRequests) {
                    textureAtlas.stitchNew();
                    if (VoxelConstants.DEBUG) {
                        VoxelConstants.getLogger().info("EntityMapImageManager: Stiching!");
                        textureAtlas.saveDebugImage();
                    }
                }
            });
        });
    }

    private ModelPart[] getPartToRender(EntityModel<?> model) {
        if (model instanceof TropicalFishModelA || model instanceof TropicalFishModelB || model instanceof SalmonModel || model instanceof CodModel || model instanceof SlimeModel) {
            return new ModelPart[] { model.root() };
        }
        // horses
        for (ModelPart part : model.allParts()) {
            if (part.hasChild("head_parts")) {
                return new ModelPart[] { part.getChild("head_parts") };
            }
        }
        // most mobs
        for (ModelPart part : model.allParts()) {
            if (part.hasChild("head")) {
                if (part.hasChild("body0")) {
                    // spider
                    return new ModelPart[] { part.getChild("head"), part.getChild("body0") };
                }
                return new ModelPart[] { part.getChild("head") };
            }
        }
        // bee, ghast
        for (ModelPart part : model.allParts()) {
            if (part.hasChild("body")) {
                return new ModelPart[] { part.getChild("body") };
            }
        }
        // bee, ghast, slime
        for (ModelPart part : model.allParts()) {
            if (part.hasChild("cube")) {
                return new ModelPart[] { part.getChild("cube") };
            }
        }
        // silverfish, endermite
        for (ModelPart part : model.allParts()) {
            if (part.hasChild("segment0")) {
                return new ModelPart[] { part.getChild("segment0"), part.getChild("segment1") };
            }
        }
        // magma slime: head_parts
        if (model instanceof LavaSlimeModel slime) {
            return slime.bodyCubes;
        }
        return new ModelPart[] { model.root() };
    }

    private BufferedImage getPlayerIcon(AbstractClientPlayer player, int size, boolean addBorder) {
        ResourceLocation skinLocation = player.getSkin().texture();
        AbstractTexture skinTexture = minecraft.getTextureManager().getTexture(skinLocation);
        BufferedImage skinImage = null;
        if (skinTexture instanceof DynamicTexture dynamicTexture) {
            skinImage = ImageUtils.bufferedImageFromNativeImage(dynamicTexture.getPixels());
        } else { // should be ReloadableImage
            skinImage = ImageUtils.createBufferedImageFromResourceLocation(skinLocation);
        }

        if (skinImage == null) {
            if (VoxelConstants.DEBUG) {
                VoxelConstants.getLogger().info("Got no player skin! -> " + skinLocation + " -- " + skinTexture.getClass());
            }
            return null;
        }

        boolean showHat = VoxelConstants.getPlayer().isModelPartShown(PlayerModelPart.HAT);
        if (showHat) {
            skinImage = ImageUtils.addImages(ImageUtils.loadImage(skinImage, 8, 8, 8, 8), ImageUtils.loadImage(skinImage, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
        } else {
            skinImage = ImageUtils.loadImage(skinImage, 8, 8, 8, 8);
        }

        float scale = size == -1 ? 2 : (float) size / skinImage.getWidth();
        skinImage = ImageUtils.pad(ImageUtils.scaleImage(skinImage, scale));

        if (addBorder) {
            skinImage = ImageUtils.fillOutline(skinImage, true, 1);
        }
        return skinImage;
    }

    public void onRenderTick(GuiGraphics drawContext) {
        Runnable task;
        while ((task = taskQueue.poll()) != null) {
            task.run();
        }
    }
}
