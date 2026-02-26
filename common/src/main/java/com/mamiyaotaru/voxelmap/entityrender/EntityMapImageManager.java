package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantData;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.EnderDragonVarintDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.HorseVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.VillagerVariantDataFactory;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.AllocatedTexture;
import com.mamiyaotaru.voxelmap.util.EmptySubmitNodeCollector;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapCachedOrthoProjectionMatrixBuffer;
import com.mamiyaotaru.voxelmap.util.VoxelMapPipelines;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.animal.fish.CodModel;
import net.minecraft.client.model.animal.fish.SalmonModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.model.animal.llama.LlamaModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.slime.MagmaCubeModel;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.model.monster.zombie.ZombieVillagerModel;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
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
    private final RandomSource randomSource = RandomSource.create();

    private final HashMap<EntityType<?>, EntityVariantDataFactory> variantDataFactories = new HashMap<>();
    private final HashMap<Identifier, EntityArmorDataFactory> armorDataFactories = new HashMap<>();
    private final EmptySubmitNodeCollector emptySubmitNodeCollector = new EmptySubmitNodeCollector();
    private final Class<?>[] fullRenderModels;
    private final HumanoidModel<?> humanoidModel;
    private final HashMap<EntityType<?>, String> customMobIcons = new HashMap<>();
    private final HashMap<EntityType<?>, Properties> customMobProperties = new HashMap<>();

    private int imageCreationRequests;
    private int fulfilledImageCreationRequests;
    private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    private static final int LIGHT = LightCoordsUtil.FULL_BRIGHT;
    private static final int OVERLAY = OverlayTexture.NO_OVERLAY;
    private final GpuBuffer lightingBuffer;

    private final VoxelMapCachedOrthoProjectionMatrixBuffer projection;
    private final Identifier resourceFboTexture = Identifier.fromNamespaceAndPath("voxelmap", "entityimagemanager/fbo");
    private final Tesselator fboTessellator = new Tesselator(4096);
    private final GpuTexture fboTexture;
    private final GpuTexture fboDepthTexture;
    private final GpuTextureView fboTextureView;
    private final GpuTextureView fboDepthTextureView;

    public EntityMapImageManager() {
        this.textureAtlas = new TextureAtlas("mobsmap", resourceTextureAtlasMarker);
        this.textureAtlas.setFilter(true, false);

        this.fullRenderModels = new Class[] { CodModel.class, MagmaCubeModel.class, SalmonModel.class, SlimeModel.class, TropicalFishSmallModel.class, TropicalFishLargeModel.class };
        CubeDeformation armorInflate = new CubeDeformation(1.0F);
        LayerDefinition layerDefinition = LayerDefinition.create(HumanoidModel.createMesh(armorInflate, 0.0F), 64, 32);
        this.humanoidModel = new HumanoidModel<>(layerDefinition.bakeRoot());

        Vector3f fullBright = new Vector3f(1.0F, -1.0F, 1.0F).normalize();
        Vector3f fullBright2 = new Vector3f(-1.0F, -1.0F, 1.0F).normalize();
        this.lightingBuffer = RenderSystem.getDevice().createBuffer(() -> "VoxelMap Lighting UBO", GpuBuffer.USAGE_UNIFORM + GpuBuffer.USAGE_COPY_DST, Lighting.UBO_SIZE);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, Lighting.UBO_SIZE).putVec3(fullBright).putVec3(fullBright2).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.lightingBuffer.slice(), byteBuffer);
        }

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
        debugInfo("EntityMapImageManager: Resetting");

        this.textureAtlas.reset();
        this.textureAtlas.registerIconForBufferedImage("hostile", ImageUtils.loadImage(Identifier.fromNamespaceAndPath("voxelmap", "images/radar/hostile.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.registerIconForBufferedImage("neutral", ImageUtils.loadImage(Identifier.fromNamespaceAndPath("voxelmap", "images/radar/neutral.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.registerIconForBufferedImage("tame", ImageUtils.loadImage(Identifier.fromNamespaceAndPath("voxelmap", "images/radar/tame.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.stitch();

        reloadConfigurations();

        variantDataFactories.clear();
        armorDataFactories.clear();

        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.BOGGED, Identifier.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png"), null, null));
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.DROWNED, Identifier.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png"), null, null));
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.ENDERMAN, Identifier.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png"), null, null));
//        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.TROPICAL_FISH, null, null, null));
        addVariantDataFactory(new HorseVariantDataFactory(EntityType.HORSE));
        addVariantDataFactory(new EnderDragonVarintDataFactory(EntityType.ENDER_DRAGON));
        addVariantDataFactory(new VillagerVariantDataFactory(EntityType.VILLAGER));
        addVariantDataFactory(new VillagerVariantDataFactory(EntityType.ZOMBIE_VILLAGER));

        if (VoxelConstants.DEBUG) {
            BuiltInRegistries.ENTITY_TYPE.forEach(t -> {
                requestImageForMobType(t, 32, true);
            });
        }
    }

    private void reloadConfigurations() {
        customMobIcons.clear();
        customMobProperties.clear();

        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            String entityId = type.getDescriptionId();

            Identifier iconPath = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "configs/radar_icon/" + entityId + ".png");
            Optional<Resource> iconResource = minecraft.getResourceManager().getResource(iconPath);
            if (iconResource.isPresent() && loadCustomMobIcon(iconPath, entityId)) {
                customMobIcons.put(type, entityId);
            }

            Identifier propertiesPath = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "configs/radar_icon/" + entityId + ".properties");
            Optional<Resource> propertiesResource = minecraft.getResourceManager().getResource(propertiesPath);
            if (propertiesResource.isPresent()) {
                Properties properties = new Properties();
                try (InputStream is = propertiesResource.get().open()) {
                    properties.load(is);
                } catch (IOException ignored) { }

                customMobProperties.put(type, properties);
            }
        });
    }

    private boolean loadCustomMobIcon(Identifier iconPath, String iconName) {
        BufferedImage image = ImageUtils.createBufferedImageFromIdentifier(iconPath);
        if (image == null) return false;

        image = ImageUtils.trim(image);
        image = ImageUtils.intoSquare(image);

        BufferedImage newImage = ImageUtils.fillOutline(ImageUtils.pad(image), true, 2);
        newImage = ImageUtils.intoSquare(newImage);

        int maxTries = 3;
        int tries = 0;

        while (textureAtlas.getAtlasSprite(iconName) == textureAtlas.getMissingImage()) {
            textureAtlas.registerIconForBufferedImage(iconName, image);
            textureAtlas.stitchNew();

            if (++tries > maxTries) return false;
        }

        tries = 0;
        while (textureAtlas.getAtlasSprite(iconName + "_outlined") == textureAtlas.getMissingImage()) {
            textureAtlas.registerIconForBufferedImage(iconName + "_outlined", newImage);
            textureAtlas.stitchNew();

            if (++tries > maxTries) return false;
        }

        return true;
    }

    public String getCustomMobIcon(EntityType<?> type, boolean outline) {
        String icon = customMobIcons.get(type);
        if (icon != null && outline) {
            return icon + "_outlined";
        }
        return icon;
    }

    public Properties getCustomMobProperties(EntityType<?> type) {
        return customMobProperties.get(type);
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

    private EntityVariantData getVariantData(Entity entity, @SuppressWarnings("rawtypes") EntityRenderer renderer, EntityRenderState state, int identifier, int size, boolean addBorder) {
        EntityVariantDataFactory factory = variantDataFactories.get(entity.getType());
        if (factory != null) {
            EntityVariantData data = factory.createVariantData(entity, renderer, state, identifier, size, addBorder);
            if (data != null) {
                return data;
            }
        }
        return DefaultEntityVariantDataFactory.createSimpleVariantData(entity, renderer, state, identifier, size, addBorder);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private EntityVariantData getOrCreateVariantData(Entity entity, EntityRenderer renderer, int identifier, int size, boolean addBorder) {
        EntityRenderState renderState = null;
        if (entity instanceof AbstractClientPlayer player) {
            return new DefaultEntityVariantData(entity.getType(), identifier, size, addBorder, player.getSkin().body().texturePath(), null, null, null);
        }

        if (entity instanceof LivingEntity entity2 && renderer instanceof LivingEntityRenderer renderer2) {
            renderState = renderer2.createRenderState(entity2, 0.5f);
        } else if (entity instanceof EnderDragon entity2 && renderer instanceof EnderDragonRenderer renderer2) {
            renderState = renderer2.createRenderState(entity2, 0.5f);
        }

        if (renderState == null) {
            return null;
        }

        return getVariantData(entity, renderer, renderState, identifier, size, addBorder);
    }

    @SuppressWarnings("rawtypes")
    private EntityModel getEntityModel(EntityRenderer renderer) {
        if (renderer instanceof LivingEntityRenderer renderer2) {
            return renderer2.getModel();
        } else if (renderer instanceof EnderDragonRenderer renderer2) {
            return renderer2.model;
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Sprite requestImageForMob(Entity entity, int size, boolean addBorder) {
        String customIcon = getCustomMobIcon(entity.getType(), addBorder);
        if (customIcon != null) {
            return textureAtlas.getAtlasSpriteIncludingYetToBeStitched(customIcon);
        }

        EntityRenderer<?, ?> baseRenderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        int identifier = getMobIdentifier(entity);
        EntityVariantData variant = getOrCreateVariantData(entity, baseRenderer, identifier, size, addBorder);

        if (variant == null) {
            return null;
        }

        Sprite existing = textureAtlas.getAtlasSpriteIncludingYetToBeStitched(variant);
        if (existing != null && existing != textureAtlas.getMissingImage()) {
//            debugInfo("EntityMapImageManager: Existing type " + entity.getType().getDescriptionId());
            return existing;
        }
        debugInfo("EntityMapImageManager: Rendering Mob of type " + entity.getType().getDescriptionId());

        Sprite sprite = textureAtlas.registerEmptyIcon(variant);

        Identifier primaryTexture = variant.getPrimaryTexture();
        Identifier secondaryTexture = variant.getSecondaryTexture();
        Identifier tertiaryTexture = variant.getTertiaryTexture();
        Identifier quaternaryTexture = variant.getQuaternaryTexture();

        CaptureContext context = setupCapture(VoxelMapPipelines.ENTITY_ICON);
        PoseStack pose = context.poseStack();
        BufferBuilder bufferBuilder = context.bufferBuilder();

        Properties mobProperties = getCustomMobProperties(entity.getType());
        float iconScale = 1.0F;
        if (mobProperties != null) {
            iconScale = Float.parseFloat(mobProperties.getProperty("scale", "1.0"));

            String rotation = mobProperties.getProperty("rotation", "");
            if (rotation.startsWith("{") && rotation.endsWith("}")) {
                for (String entry : rotation.substring(1, rotation.length() - 1).split(",")) {
                    String[] keyValue = entry.split(":");
                    if (keyValue.length != 2) continue;

                    float value = Float.parseFloat(keyValue[1].trim());
                    switch (keyValue[0].trim()) {
                        case "x" -> pose.mulPose(Axis.XP.rotationDegrees(value));
                        case "y" -> pose.mulPose(Axis.YP.rotationDegrees(value));
                        case "z" -> pose.mulPose(Axis.ZP.rotationDegrees(value));
                    }
                }
            }
        }
        float iconScale2 = iconScale;

        EntityRenderState renderState = ((EntityRenderer) baseRenderer).createRenderState(entity, 0.5F);
        ((EntityRenderer) baseRenderer).submit(renderState, pose, emptySubmitNodeCollector, minecraft.gameRenderer.getLevelRenderState().cameraRenderState);

        EntityModel model = getEntityModel(baseRenderer);
        if (model == null) {
            return null;
        }

        model.resetPose();

        for (ModelPart part : getPartToRender(model)) {
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            part.render(pose, bufferBuilder, LIGHT, OVERLAY, 0xffffffff); // light, overlay, color
        }

        if (baseRenderer instanceof SlimeRenderer slimeRenderer) {
            SlimeOuterLayer slimeOuter = (SlimeOuterLayer) slimeRenderer.layers.get(0);
            slimeOuter.model.root().render(pose, bufferBuilder, LIGHT, OVERLAY, 0xffffffff); // light, overlay, color
        }

        boolean success = this.doCapture(context, primaryTexture, secondaryTexture, tertiaryTexture, quaternaryTexture);
        if (!success) {
            return null;
        }

        // if (VoxelConstants.DEBUG) {
        // ImageUtils.saveImage("mob_" + entity.getType().getDescriptionId(), fboTexture, 0, fboTexture.getWidth(0), fboTexture.getHeight(0));
        // }
        imageCreationRequests++;
        GLUtils.readTextureContentsToBufferedImage(fboTexture, image2 -> {
            postProcessRenderedMobImage(entity, sprite, model, image2, addBorder, iconScale2);
        });

        return sprite;
    }

    private int getMobIdentifier(Entity entity) {
        int id = 0;
        switch (entity) {
            case Pufferfish pufferfish -> id = pufferfish.getPuffState();
            case Sheep sheep -> id = (sheep.isSheared() ? (1 << 8) : sheep.getColor().getId());
            default -> {}
        }

        if (entity instanceof LivingEntity livingEntity && livingEntity.isBaby()) {
            id |= (1 << 9);
        }

        return id;
    }

    private void postProcessRenderedMobImage(Entity entity, Sprite sprite, @SuppressWarnings("rawtypes") EntityModel model, BufferedImage image2, boolean addBorder, float scale) {
        Util.backgroundExecutor().execute(() -> {
            BufferedImage image = image2;
            image = ImageUtils.flipHorizontal(image);
            // try {
            // ImageIO.write(image, "png", new File(entity.getType().getDescriptionId() + ".png"));
            // } catch (IOException e) {
            // e.printStackTrace();
            // }

            boolean useFixedScale = false;
            switch (model) {
                case CamelModel camelModel -> {
                    Graphics2D g = image.createGraphics();
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(0, 192, image.getWidth(), image.getHeight());
                }
                case LlamaModel llamaModel -> {
                    Graphics2D g = image.createGraphics();
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(0, 248, image.getWidth(), image.getHeight());
                }
                case HappyGhastModel happyGhastModel -> {
                    Graphics2D g = image.createGraphics();
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(0,  352, image.getWidth(), image.getHeight());
                }
                case SalmonModel salmonModel -> useFixedScale = true;
                case SheepModel sheepModel -> {
                    Sheep sheepEntity = (Sheep) entity;
                    if (!sheepEntity.isSheared()) {
                        Graphics2D g = image.createGraphics();
                        g.setComposite(AlphaComposite.DstOver);
                        g.setColor(new Color(sheepEntity.getColor().getTextureDiffuseColor()));
                        g.fillRect(242, 262, image.getWidth() - 484, image.getHeight() - 484);
                    }
                }
                default -> {}
            }

            image = ImageUtils.trim(image);
            if (useFixedScale) {
                int maxSize = Math.max(image.getWidth(), image.getHeight());
                image = ImageUtils.scaleImage(image, 64.0F / maxSize);
            }
            image = ImageUtils.scaleImage(image, scale);
            image = ImageUtils.fillOutline(ImageUtils.pad(image), addBorder, 2);

            addToTaskQueue(sprite, image, entity.getType().getDescriptionId().toString());
        });
    }

    private EntityArmorData getArmorData(Identifier material, int size, boolean addBorder) {
        EntityArmorDataFactory factory = armorDataFactories.get(material);
        if (factory != null) {
            return factory.createArmorData(size, addBorder);
        }

        return null;
    }

    private EntityArmorData getOrCreateArmorData(Identifier material, Identifier texture, int size, boolean addBorder) {
        if (!armorDataFactories.containsKey(material)) {
            EntityArmorDataFactory factory = new EntityArmorDataFactory(material, texture);
            armorDataFactories.put(material, factory);
        }

        return getArmorData(material, size, addBorder);
    }

    public Sprite requestImageForArmor(Entity entity, int size, boolean addBorder) {
        EntityRenderer<?, ?> entityRenderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        if (!(entity instanceof LivingEntity livingEntity) || !(entityRenderer instanceof LivingEntityRenderer<?,?,?> livingEntityRenderer) || !(livingEntityRenderer.getModel() instanceof HumanoidModel)) {
            return null;
        }

        ItemStack itemStack = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
        if (itemStack.isEmpty()) {
            return null;
        }

        EntityArmorData armorData = null;

        // get item texture
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        Block block = null;
        if (equippable != null) {
            Optional<ResourceKey<EquipmentAsset>> assetId = equippable.assetId();
            if (assetId.isPresent()) {
                Identifier material = assetId.get().identifier();
                armorData = getArmorData(material, size, addBorder);
                if (armorData == null) {
                    Identifier texture = Identifier.fromNamespaceAndPath(material.getNamespace(), "textures/entity/equipment/humanoid/" + material.getPath() + ".png");
                    armorData = getOrCreateArmorData(material, texture, size, addBorder);
                }
            }
        } else if (itemStack.getItem() instanceof BlockItem blockItem) {
            Block tempBlock = blockItem.getBlock();
            if (tempBlock.defaultBlockState().getRenderShape() == RenderShape.MODEL) {
                block = tempBlock;
                Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
                armorData = getArmorData(blockId, size, addBorder);
                if (armorData == null) {
                    Identifier texture = minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).location();
                    armorData = getOrCreateArmorData(blockId, texture, size, addBorder);
                }
            }
        }

        if (armorData == null) {
            return null;
        }

        Sprite existing = textureAtlas.getAtlasSpriteIncludingYetToBeStitched(armorData);
        if (existing != null && existing != textureAtlas.getMissingImage()) {
            return existing;
        }
        Sprite sprite = textureAtlas.registerEmptyIcon(armorData);

        Identifier armorTexture = armorData.getTexture();

        CaptureContext context = this.setupCapture(VoxelMapPipelines.ENTITY_ICON_CULLED);
        PoseStack pose = context.poseStack();
        BufferBuilder bufferBuilder = context.bufferBuilder();

        if (equippable != null) {
            ModelPart part = humanoidModel.root().getChild("head");
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            part.render(pose, bufferBuilder, LIGHT, OVERLAY, 0xFFFFFFFF);
        } else if (block != null) {
            // FIXME 26.1: tessellate block
//
//            pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
//            pose.scale(0.65F, 0.65F, 0.65F);
//
//            BlockState blockState = block.defaultBlockState();
//            BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
//            List<BlockModelPart> blockMesh = blockRenderer.getBlockModel(blockState).collectParts(this.randomSource);
//
//            blockRenderer.getModelRenderer().tesselateBlock(minecraft.level, blockMesh, blockState, BlockPos.ZERO, pose, bufferBuilder, true, OVERLAY);
        }

        this.doCapture(context, armorTexture, null, null, null);

        imageCreationRequests++;
        GLUtils.readTextureContentsToBufferedImage(fboTexture, image2 -> {
            postProcessRenderedArmorImage(sprite, image2, addBorder);
        });

        return sprite;
    }

    private void postProcessRenderedArmorImage(Sprite sprite, BufferedImage image2, boolean addBorder) {
        Util.backgroundExecutor().execute(() -> {
            BufferedImage image = image2;
            image = ImageUtils.flipHorizontal(image);
            image = ImageUtils.trim(image);

            // make the image square and align it at the top
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getWidth(), image.getType());
            newImage = ImageUtils.addImages(newImage, image, 0, 0, image.getWidth(), image.getHeight());
            newImage = ImageUtils.fillOutline(ImageUtils.pad(newImage), addBorder, true, 37.5F, 37.5F, 2);

            addToTaskQueue(sprite, newImage, sprite.getIconName().toString());
        });
    }

    private void addToTaskQueue(Sprite sprite, BufferedImage image, String debugId) {
        taskQueue.add(() -> {
            fulfilledImageCreationRequests++;

            sprite.setTextureData(ImageUtils.nativeImageFromBufferedImage(image));
            debugInfo("EntityMapImageManager: Buffered Image (" + fulfilledImageCreationRequests + "/" + imageCreationRequests + ") added to texture atlas " + debugId + " (" + image.getWidth() + " * " + image.getHeight() + ")");
            if (fulfilledImageCreationRequests == imageCreationRequests) {
                textureAtlas.stitchNew();
                debugInfo("EntityMapImageManager: Stiching!");
                if (VoxelConstants.DEBUG) {
                    textureAtlas.saveDebugImage();
                }
            }
        });
    }

    private ModelPart[] getPartToRender(EntityModel<?> model) {
        // full-model rendered mobs
        for (Class<?> clazz : fullRenderModels) {
            if (clazz.isInstance(model)) {
                return new ModelPart[] { model.root() };
            }
        }

        // wither
        if (model instanceof WitherBossModel witherModel) {
            return new ModelPart[] { witherModel.root().getChild("left_head"), witherModel.root().getChild("center_head"), witherModel.root().getChild("right_head") };
        }

        // villager
        if (model instanceof VillagerModel villagerModel) {
            return new ModelPart[] { villagerModel.root().getChild("head"), villagerModel.root().getChild("head").getChild("hat") };
        }
        if (model instanceof ZombieVillagerModel<?> zombieVillagerModel) {
            return new ModelPart[] { zombieVillagerModel.root().getChild("head"), zombieVillagerModel.root().getChild("head").getChild("hat") };
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

    private CaptureContext setupCapture(RenderPipeline renderPipeline) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0f, 0.0f, -3000.0f);
        float scale = 64;
        poseStack.scale(scale, scale, -scale);

        BufferBuilder bufferBuilder = fboTessellator.begin(Mode.QUADS, renderPipeline.getVertexFormat());

        return new CaptureContext(poseStack, renderPipeline, bufferBuilder);
    }

    private boolean doCapture(CaptureContext context, Identifier primary, Identifier secondary, Identifier tertiary, Identifier quaternary) {
        RenderPipeline renderPipeline = context.renderPipeline();
        BufferBuilder bufferBuilder = context.bufferBuilder();

        AbstractTexture primaryTexture = minecraft.getTextureManager().getTexture(primary);
        AbstractTexture secondaryTexture = secondary == null ? null : minecraft.getTextureManager().getTexture(secondary);
        AbstractTexture tertiaryTexture = tertiary == null ? null : minecraft.getTextureManager().getTexture(tertiary);
        AbstractTexture quaternaryTexture = quaternary  == null ? null : minecraft.getTextureManager().getTexture(quaternary);

        RenderSystem.setShaderLights(lightingBuffer.slice());
		
        ProjectionType originalProjectionType = RenderSystem.getProjectionType();
        GpuBufferSlice originalProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
        RenderSystem.setProjectionMatrix(projection.getBuffer(), ProjectionType.ORTHOGRAPHIC);

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
                return false;
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

            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "VoxelMap entity image renderer", fboTextureView, OptionalInt.of(0x00000000), fboDepthTextureView, OptionalDouble.of(1.0))) {
                renderPass.setPipeline(renderPipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                renderPass.bindTexture("Sampler0", primaryTexture.getTextureView(), primaryTexture.getSampler());
                renderPass.bindTexture("Sampler1", minecraft.gameRenderer.overlayTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indexBuffer, indexType);
                renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
                if (secondaryTexture != null) {
                    renderPass.bindTexture("Sampler0", secondaryTexture.getTextureView(), secondaryTexture.getSampler());
                    renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
                }
                if (tertiaryTexture != null) {
                    renderPass.bindTexture("Sampler0", tertiaryTexture.getTextureView(), tertiaryTexture.getSampler());
                    renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
                }
                if (quaternaryTexture != null) {
                    renderPass.bindTexture("Sampler0", quaternaryTexture.getTextureView(), quaternaryTexture.getSampler());
                    renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
                }
            }
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(originalProjectionMatrix, originalProjectionType);
        }

        return true;
    }

    public void onRenderTick(Matrix4fStack matrixStack) {
        Runnable task;
        while ((task = taskQueue.poll()) != null) {
            task.run();
        }
    }

    private void debugInfo(String str) {
        if (VoxelConstants.DEBUG) {
            VoxelConstants.getLogger().info(str);
        }
    }

    private record CaptureContext(PoseStack poseStack, RenderPipeline renderPipeline, BufferBuilder bufferBuilder) {}
}
