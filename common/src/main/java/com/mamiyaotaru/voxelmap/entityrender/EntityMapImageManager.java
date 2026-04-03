package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.entityrender.armors.AbstractArmorHandler;
import com.mamiyaotaru.voxelmap.entityrender.armors.DefaultArmorHandler;
import com.mamiyaotaru.voxelmap.entityrender.armors.EntityArmorData;
import com.mamiyaotaru.voxelmap.entityrender.armors.SheepOverlayHandler;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantData;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.EnderDragonVarintDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.HorseVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.TropicalFishVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.VillagerVariantDataFactory;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.EmptySubmitNodeCollector;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.animal.fish.CodModel;
import net.minecraft.client.model.animal.fish.SalmonModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.model.animal.llama.LlamaModel;
import net.minecraft.client.model.geom.ModelPart;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.joml.Matrix4fStack;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EntityMapImageManager {
    public static final Identifier resourceTextureAtlasMarker = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "atlas/mobs");
    private final RadarSettingsManager radarOptions;
    private final TextureAtlas textureAtlas;
    private final Minecraft minecraft = Minecraft.getInstance();

    private final HashMap<EntityType<?>, EntityVariantDataFactory> variantDataFactories = new HashMap<>();
    private final HashMap<EntityType<?>, AbstractArmorHandler> armorHandlers = new HashMap<>();
    private final DefaultArmorHandler defaultArmorHandler = new DefaultArmorHandler();
    private final PoseStack emptyPoseStack = new PoseStack();
    private final EmptySubmitNodeCollector emptySubmitNodeCollector = new EmptySubmitNodeCollector();
    private final Class<?>[] fullRenderModels;
    private final HashMap<EntityType<?>, Properties> customMobProperties = new HashMap<>();

    private int imageCreationRequests;
    private int fulfilledImageCreationRequests;
    private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    private final EntityGPURenderer gpuRenderer = new EntityGPURenderer();
    private final EntityCPURenderer cpuRenderer = new EntityCPURenderer();
    private boolean cpuRendering = false;
    private boolean lastCpuRendering = false;

    public EntityMapImageManager() {
        this.radarOptions = VoxelConstants.getVoxelMapInstance().getRadarOptions();

        this.textureAtlas = new TextureAtlas("mobsmap", resourceTextureAtlasMarker);
        this.textureAtlas.setFilter(true, false);

        this.fullRenderModels = new Class[] { CodModel.class, MagmaCubeModel.class, SalmonModel.class, SlimeModel.class, TropicalFishSmallModel.class, TropicalFishLargeModel.class };
        reset();
    }

    public void reset() {
        debugInfo("EntityMapImageManager: Resetting");

        this.textureAtlas.reset();
        this.textureAtlas.registerIconForBufferedImage("hostile", ImageUtils.loadImage(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/radar/hostile.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.registerIconForBufferedImage("neutral", ImageUtils.loadImage(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/radar/neutral.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.registerIconForBufferedImage("tame", ImageUtils.loadImage(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/radar/tame.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.stitch();

        variantDataFactories.clear();
        customMobProperties.clear();

        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.BOGGED, Identifier.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png"), null, null));
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.DROWNED, Identifier.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png"), null, null));
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.ENDERMAN, Identifier.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png"), null, null));
        addVariantDataFactory(new HorseVariantDataFactory(EntityType.HORSE));
        addVariantDataFactory(new EnderDragonVarintDataFactory(EntityType.ENDER_DRAGON));
        addVariantDataFactory(new VillagerVariantDataFactory(EntityType.VILLAGER));
        addVariantDataFactory(new VillagerVariantDataFactory(EntityType.ZOMBIE_VILLAGER));
        addVariantDataFactory(new TropicalFishVariantDataFactory(EntityType.TROPICAL_FISH));

        addArmorHandler(EntityType.SHEEP, new SheepOverlayHandler());

        if (VoxelConstants.DEBUG) {
            BuiltInRegistries.ENTITY_TYPE.forEach(t -> {
                requestImageForMobType(t, 32, true);
            });
        }
    }

    public Properties getCustomMobProperties(EntityType<?> type) {
        if (customMobProperties.containsKey(type)) {
            return customMobProperties.get(type);
        }

        String entityId = type.getDescriptionId();
        Identifier filePath = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "configs/radar_icon/" + entityId + ".properties");
        Optional<Resource> resource = minecraft.getResourceManager().getResource(filePath);

        Properties properties = new Properties();
        if (resource.isPresent()) {
            try (InputStream is = resource.get().open()) {
                properties.load(is);
            } catch (IOException ignored) {
            }
        }
        customMobProperties.put(type, properties);

        return properties;
    }

    private Sprite tryCustomMobIcon(EntityType<?> type, boolean addBorder) {
        String entityId = type.getDescriptionId();
        String iconId = entityId + "-custom" + (addBorder ? "-outlined" : "");
        Sprite existing = textureAtlas.getAtlasSpriteIncludingYetToBeStitched(iconId);
        if (existing != null && existing != textureAtlas.getMissingImage()) {
            return existing;
        }

        Sprite sprite = textureAtlas.registerEmptyIcon(iconId);
        Identifier filePath = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "configs/radar_icon/" + entityId + ".png");
        Optional<Resource> resource = minecraft.getResourceManager().getResource(filePath);

        BufferedImage image = null;
        if (resource.isPresent()) {
            try (InputStream is = resource.get().open()) {
                image = ImageIO.read(is);
            } catch (IOException ignored) {
            }
        }

        if (image != null) {
            image = ImageUtils.validateImage(image);
            image = ImageUtils.fillOutline(ImageUtils.pad(image), addBorder, 2);
            sprite.setTextureData(ImageUtils.nativeImageFromBufferedImage(image));
        }

        return sprite;
    }

    private AbstractEntityRenderer getEntityRenderer() {
        return cpuRendering ? cpuRenderer : gpuRenderer;
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
        Sprite customIcon = tryCustomMobIcon(entity.getType(), addBorder);
        if (customIcon != null && customIcon.getTextureData() != null && customIcon != textureAtlas.getMissingImage()) {
            return customIcon;
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
        Properties iconConfig = getCustomMobProperties(entity.getType());

        AbstractEntityRenderer renderer = getEntityRenderer();
        renderer.setup(iconConfig);
        renderer.enableCull(false);

        EntityRenderState renderState = ((EntityRenderer) baseRenderer).createRenderState(entity, 0.5F);
        ((EntityRenderer) baseRenderer).submit(renderState, emptyPoseStack, emptySubmitNodeCollector, minecraft.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState);

        EntityModel model = getEntityModel(baseRenderer);
        if (model == null) {
            return null;
        }
        model.resetPose();

        for (ModelPart part : getPartToRender(model)) {
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;

            renderer.addMesh(part);
        }

        if (baseRenderer instanceof SlimeRenderer slimeRenderer) {
            SlimeOuterLayer slimeOuter = (SlimeOuterLayer) slimeRenderer.layers.getFirst();
            renderer.addMesh(slimeOuter.model.root());
        }

        AbstractEntityRenderer.TextureSet textureSet = new AbstractEntityRenderer.TextureSet(
                variant.getPrimaryTexture(), getPrimaryTextureColor(entity),
                variant.getSecondaryTexture(), getSecondaryTextureColor(entity),
                variant.getTertiaryTexture(), getTertiaryTextureColor(entity),
                variant.getQuaternaryTexture(), getQuaternaryTextureColor(entity)
        );

        float iconScale = Float.parseFloat(iconConfig.getProperty("scale", "1.0"));
        renderer.render(textureSet, (output) -> {
            postProcessRenderedMobImage(entity, sprite, model, output, addBorder, iconScale);
        });

        return sprite;
    }

    private int getPrimaryTextureColor(Entity entity) {
        if (entity instanceof TropicalFish tropicalFish) {
            return tropicalFish.getBaseColor().getMapColor().col | 0xFF000000;
        }

        return 0xFFFFFFFF;
    }

    private int getSecondaryTextureColor(Entity entity) {
        if (entity instanceof TropicalFish tropicalFish) {
            return tropicalFish.getPatternColor().getMapColor().col | 0xFF000000;
        }

        return 0xFFFFFFFF;
    }

    private int getTertiaryTextureColor(Entity entity) {
        return 0xFFFFFFFF;
    }

    private int getQuaternaryTextureColor(Entity entity) {
        return 0xFFFFFFFF;
    }

    private int getMobIdentifier(Entity entity) {
        int id = 0;

        // Unique properties
        switch (entity) {
            case Pufferfish pufferfish -> id = pufferfish.getPuffState() & 0x7;
            case TropicalFish tropicalFish -> {
                id = tropicalFish.getBaseColor().getId() & 0xF;
                id |= (tropicalFish.getPatternColor().getId() & 0xF) << 4;
            }
            default -> {}
        }

        // Common properties
        if (entity instanceof LivingEntity livingEntity && livingEntity.isBaby()) {
            id |= (1 << 8);
        }

        int intScale = (int) Mth.clamp(getUniqueMobScale(entity) * 10.0F, 0.0F, 100.0F);
        id |= (intScale & 0x3FF) << 9;

        return id;
    }

    private float getUniqueMobScale(Entity entity) {
        float scale = 1.0F;
        if (entity instanceof Salmon salmon) {
            scale *= salmon.getSalmonScale();
        }

        return scale;
    }

    private void postProcessRenderedMobImage(Entity entity, Sprite sprite, @SuppressWarnings("rawtypes") EntityModel model, BufferedImage image2, boolean addBorder, float scale) {
        Util.backgroundExecutor().execute(() -> {
            BufferedImage image = image2;

            switch (model) {
                case CamelModel camelModel -> {
                    Graphics2D g = image.createGraphics();
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(0, 192, image.getWidth(), image.getHeight());
                    g.dispose();
                }
                case LlamaModel llamaModel -> {
                    Graphics2D g = image.createGraphics();
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(0, 248, image.getWidth(), image.getHeight());
                    g.dispose();
                }
                case HappyGhastModel happyGhastModel -> {
                    Graphics2D g = image.createGraphics();
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(0,  352, image.getWidth(), image.getHeight());
                    g.dispose();
                }
                default -> {}
            }

            float uniqueMobScale = getUniqueMobScale(entity);
            image = ImageUtils.trim(image);
            image = ImageUtils.scaleImage(image, scale / uniqueMobScale);
            image = ImageUtils.fillOutline(ImageUtils.pad(image), addBorder, 2);

            addToCreationTask(sprite, image, entity.getType().getDescriptionId());
        });
    }

    private void addArmorHandler(EntityType<?> type, AbstractArmorHandler handler) {
        armorHandlers.put(type, handler);
    }

    private AbstractArmorHandler getArmorHandler(EntityType<?> type) {
        AbstractArmorHandler armorHandler = armorHandlers.get(type);
        if (armorHandler != null) {
            return armorHandler;
        }

        return defaultArmorHandler;
    }

    private AbstractArmorHandler getAndSetupArmorHandler(Entity entity, EntityRenderer<?, ?> renderer, int size, boolean addBorder) {
        AbstractArmorHandler armorHandler = getArmorHandler(entity.getType());
        armorHandler.setupForEntity(entity, renderer, size, addBorder);

        return armorHandler;
    }

    public Sprite requestImageForArmor(Entity entity, int size, boolean addBorder) {
        EntityRenderer<?, ?> entityRenderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);

        AbstractArmorHandler armorHandler = getAndSetupArmorHandler(entity, entityRenderer, size, addBorder);
        EntityArmorData armorData = armorHandler.getArmorData();
        if (armorData == null) {
            return null;
        }

        Sprite existing = textureAtlas.getAtlasSpriteIncludingYetToBeStitched(armorData);
        if (existing != null && existing != textureAtlas.getMissingImage()) {
            return existing;
        }
        Sprite sprite = textureAtlas.registerEmptyIcon(armorData);
        Properties iconConfig = getCustomMobProperties(entity.getType());

        AbstractEntityRenderer renderer = getEntityRenderer();
        renderer.setup(iconConfig);
        renderer.enableCull(true);

        armorHandler.renderArmorModel(renderer);

        AbstractEntityRenderer.TextureSet textureSet = new AbstractEntityRenderer.TextureSet(armorData.getTexture(), 0xFFFFFFFF, null, -1, null, -1, null, -1);

        float iconScale = Float.parseFloat(iconConfig.getProperty("scale", "1.0"));
        renderer.render(textureSet, (output) -> {
            postProcessRenderedArmorImage(sprite, output, armorHandler, armorData, iconScale);
        });

        return sprite;
    }

    private void postProcessRenderedArmorImage(Sprite sprite, BufferedImage image2, AbstractArmorHandler armorHandler, EntityArmorData armorData, float scale) {
        Util.backgroundExecutor().execute(() -> {
            BufferedImage image = image2;

            image = ImageUtils.flipHorizontal(image);
            image = armorHandler.postProcessTexture(image, armorData);
            image = ImageUtils.scaleImage(image, scale);

            addToCreationTask(sprite, image, sprite.getIconName().toString());
        });
    }

    private void addToCreationTask(Sprite sprite, BufferedImage image, String debugId) {
        imageCreationRequests++;

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

    public void onRenderTick(Matrix4fStack matrixStack) {
        Runnable task;
        while ((task = taskQueue.poll()) != null) {
            task.run();
        }

        if ((cpuRendering = radarOptions.cpuRendering || radarOptions.forceCpuRendering) != lastCpuRendering) {
            reset();
            lastCpuRendering = cpuRendering;
        }
    }

    private void debugInfo(String str) {
        if (VoxelConstants.DEBUG) {
            VoxelConstants.getLogger().info(str);
        }
    }
}
