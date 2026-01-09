package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantData;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.HorseVariantDataFactory;
import com.mamiyaotaru.voxelmap.mixins.AccessorEnderDragonRenderer;
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
import net.minecraft.util.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.animal.fish.CodModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.model.monster.slime.MagmaCubeModel;
import net.minecraft.client.model.animal.llama.LlamaModel;
import net.minecraft.client.model.animal.fish.SalmonModel;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EntityMapImageManager {
    public static final Identifier resourceTextureAtlasMarker = Identifier.fromNamespaceAndPath("voxelmap", "atlas/mobs");
    private final TextureAtlas textureAtlas;
    private final Minecraft minecraft = Minecraft.getInstance();
    private GpuTexture fboDepthTexture;
    private GpuTexture fboTexture;
    private final Identifier resourceFboTexture = Identifier.fromNamespaceAndPath("voxelmap", "entityimagemanager/fbo");
    private Tesselator fboTessellator = new Tesselator(4096);
    private int imageCreationRequests;
    private int fulfilledImageCreationRequests;
    private final HashMap<EntityType<?>, EntityVariantDataFactory> variantDataFactories = new HashMap<>();
    private ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final Camera fakeCamera = new Camera();
    private GpuTextureView fboTextureView;
    private GpuTextureView fboDepthTextureView;
    private VoxelMapCachedOrthoProjectionMatrixBuffer projection;
    private final HashMap<String, Properties> mobPropertiesMap = new HashMap<>();
    private final Class<?>[] fullRenderModels = new Class[] { CodModel.class, MagmaCubeModel.class, SalmonModel.class, SlimeModel.class, TropicalFishSmallModel.class, TropicalFishLargeModel.class };

    public EntityMapImageManager() {
        this.textureAtlas = new TextureAtlas("mobsmap", resourceTextureAtlasMarker);
        this.textureAtlas.setFilter(true, false);

        final int fboTextureSize = 512;
        this.fboTexture = RenderSystem.getDevice().createTexture("voxelmap-radarfbotexture", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, fboTextureSize, fboTextureSize, 1, 1);
        this.fboDepthTexture = RenderSystem.getDevice().createTexture("voxelmap-radarfbodepth", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.DEPTH32, fboTextureSize, fboTextureSize, 1, 1);
        Minecraft.getInstance().getTextureManager().register(resourceFboTexture, new AllocatedTexture(fboTexture));

        // this.fboTexture = fboTexture.getTexture();
        fboTextureView = RenderSystem.getDevice().createTextureView(this.fboTexture);
        fboDepthTextureView = RenderSystem.getDevice().createTextureView(this.fboDepthTexture);

        projection = new VoxelMapCachedOrthoProjectionMatrixBuffer("VoxelMap Entity Map Image Proj", 256.0F, -256.0F, -256.0F, 256.0F, 1000.0F, 21000.0F);
        reset();
    }

    public void reset() {
        if (VoxelConstants.DEBUG) {
            VoxelConstants.getLogger().info("EntityMapImageManager: Resetting");
        }

        this.textureAtlas.reset();
        this.textureAtlas.registerIconForBufferedImage("hostile", ImageUtils.loadImage(Identifier.fromNamespaceAndPath("voxelmap", "images/radar/hostile.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.registerIconForBufferedImage("neutral", ImageUtils.loadImage(Identifier.fromNamespaceAndPath("voxelmap", "images/radar/neutral.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.registerIconForBufferedImage("tame", ImageUtils.loadImage(Identifier.fromNamespaceAndPath("voxelmap", "images/radar/tame.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.stitch();

        mobPropertiesMap.clear();
        variantDataFactories.clear();
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.BOGGED, Identifier.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png")));
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.DROWNED, Identifier.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png")));
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.ENDERMAN, Identifier.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png")));
        // addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.TROPICAL_FISH, Identifier.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png")));
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

    public Sprite requestImageForMobType(EntityType<?> type, boolean addBorder) {
        return requestImageForMobType(type, -1, addBorder);
    }

    public Sprite requestImageForMobType(EntityType<?> type, int size, boolean addBorder) {
        if (minecraft.level != null && type.create(minecraft.level, EntitySpawnReason.LOAD) instanceof LivingEntity le) {
            return requestImageForMob(le, size, addBorder);
        }
        return null;
    }

    public Sprite requestImageForMob(LivingEntity e, boolean addBorder) {
        return requestImageForMob(e, -1, addBorder);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private EntityVariantData getOrCreateVariantData(Entity entity, EntityRenderer renderer, int size, boolean addBorder) {
        EntityRenderState renderState = null;
        if (entity instanceof AbstractClientPlayer player) {
            return new DefaultEntityVariantData(entity.getType(), player.getSkin().body().texturePath(), null, size, addBorder);
        }

        if (entity instanceof LivingEntity entity2 && renderer instanceof LivingEntityRenderer renderer2) {
            renderState = renderer2.createRenderState(entity2, 0.5f);
        } else if (entity instanceof EnderDragon entity2 && renderer instanceof EnderDragonRenderer renderer2) {
            renderState = renderer2.createRenderState(entity2, 0.5f);
        }

        if (renderState == null) {
            return null;
        }

        return getVariantData(entity, renderer, renderState, size, addBorder);
    }

    @SuppressWarnings("rawtypes")
    private EntityModel getEntityModel(EntityRenderer renderer) {
        if (renderer instanceof LivingEntityRenderer renderer2) {
            return renderer2.getModel();
        } else if (renderer instanceof EnderDragonRenderer renderer2) {
            return ((AccessorEnderDragonRenderer) renderer2).getModel();
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Sprite requestImageForMob(Entity entity, int size, boolean addBorder) {
        EntityRenderer<?, ?> baseRenderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        EntityVariantData variant = getOrCreateVariantData(entity, baseRenderer, size, addBorder);

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

        // getPlayerIcon() sometimes causes an unknown error and breaks the entire radar.
        // if (entity instanceof AbstractClientPlayer player) {
        //     BufferedImage playerImage = getPlayerIcon(player, size, addBorder);
        //     postProcessRenderedMobImage(entity, sprite, null, playerImage);
        //     return sprite;
        // }

        Identifier Identifier = variant.getPrimaryTexture();
        Identifier Identifier2 = variant.getSecondaryTexture();

        // VoxelConstants.getLogger().info(" -> " + Identifier);
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

        Properties mobProperties = getMobProperties(entity);
        String rotation = mobProperties.getProperty("rotation", "");
        if (!rotation.isEmpty()) {
            for (String data : rotation.split(",")) {
                if (data.length() < 2) continue;

                char key = data.charAt(0);
                float degrees = Float.parseFloat(data.substring(1));

                switch (key) {
                    case 'x' -> pose.mulPose(Axis.XP.rotationDegrees(degrees));
                    case 'y' -> pose.mulPose(Axis.YP.rotationDegrees(degrees));
                    case 'z' -> pose.mulPose(Axis.ZP.rotationDegrees(degrees));
                }
            }
        }

        EntityModel model = getEntityModel(baseRenderer);

        if (model == null) {
            return null;
        }


        // setupAnim() causes an error when the entity is a player.
        // model.setupAnim((LivingEntityRenderState) renderState);

        // We can also use resetPose() instead of setupAnim() to get the default pose.
        model.resetPose();

        for (ModelPart part : getPartToRender(model)) {
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            part.render(pose, bufferBuilder, 15, 0, 0xffffffff); // light, overlay, color //TODO set model tint
        }
        if (baseRenderer instanceof SlimeRenderer slimeRenderer) {
            SlimeOuterLayer slimeOuter = (SlimeOuterLayer) slimeRenderer.layers.get(0);
            slimeOuter.model.root().render(pose, bufferBuilder, 15, 0, 0xffffffff); // light, overlay, color
        }

        AbstractTexture texture = minecraft.getTextureManager().getTexture(Identifier);
        AbstractTexture texture2 = Identifier2 == null ? null : minecraft.getTextureManager().getTexture(Identifier2);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
                .writeTransform(
                        RenderSystem.getModelViewMatrix(),
                        new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                        new Vector3f(),
                        new Matrix4f());

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
                renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
                // renderPass.bindSampler("Sampler1", texture.getTexture()); // overlay
                // minecraft.gameRenderer.overlayTexture().setupOverlayColor();
                // renderPass.bindSampler("Sampler2", texture.getTexture()); // lightmap
                // minecraft.gameRenderer.lightTexture().turnOnLightLayer();
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indexBuffer, indexType);
                renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);

                if (texture2 != null) {
                    renderPass.bindTexture("Sampler0", texture2.getTextureView(), texture2.getSampler());
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
            postProcessRenderedMobImage(entity, sprite, model, image2, addBorder);
        });

        return sprite;
    }

    private void postProcessRenderedMobImage(Entity entity, Sprite sprite, @SuppressWarnings("rawtypes") EntityModel model, BufferedImage image2, boolean addBorder) {
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
            if (model instanceof HappyGhastModel) {
                Graphics2D g = image.createGraphics();
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0,  352, image.getWidth(), image.getHeight());
            }
            image = ImageUtils.trim(image);
            float maxSize = Math.max(image.getHeight(), image.getWidth());
            float targetSize = maxSize;
            if (model instanceof SalmonModel) {
                targetSize = 64.0F;
            }
            Properties mobProperties = getMobProperties(entity);
            targetSize *= Float.parseFloat(mobProperties.getProperty("scale", "1.0"));
            if (maxSize > 0 && maxSize != targetSize) {
                image = ImageUtils.scaleImage(image, targetSize / maxSize);
            }

            image = ImageUtils.fillOutline(ImageUtils.pad(image), addBorder, 2);

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

    private Properties getMobProperties(Entity entity) {
        String entityId = entity.getType().getDescriptionId();
        String filePath = ("voxelmap:configs/radar_icon/" + entityId + ".properties").toLowerCase();

        if (mobPropertiesMap.containsKey(filePath)) {
            return mobPropertiesMap.get(filePath);
        } else {
            Properties properties = new Properties();
            Optional<Resource> resource = minecraft.getResourceManager().getResource(Identifier.parse(filePath));
            if (resource.isPresent()) {
                try (InputStream inputStream = resource.get().open()) {
                    properties.load(inputStream);
                } catch (Exception ignored) {
                }
            }
            mobPropertiesMap.put(filePath, properties);
            return properties;
        }
    }

    private ModelPart[] getPartToRender(EntityModel<?> model) {
        // full-model rendered mobs
        for (Class<?> clazz : fullRenderModels) {
            if (clazz.isInstance(model)) {
                return new ModelPart[] { model.root() };
            }
        }

        // some special mobs
        if (model instanceof WitherBossModel witherModel) {
            return new ModelPart[] { witherModel.root().getChild("left_head"), witherModel.root().getChild("center_head"), witherModel.root().getChild("right_head") };
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

        // fallback
        return new ModelPart[] { model.root() };
    }

    // We don't need to use this code anymore. (but I'll leave this code here in case we need it later!)
    //
    //private BufferedImage getPlayerIcon(AbstractClientPlayer player, int size, boolean addBorder) {
    //    Identifier skinLocation = player.getSkin().body().texturePath();
    //    AbstractTexture skinTexture = minecraft.getTextureManager().getTexture(skinLocation);
    //    BufferedImage skinImage = null;
    //    if (skinTexture instanceof DynamicTexture dynamicTexture) {
    //        skinImage = ImageUtils.bufferedImageFromNativeImage(dynamicTexture.getPixels());
    //    } else { // should be ReloadableImage
    //        skinImage = ImageUtils.createBufferedImageFromIdentifier(skinLocation);
    //    }
    //
    //    if (skinImage == null) {
    //        if (VoxelConstants.DEBUG) {
    //            VoxelConstants.getLogger().info("Got no player skin! -> " + skinLocation + " -- " + skinTexture.getClass());
    //        }
    //        return null;
    //    }
    //
    //    boolean showHat = VoxelConstants.getPlayer().isModelPartShown(PlayerModelPart.HAT);
    //    if (showHat) {
    //        skinImage = ImageUtils.addImages(ImageUtils.loadImage(skinImage, 8, 8, 8, 8), ImageUtils.loadImage(skinImage, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
    //    } else {
    //        skinImage = ImageUtils.loadImage(skinImage, 8, 8, 8, 8);
    //    }
    //
    //    float scale = size == -1 ? 2 : (float) size / skinImage.getWidth();
    //    skinImage = ImageUtils.pad(ImageUtils.scaleImage(skinImage, scale));
    //
    //    return skinImage;
    //}

    public void onRenderTick(GuiGraphics drawContext) {
        Runnable task;
        while ((task = taskQueue.poll()) != null) {
            task.run();
        }
    }
}
