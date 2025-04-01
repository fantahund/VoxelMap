package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.HorseVariantDataFactory;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
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
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import javax.imageio.ImageIO;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AbstractEquineModel;
import net.minecraft.client.model.CamelModel;
import net.minecraft.client.model.CodModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.LavaSlimeModel;
import net.minecraft.client.model.LlamaModel;
import net.minecraft.client.model.SalmonModel;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.TropicalFishModelA;
import net.minecraft.client.model.TropicalFishModelB;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.BoggedRenderer;
import net.minecraft.client.renderer.entity.CodRenderer;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.client.renderer.entity.HoglinRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.SalmonRenderer;
import net.minecraft.client.renderer.entity.TropicalFishRenderer;
import net.minecraft.client.renderer.entity.ZoglinRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Matrix4f;

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

    public EntityMapImageManager() {
        this.textureAtlas = new TextureAtlas("mobsmap", resourceTextureAtlasMarker);
        this.textureAtlas.reset();
        this.textureAtlas.registerIconForBufferedImage("neutral", ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/neutral.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.stitch();
        final int fboTextureSize = 512;
        DynamicTexture fboTexture = new DynamicTexture("voxelmap-radarfbotexture", fboTextureSize, fboTextureSize, true);
        this.fboDepthTexture = RenderSystem.getDevice().createTexture("voxelmap-radarfbodepth", TextureFormat.DEPTH32, fboTextureSize, fboTextureSize, 1);
        Minecraft.getInstance().getTextureManager().register(resourceFboTexture, fboTexture);
        this.fboTexture = fboTexture.getTexture();

    }

    public void reset() {
        VoxelConstants.getLogger().info("EntityMapImageManager: Resetting");
        variantDataFactories.clear();
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.BOGGED, ResourceLocation.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png")));
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.ENDERMAN, ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png")));
        // addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.TROPICAL_FISH, ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png")));
        addVariantDataFactory(new HorseVariantDataFactory(EntityType.HORSE));

        BuiltInRegistries.ENTITY_TYPE.forEach(t -> {
            requestImageForMobType(t);
        });
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
        VoxelConstants.getLogger().info("EntityMapImageManager: No living entity: " + type.getDescriptionId());
        return null;
    }

    public Sprite requestImageForMob(LivingEntity e) {
        return requestImageForMob(e, -1, true);
    }

    private <T extends LivingEntity, S extends LivingEntityRenderState> EntityVariantData getVariantData(T entity, LivingEntityRenderer<T, S, ?> renderer, S state, int size, boolean addBorder) {
        EntityVariantDataFactory factory = variantDataFactories.get(entity.getType());
        if (factory != null) {
            EntityVariantData data = factory.createVariantData(entity, renderer, state, size, addBorder);
            if (data != null) {
                return data;
            }
        }
        return DefaultEntityVariantDataFactory.createSimpleVariantData(entity, renderer, state, size, addBorder);
    }

    public <L extends LivingEntity, T extends LivingEntityRenderState> Sprite requestImageForMob(L entity, int size, boolean addBorder) {
        if (entity instanceof AbstractClientPlayer player) {
            // getPlayerIcon(player, size, addBorder);
        }
        EntityRenderer<? super L, ?> baseRenderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        if (!(baseRenderer instanceof LivingEntityRenderer)) {
            return null;
        }
        LivingEntityRenderer<? super L, T, ?> renderer = (LivingEntityRenderer<? super L, T, ?>) baseRenderer;

        boolean resetCamera = false;
        if (minecraft.getEntityRenderDispatcher().camera == null) {
            minecraft.getEntityRenderDispatcher().camera = new Camera();
            resetCamera = true;
        }

        T renderState = renderer.createRenderState(entity, 0.5f);

        if (resetCamera) {
            minecraft.getEntityRenderDispatcher().camera = null;
        }

        EntityVariantData variant = getVariantData(entity, renderer, renderState, size, addBorder);
        Sprite existing = textureAtlas.getAtlasSpriteIncludingYetToBeStitched(variant);
        if (existing != null) {
            return existing;
        }
        VoxelConstants.getLogger().info("EntityMapImageManager: Rendering Mob of type " + entity.getType().getDescriptionId());

        Sprite sprite = textureAtlas.registerEmptyIcon(variant);


        ResourceLocation resourceLocation = variant.getPrimaryTexture();
        VoxelConstants.getLogger().info("  -> " + resourceLocation);
        ResourceLocation resourceLocation2 = variant.getSecondaryTexture();

        // VoxelConstants.getLogger().info(" -> " + resourceLocation);
        RenderPipeline renderPipeline = GLUtils.ENTITY_ICON;
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

        if (renderer instanceof AbstractHorseRenderer<?, ?, ?>) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.XP.rotationDegrees(35.0F));
        }
        if (renderer instanceof SalmonRenderer || renderer instanceof CodRenderer || renderer instanceof TropicalFishRenderer) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        }
        if (renderer instanceof ParrotRenderer) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        }
        if (renderer instanceof HoglinRenderer || renderer instanceof ZoglinRenderer) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        }
        if (renderer instanceof GoatRenderer) {
            pose.mulPose(Axis.XP.rotationDegrees(-30.0F));
        }

        EntityModel<T> model = (EntityModel<T>) renderer.getModel();
        model.setupAnim(renderState);
        for (ModelPart part : getPartToRender(model)) {
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            part.render(pose, bufferBuilder, 15, 0, 0xffffffff); // light, overlay, color //TODO set model tint
        }

        AbstractTexture texture = minecraft.getTextureManager().getTexture(resourceLocation);
        AbstractTexture texture2 = resourceLocation2 == null ? null : minecraft.getTextureManager().getTexture(resourceLocation2);

        try (MeshData meshData = bufferBuilder.build()) {
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
            int width = fboTexture.getWidth(0);
            int height = fboTexture.getHeight(0);
            ProjectionType originalProjectionType = RenderSystem.getProjectionType();
            Matrix4f originalProjectionMatrix = RenderSystem.getProjectionMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f().ortho(256.0F, -256.0F, -256.0F, 256.0F, 1000.0F, 21000.0F), ProjectionType.ORTHOGRAPHIC);

            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(fboTexture, OptionalInt.of(0x00000000), fboDepthTexture, OptionalDouble.of(1.0))) {
                renderPass.setPipeline(renderPipeline);
                renderPass.bindSampler("Sampler0", texture.getTexture());
                // renderPass.bindSampler("Sampler1", texture.getTexture()); // overlay
                // minecraft.gameRenderer.overlayTexture().setupOverlayColor();
                // renderPass.bindSampler("Sampler2", texture.getTexture()); // lightmap
                // minecraft.gameRenderer.lightTexture().turnOnLightLayer();
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indexBuffer, indexType);
                renderPass.drawIndexed(0, meshData.drawState().indexCount());

                if (texture2 != null) {
                    renderPass.bindSampler("Sampler0", texture2.getTexture());
                    renderPass.drawIndexed(0, meshData.drawState().indexCount());
                }
            }

            RenderSystem.setProjectionMatrix(originalProjectionMatrix, originalProjectionType);

        }
        imageCreationRequests++;
        GLUtils.readTextureContentsToBufferedImage(fboTexture, image -> {
            fulfilledImageCreationRequests++;
            image = ImageUtils.flipHorizontal(image);
            try {
                ImageIO.write(image, "png", new File(entity.getType().getDescriptionId() + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            int targetSize = model instanceof AbstractEquineModel<?> ? 50 : 32;
            if (maxSize > 0 && maxSize != targetSize) {
                image = ImageUtils.scaleImage(image, (float) targetSize / maxSize);
            }
            image = ImageUtils.fillOutline(ImageUtils.pad(image), true, 2);

            sprite.setTextureData(ImageUtils.nativeImageFromBufferedImage(image));
            if (fulfilledImageCreationRequests == imageCreationRequests) {
                textureAtlas.stitchNew();
                if (VoxelConstants.DEBUG) {
                    textureAtlas.saveDebugImage();
                }
            }
        });

        return sprite;
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
}
