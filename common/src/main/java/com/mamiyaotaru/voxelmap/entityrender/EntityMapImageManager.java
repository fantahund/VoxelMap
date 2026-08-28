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
import com.mamiyaotaru.voxelmap.rendering.EmptySubmitNodeCollector;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.fish.CodModel;
import net.minecraft.client.model.animal.fish.SalmonModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.ghast.GhastModel;
import net.minecraft.client.model.monster.slime.MagmaCubeModel;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.model.monster.wither.WitherBossModel;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import org.joml.Matrix4fStack;

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
    private final HashSet<EntityType<?>> failedPreviewIconTypes = new HashSet<>();
    private final Set<EntityType<?>> warnedEmptyIconTypes = ConcurrentHashMap.newKeySet();
    private final AtomicInteger previewEntityIds = new AtomicInteger(-1);
    private final EmfAnimationCompat emfAnimations = EmfAnimationCompat.INSTANCE;

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

        this.fullRenderModels = new Class[] { CodModel.class, MagmaCubeModel.class, SalmonModel.class, SlimeModel.class, SulfurCube.class, TropicalFishSmallModel.class, TropicalFishLargeModel.class };
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
        failedPreviewIconTypes.clear();
        warnedEmptyIconTypes.clear();
        previewEntityIds.set(-1);

        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityTypes.BOGGED, Identifier.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png"), null, null));
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityTypes.DROWNED, Identifier.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png"), null, null));
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityTypes.ENDERMAN, Identifier.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png"), null, null));
        addVariantDataFactory(new HorseVariantDataFactory(EntityTypes.HORSE));
        addVariantDataFactory(new EnderDragonVarintDataFactory(EntityTypes.ENDER_DRAGON));
        addVariantDataFactory(new VillagerVariantDataFactory(EntityTypes.VILLAGER));
        addVariantDataFactory(new VillagerVariantDataFactory(EntityTypes.ZOMBIE_VILLAGER));
        addVariantDataFactory(new TropicalFishVariantDataFactory(EntityTypes.TROPICAL_FISH));

        addArmorHandler(EntityTypes.SHEEP, new SheepOverlayHandler());

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
        return shouldUseCpuRendering() ? cpuRenderer : gpuRenderer;
    }

    private boolean shouldUseCpuRendering() {
        return radarOptions.cpuRendering || radarOptions.forceCpuRendering;
    }

    private void addVariantDataFactory(EntityVariantDataFactory factory) {
        variantDataFactories.put(factory.getType(), factory);
    }

    public Sprite requestImageForMobType(EntityType<?> type, boolean addBorder) {
        return requestImageForMobType(type, -1, addBorder);
    }

    public Sprite requestImageForMobType(EntityType<?> type, int size, boolean addBorder) {
        if (failedPreviewIconTypes.contains(type)) {
            return null;
        }

        LivingEntity previewEntity = createPreviewEntity(type);
        if (previewEntity == null) {
            return null;
        }

        try {
            return requestImageForMob(previewEntity, size, addBorder);
        } catch (RuntimeException e) {
            failedPreviewIconTypes.add(type);
            VoxelConstants.getLogger().warn("Failed to render radar preview icon for mob type {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), e);
            return null;
        }
    }

    private LivingEntity createPreviewEntity(EntityType<?> type) {
        if (minecraft.level != null && type.create(minecraft.level, EntitySpawnReason.LOAD) instanceof LivingEntity livingEntity) {
            livingEntity.setId(previewEntityIds.getAndDecrement());
            return livingEntity;
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

        EntityRenderState renderState = ((EntityRenderer) baseRenderer).createRenderState(entity, 0.5F);
        EntityModel model = getEntityModel(baseRenderer);
        if (model == null) {
            return null;
        }

        PreparedRendering preparedRendering;
        RadarModelPose originalPose = RadarModelPose.capture(model.root());
        try {
            submitEntity(baseRenderer, renderState);
            preparedRendering = prepareRendering(model);
        } finally {
            originalPose.apply();
        }

        SlimeOverlay slimeOverlay = prepareSlimeOverlay(baseRenderer);

        AbstractEntityRenderer.TextureSet textureSet = new AbstractEntityRenderer.TextureSet(
                variant.getPrimaryTexture(), getPrimaryTextureColor(entity),
                variant.getSecondaryTexture(), getSecondaryTextureColor(entity),
                variant.getTertiaryTexture(), getTertiaryTextureColor(entity),
                variant.getQuaternaryTexture(), getQuaternaryTextureColor(entity)
        );

        float iconScale = Float.parseFloat(iconConfig.getProperty("scale", "1.0"));
        renderMobAttempt(entity, sprite, baseRenderer, renderState, model,
                preparedRendering.renderAttempts(), preparedRendering.preparedPoses(),
                slimeOverlay, textureSet, iconConfig, addBorder, iconScale, 0);

        return sprite;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void submitEntity(EntityRenderer baseRenderer, EntityRenderState renderState) {
        baseRenderer.submit(renderState, emptyPoseStack, emptySubmitNodeCollector,
                minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState);
    }

    private PreparedRendering prepareRendering(EntityModel<?> model) {
        RadarModelPose submittedPose = RadarModelPose.capture(model.root());
        RadarModelPose initializedDefaultPose = null;
        boolean neutralize = false;

        try {
            EmfAnimationCompat.CustomizationState customizationState = emfAnimations.customizationState(model);
            RadarModelPose assembledPose;
            if (customizationState == EmfAnimationCompat.CustomizationState.CUSTOMIZED
                    || customizationState == EmfAnimationCompat.CustomizationState.UNKNOWN) {
                boolean primed = emfAnimations.primeAnimations(model);
                neutralize = primed && emfAnimations.animationState(model) == EmfAnimationCompat.AnimationState.ANIMATED;
                assembledPose = RadarModelPose.capture(model.root());

                // Lazy EMF initialization may add custom parts. Restore new parts to their JEM defaults later,
                // while submittedPose restores all parts that already existed before initialization.
                RadarModelPose.resetToInitial(model.root());
                initializedDefaultPose = RadarModelPose.capture(model.root());
                assembledPose.apply();
            } else {
                RadarModelPose.resetToInitial(model.root());
                assembledPose = RadarModelPose.capture(model.root());
                initializedDefaultPose = assembledPose;
            }

            // EMF swaps the vanilla wrapper contents lazily, so candidates must be resolved after priming.
            List<RenderAttempt> renderAttempts = getRenderAttempts(model);
            ArrayList<RadarModelPose> poses = new ArrayList<>(renderAttempts.size());
            for (RenderAttempt attempt : renderAttempts) {
                assembledPose.apply();
                if (neutralize) {
                    RadarModelPose.normalizeOrientations(attempt.selections());
                }
                poses.add(RadarModelPose.capture(model.root()));
            }

            return new PreparedRendering(renderAttempts, new PreparedPoses(poses, neutralize));
        } finally {
            if (initializedDefaultPose != null) {
                initializedDefaultPose.apply();
            }
            submittedPose.apply();
        }
    }

    private SlimeOverlay prepareSlimeOverlay(EntityRenderer<?, ?> baseRenderer) {
        if (!(baseRenderer instanceof SlimeRenderer slimeRenderer)) {
            return null;
        }

        SlimeOuterLayer slimeOuter = (SlimeOuterLayer) slimeRenderer.layers.getFirst();
        ModelPart root = slimeOuter.model.root();
        RadarModelPose originalPose = RadarModelPose.capture(root);
        try {
            slimeOuter.model.resetPose();
            return new SlimeOverlay(root, RadarModelPose.capture(root));
        } finally {
            originalPose.apply();
        }
    }

    @SuppressWarnings("rawtypes")
    private void renderMobAttempt(Entity entity, Sprite sprite, EntityRenderer baseRenderer, EntityRenderState renderState,
                                  EntityModel<?> model, List<RenderAttempt> renderAttempts, PreparedPoses preparedPoses,
                                  SlimeOverlay slimeOverlay, AbstractEntityRenderer.TextureSet textureSet, Properties iconConfig,
                                  boolean addBorder, float iconScale, int attemptIndex) {
        RenderAttempt attempt = renderAttempts.get(attemptIndex);
        AbstractEntityRenderer renderer = getEntityRenderer();
        RadarModelPose currentPose = RadarModelPose.capture(model.root());
        RadarModelPose currentSlimePose = slimeOverlay == null ? null : RadarModelPose.capture(slimeOverlay.root());

        try {
            submitEntity(baseRenderer, renderState);
            preparedPoses.poses().get(attemptIndex).apply();
            if (slimeOverlay != null) {
                slimeOverlay.preparedPose().apply();
            }

            renderer.setup(iconConfig);
            renderer.enableCull(false);
            for (RadarModelPartResolver.Selection selection : attempt.selections()) {
                renderer.addMesh(selection.part(), selection.ancestors(), selection.includeChildren());
            }
            if (slimeOverlay != null) {
                renderer.addMesh(slimeOverlay.root());
            }

            try (EmfAnimationCompat.PauseScope ignored = emfAnimations.pause(entity, preparedPoses.pauseEmf())) {
                renderer.render(textureSet, output -> {
                    RadarIconFallback.Decision fallbackDecision = RadarIconFallback.decide(hasVisiblePixel(output), attemptIndex, renderAttempts.size());
                    if (fallbackDecision == RadarIconFallback.Decision.TRY_NEXT) {
                        renderMobAttempt(entity, sprite, baseRenderer, renderState, model, renderAttempts, preparedPoses,
                                slimeOverlay, textureSet, iconConfig, addBorder, iconScale, attemptIndex + 1);
                        return;
                    }

                    if (RadarIconFallback.shouldWarn(fallbackDecision, warnedEmptyIconTypes, entity.getType())) {
                        String attemptedPaths = renderAttempts.stream().map(RenderAttempt::description).reduce((left, right) -> left + " -> " + right).orElse("root");
                        VoxelConstants.getLogger().warn(
                                "Rendered an empty radar icon for mob type {} using model {} after model part fallbacks [{}]",
                                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), model.getClass().getName(), attemptedPaths);
                    }

                    postProcessRenderedMobImage(entity, sprite, output, addBorder, iconScale);
                });
            }
        } finally {
            currentPose.apply();
            if (currentSlimePose != null) {
                currentSlimePose.apply();
            }
        }
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

    private void postProcessRenderedMobImage(Entity entity, Sprite sprite, BufferedImage image2, boolean addBorder, float scale) {
        Util.backgroundExecutor().execute(() -> {
            BufferedImage image = image2;

            float uniqueMobScale = getUniqueMobScale(entity);
            image = ImageUtils.trim(image);
            image = ImageUtils.scaleImage(image, scale / uniqueMobScale);
            image = ImageUtils.fillOutline(ImageUtils.pad(image), addBorder, 2);

            addToCreationTask(sprite, image, entity.getType().getDescriptionId());
        });
    }

    private static boolean hasVisiblePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return true;
                }
            }
        }

        return false;
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

    private List<RenderAttempt> getRenderAttempts(EntityModel<?> model) {
        ModelPart root = model.root();
        ArrayList<RenderAttempt> attempts = new ArrayList<>();

        // full-model rendered mobs
        for (Class<?> clazz : fullRenderModels) {
            if (clazz.isInstance(model)) {
                return List.of(new RenderAttempt(List.of(RadarModelPartResolver.root(root))));
            }
        }

        // The face is on the main body cube. Long animated tentacles make the icon excessively large.
        if (model instanceof GhastModel || model instanceof HappyGhastModel) {
            RadarModelPartResolver.find(root, "body")
                    .flatMap(RadarModelPartResolver::largestDirectGeometry)
                    .ifPresent(selection -> attempts.add(new RenderAttempt(List.of(selection))));
            attempts.add(new RenderAttempt(List.of(RadarModelPartResolver.root(root))));
            return List.copyOf(attempts);
        }

        // wither
        if (model instanceof WitherBossModel) {
            ArrayList<RadarModelPartResolver.Selection> heads = new ArrayList<>();
            for (String name : List.of("left_head", "center_head", "right_head")) {
                RadarModelPartResolver.find(root, name).ifPresent(heads::add);
            }
            if (!heads.isEmpty()) {
                attempts.add(new RenderAttempt(heads));
                attempts.add(new RenderAttempt(List.of(RadarModelPartResolver.root(root))));
                return List.copyOf(attempts);
            }
        }

        List<RadarModelPartResolver.Selection> headCandidates = RadarModelPartResolver.resolveHeadCandidates(root);
        for (RadarModelPartResolver.Selection selection : headCandidates) {
            Optional<RadarModelPartResolver.Selection> spiderBody = selection.name().equalsIgnoreCase("head")
                    ? RadarModelPartResolver.findSibling(selection, "body0")
                    : Optional.empty();
            attempts.add(new RenderAttempt(spiderBody
                    .map(body -> List.of(selection, body))
                    .orElseGet(() -> List.of(selection))));
        }

        if (attempts.isEmpty()) {
            RadarModelPartResolver.find(root, "body").ifPresent(selection -> attempts.add(new RenderAttempt(List.of(selection))));
            RadarModelPartResolver.find(root, "cube").ifPresent(selection -> attempts.add(new RenderAttempt(List.of(selection))));

            Optional<RadarModelPartResolver.Selection> segment0 = RadarModelPartResolver.find(root, "segment0");
            Optional<RadarModelPartResolver.Selection> segment1 = RadarModelPartResolver.find(root, "segment1");
            if (segment0.isPresent() && segment1.isPresent()) {
                attempts.add(new RenderAttempt(List.of(segment0.get(), segment1.get())));
            }
        }

        attempts.add(new RenderAttempt(List.of(RadarModelPartResolver.root(root))));
        return List.copyOf(attempts);
    }

    private record RenderAttempt(List<RadarModelPartResolver.Selection> selections) {
        RenderAttempt {
            selections = List.copyOf(selections);
        }

        String description() {
            return selections.stream().map(RadarModelPartResolver.Selection::path).reduce((left, right) -> left + ", " + right).orElse("root");
        }
    }

    private record PreparedPoses(List<RadarModelPose> poses, boolean pauseEmf) {
        PreparedPoses {
            poses = List.copyOf(poses);
        }
    }

    private record PreparedRendering(List<RenderAttempt> renderAttempts, PreparedPoses preparedPoses) {
        PreparedRendering {
            renderAttempts = List.copyOf(renderAttempts);
        }
    }

    private record SlimeOverlay(ModelPart root, RadarModelPose preparedPose) {
    }

    public void onRenderTick(Matrix4fStack matrixStack) {
        Runnable task;
        while ((task = taskQueue.poll()) != null) {
            task.run();
        }

        if ((cpuRendering = shouldUseCpuRendering()) != lastCpuRendering) {
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
