package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.entityrender.armors.ArmorVariantData;
import com.mamiyaotaru.voxelmap.entityrender.armors.ArmorVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.EnderDragonVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.EntityVariantData;
import com.mamiyaotaru.voxelmap.entityrender.variants.EntityVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.HorseVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.TropicalFishVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.VillagerVariantDataFactory;
import com.mamiyaotaru.voxelmap.interfaces.IReloadListener;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.EmptySubmitNodeCollector;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapPipelines;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.animal.fish.CodModel;
import net.minecraft.client.model.animal.fish.SalmonModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.slime.MagmaCubeModel;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.model.monster.zombie.ZombieVillagerModel;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SkullBlock;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EntityMapImageManager implements IReloadListener {
    public static final Identifier resourceTextureAtlasMarker = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "atlas/mobs");
    private final Minecraft minecraft = Minecraft.getInstance();
    private final TextureAtlas textureAtlas;
    private final StringBuilder stringBuilder = new StringBuilder();

    private final HashMap<EntityType<?>, EntityVariantDataFactory> entityVariantDataFactories = new HashMap<>();
    private final HashMap<Item, ArmorVariantDataFactory> armorVariantDataFactories = new HashMap<>();
    private final Set<Class<?>> fullRenderModels;
    private final HashMap<EntityType<?>, Properties> customMobProperties = new HashMap<>();

    private final RandomSource randomSource = RandomSource.create();
    private final PoseStack emptyPoseStack = new PoseStack();
    private final EmptySubmitNodeCollector emptySubmitNodeCollector = new EmptySubmitNodeCollector();
    private final HumanoidModel<?> humanoidModel;
    private final EntityImageRenderer renderer = new EntityImageRenderer();

    private int totalSpriteCreations;
    private int doneSpriteCreations;
    private final ConcurrentLinkedQueue<Runnable> spriteCreationTask = new ConcurrentLinkedQueue<>();

    public EntityMapImageManager() {
        this.textureAtlas = new TextureAtlas("mobsmap", resourceTextureAtlasMarker);
        this.textureAtlas.setFilter(true, false);

        this.fullRenderModels = Set.of(CodModel.class, MagmaCubeModel.class, SalmonModel.class, SlimeModel.class, TropicalFishSmallModel.class, TropicalFishLargeModel.class);

        CubeDeformation armorInflate = new CubeDeformation(1.0F);
        LayerDefinition layerDefinition = LayerDefinition.create(HumanoidModel.createMesh(armorInflate, 0.0F), 64, 32);
        this.humanoidModel = new HumanoidModel<>(layerDefinition.bakeRoot());

        VoxelConstants.getVoxelMapInstance().addReloadListener(this);
    }

    public void reset() {
        this.textureAtlas.reset();
        this.textureAtlas.registerIconForBufferedImage("hostile", ImageUtils.loadImage(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/radar/hostile.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.registerIconForBufferedImage("neutral", ImageUtils.loadImage(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/radar/neutral.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.registerIconForBufferedImage("tame", ImageUtils.loadImage(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/radar/tame.png"), 0, 0, 16, 16, 16, 16));
        this.textureAtlas.stitch();

        entityVariantDataFactories.clear();
        customMobProperties.clear();

        addVariantDataFactory(new EntityVariantDataFactory(EntityType.BOGGED, Identifier.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png"), 0xFFFFFFFF));
        addVariantDataFactory(new EntityVariantDataFactory(EntityType.DROWNED, Identifier.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png"), 0xFFFFFFFF));
        addVariantDataFactory(new EntityVariantDataFactory(EntityType.ENDERMAN, Identifier.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png"), 0xFFFFFFFF));
        addVariantDataFactory(new EnderDragonVariantDataFactory(EntityType.ENDER_DRAGON));
        addVariantDataFactory(new HorseVariantDataFactory(EntityType.HORSE));
        addVariantDataFactory(new TropicalFishVariantDataFactory(EntityType.TROPICAL_FISH));
        addVariantDataFactory(new VillagerVariantDataFactory(EntityType.VILLAGER));
        addVariantDataFactory(new VillagerVariantDataFactory(EntityType.ZOMBIE_VILLAGER));

        addVariantDataFactory(new ArmorVariantDataFactory(Items.LEATHER_HELMET, Identifier.withDefaultNamespace("textures/entity/equipment/humanoid/leather_overlay.png"), 0xFFFFFFFF));

        runOnDebug(() -> {
            VoxelConstants.getLogger().info("EntityMapImageManager: Resetting");
            BuiltInRegistries.ENTITY_TYPE.forEach(t -> requestImageForMobType(t, 32, true));
        });
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        reset();
    }

    // Mob image

    private void addVariantDataFactory(EntityVariantDataFactory factory) {
        entityVariantDataFactories.put(factory.getType(), factory);
    }

    private String getMobIdentifier(Entity entity) {
        stringBuilder.setLength(0);

        // Common properties
        stringBuilder.append(",scale:").append(getUniqueMobScale(entity));
        stringBuilder.append(",isBaby:").append(entity instanceof LivingEntity le && le.isBaby());

        // Unique properties
        if (entity instanceof Pufferfish pufferfish) {
            stringBuilder.append(",puffState:").append(pufferfish.getPuffState());
        }

        return stringBuilder.deleteCharAt(0).toString();
    }

    private float getUniqueMobScale(Entity entity) {
        float scale = 1.0F;
        if (entity instanceof Salmon salmon) {
            scale *= salmon.getSalmonScale();
        }

        return scale;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private VariantDataHolder getOrCreateVariantData(Entity entity, EntityRenderer renderer, int size, boolean addBorder) {
        if (entity instanceof AbstractClientPlayer player) {
            return new EntityVariantData(entity.getType(), "", player.getSkin().body().texturePath(), 0xFFFFFFFF, size, addBorder);
        }

        EntityRenderState renderState = renderer.createRenderState(entity, 0.5F);
        String id = getMobIdentifier(entity);

        EntityVariantDataFactory factory = entityVariantDataFactories.get(entity.getType());
        if (factory != null) {
            EntityVariantData data = factory.create(entity, renderer, renderState, id, size, addBorder);
            if (data != null) {
                return data;
            }
        }
        return EntityVariantDataFactory.createSimple(entity, renderer, renderState, id, size, addBorder);
    }

    public Sprite requestImageForMobType(EntityType<?> type, boolean addBorder) {
        return requestImageForMobType(type, -1, addBorder);
    }

    public Sprite requestImageForMobType(EntityType<?> type, int size, boolean addBorder) {
        if (minecraft.level != null && type.create(minecraft.level, EntitySpawnReason.LOAD) instanceof LivingEntity entity) {
            return requestImageForMob(entity, size, addBorder);
        }
        return null;
    }

    public Sprite requestImageForMob(Entity entity, boolean addBorder) {
        return requestImageForMob(entity, -1, addBorder);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Sprite requestImageForMob(Entity entity, int size, boolean addBorder) {
        Sprite customIcon = tryCustomMobIcon(entity.getType(), addBorder);
        if (customIcon != null && customIcon.getTextureData() != null && customIcon != textureAtlas.getMissingImage()) {
            return customIcon;
        }

        EntityRenderer<?, ?> baseRenderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        VariantDataHolder dataHolder = getOrCreateVariantData(entity, baseRenderer, size, addBorder);

        Sprite existing = textureAtlas.getAtlasSpriteIncludingYetToBeStitched(dataHolder);
        if (existing != null && existing != textureAtlas.getMissingImage()) {
            return existing;
        }
        runOnDebug(() -> VoxelConstants.getLogger().info("EntityMapImageManager: Rendering Mob of type {}", dataHolder.getName()));

        Sprite sprite = textureAtlas.registerEmptyIcon(dataHolder);

        renderer.setup(1.0F / getUniqueMobScale(entity), getCustomMobProperties(entity.getType()));
        renderer.beginBatch(VoxelMapPipelines.ENTITY_ICON, dataHolder);

        EntityRenderState renderState = ((EntityRenderer) baseRenderer).createRenderState(entity, 0.5F);
        ((EntityRenderer) baseRenderer).submit(renderState, emptyPoseStack, emptySubmitNodeCollector, minecraft.gameRenderer.getLevelRenderState().cameraRenderState);

        ModelPart[] modelParts = getPartToRender(baseRenderer);
        if (modelParts != null) {
            for (ModelPart part : modelParts) {
                part.xRot = 0;
                part.yRot = 0;
                part.zRot = 0;

                part.render(renderer.pose(), renderer.vertexBuffer(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
        }

        if (baseRenderer instanceof SlimeRenderer slimeRenderer) {
            ((SlimeOuterLayer) slimeRenderer.layers.getFirst()).model.root().render(renderer.pose(), renderer.vertexBuffer(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }

        BufferedImage output = renderer.endBatch();
        postProcessRenderedMobImage(entity, sprite, output, addBorder);

        return sprite;
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
        String iconId = entityId + "(custom" + (addBorder ? ",outlined)" : ")");
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

    private void postProcessRenderedMobImage(Entity entity, Sprite sprite, BufferedImage image2, boolean addBorder) {
        Util.backgroundExecutor().execute(() -> {
            BufferedImage image = image2;

            Graphics2D graphics = image.createGraphics();
            if (entity instanceof Camel) {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 192, image.getWidth(), image.getHeight());
            } else if (entity instanceof Llama) {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 248, image.getWidth(), image.getHeight());
            } else if (entity instanceof HappyGhast) {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 352, image.getWidth(), image.getHeight());
            }
            graphics.dispose();

            image = ImageUtils.trim(image);
            image = ImageUtils.fillOutline(ImageUtils.pad(image), addBorder, 2);

            addSpriteCreationTask(sprite, image);
        });
    }

    @SuppressWarnings("rawtypes")
    private ModelPart[] getPartToRender(EntityRenderer renderer) {
        EntityModel<?> model;
        if (renderer instanceof LivingEntityRenderer renderer2) {
            model = renderer2.getModel();
        } else if (renderer instanceof EnderDragonRenderer renderer2) {
            model = renderer2.model;
        } else {
            return null;
        }
        model.resetPose();

        // full-model rendered mobs
        if (fullRenderModels.contains(model.getClass())) {
            return new ModelPart[]{model.root()};
        }

        // wither
        if (model instanceof WitherBossModel witherModel) {
            return new ModelPart[]{witherModel.root().getChild("left_head"), witherModel.root().getChild("center_head"), witherModel.root().getChild("right_head")};
        }

        // villager
        if (model instanceof VillagerModel villagerModel) {
            return new ModelPart[]{villagerModel.root().getChild("head"), villagerModel.root().getChild("head").getChild("hat")};
        }
        if (model instanceof ZombieVillagerModel<?> zombieVillagerModel) {
            return new ModelPart[]{zombieVillagerModel.root().getChild("head"), zombieVillagerModel.root().getChild("head").getChild("hat")};
        }

        // horses
        for (ModelPart part : model.allParts()) {
            if (part.hasChild("head_parts")) {
                return new ModelPart[]{part.getChild("head_parts")};
            }
        }

        // most mobs
        for (ModelPart part : model.allParts()) {
            if (part.hasChild("head")) {
                if (part.hasChild("body0")) {
                    // spider
                    return new ModelPart[]{part.getChild("head"), part.getChild("body0")};
                }
                return new ModelPart[]{part.getChild("head")};
            }
        }

        // bee, ghast
        for (ModelPart part : model.allParts()) {
            if (part.hasChild("body")) {
                return new ModelPart[]{part.getChild("body")};
            }
        }

        // bee, ghast, slime
        for (ModelPart part : model.allParts()) {
            if (part.hasChild("cube")) {
                return new ModelPart[]{part.getChild("cube")};
            }
        }

        // silverfish, endermite
        for (ModelPart part : model.allParts()) {
            if (part.hasChild("segment0")) {
                return new ModelPart[]{part.getChild("segment0"), part.getChild("segment1")};
            }
        }

        // fallback
        return new ModelPart[]{model.root()};
    }

    // Armor image

    private void addVariantDataFactory(ArmorVariantDataFactory factory) {
        armorVariantDataFactories.put(factory.getType(), factory);
    }

    private String getArmorIdentifier(ItemStack itemStack) {
        return "";
    }

    private VariantDataHolder getOrCreateVariantData(ItemStack itemStack, int size, boolean addBorder) {
        String id = getArmorIdentifier(itemStack);

        ArmorVariantDataFactory factory = armorVariantDataFactories.get(itemStack.getItem());
        if (factory != null) {
            ArmorVariantData data = factory.create(itemStack, id, size, addBorder);
            if (data != null) {
                return data;
            }
        }
        return ArmorVariantDataFactory.createSimple(itemStack, id, size, addBorder);
    }

    public Sprite requestImageForArmor(Entity entity, int size, boolean addBorder) {
        ItemStack itemStack;
        if (!(entity instanceof LivingEntity livingEntity) || (itemStack = livingEntity.getItemBySlot(EquipmentSlot.HEAD)).isEmpty()) {
            return null;
        }

        VariantDataHolder dataHolder = getOrCreateVariantData(itemStack, size, addBorder);

        Sprite existing = textureAtlas.getAtlasSpriteIncludingYetToBeStitched(dataHolder);
        if (existing != null && existing != textureAtlas.getMissingImage()) {
            return existing;
        }
        Sprite sprite = textureAtlas.registerEmptyIcon(dataHolder);

        renderer.setup(1.0F, getCustomMobProperties(entity.getType()));
        renderer.beginBatch(VoxelMapPipelines.ENTITY_ICON_CULLED, dataHolder);

        if (itemStack.getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() instanceof SkullBlock skullBlock) {
                SkullModelBase skullModel = SkullBlockRenderer.createModel(EntityModelSet.vanilla(), skullBlock.getType());
                renderer.pose().scale(1.1875F, 1.1875F, 1.1875F);
                skullModel.renderToBuffer(renderer.pose(), renderer.vertexBuffer(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } else {
                BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
                renderer.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
                renderer.pose().scale(0.625F, 0.625F, 0.625F);
                List<BlockModelPart> allQuads = blockRenderer.getBlockModel(blockItem.getBlock().defaultBlockState()).collectParts(randomSource);
                blockRenderer.getModelRenderer().tesselateBlock(minecraft.level, allQuads, blockItem.getBlock().defaultBlockState(), BlockPos.ZERO, renderer.pose(), renderer.vertexBuffer(), true, OverlayTexture.NO_OVERLAY);
            }
        } else if (itemStack.get(DataComponents.EQUIPPABLE) != null) {
            ModelPart part = humanoidModel.root().getChild("head");
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            part.render(renderer.pose(), renderer.vertexBuffer(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }

        BufferedImage output = renderer.endBatch();
        postProcessRenderedArmorImage(itemStack, sprite, output, addBorder);

        return sprite;
    }

    private void postProcessRenderedArmorImage(ItemStack itemStack, Sprite sprite, BufferedImage image2, boolean addBorder) {
        Util.backgroundExecutor().execute(() -> {
            BufferedImage image = image2;

            image = ImageUtils.trim(image);
            boolean isHelmetItem = itemStack.get(DataComponents.EQUIPPABLE) != null && !(itemStack.getItem() instanceof BlockItem);
            if (isHelmetItem) {
                // Top align the helmet image
                BufferedImage canvas = new BufferedImage(image.getWidth(), image.getWidth(), image.getType());
                image = ImageUtils.addImages(canvas, image, 0, 0, image.getWidth(), image.getHeight());
            }
            image = ImageUtils.fillOutline(ImageUtils.pad(image), addBorder, true, 37.5F, 37.5F, 2);

            addSpriteCreationTask(sprite, image);
        });
    }

    private void addSpriteCreationTask(Sprite sprite, BufferedImage image) {
        totalSpriteCreations++;
        spriteCreationTask.add(() -> {
            doneSpriteCreations++;
            sprite.setTextureData(ImageUtils.nativeImageFromBufferedImage(image));
            runOnDebug(() -> VoxelConstants.getLogger().info("EntityMapImageManager: BufferedImage: ({} / {}) added to texture atlas {} ({} * {})", doneSpriteCreations, totalSpriteCreations, sprite.getIconName(), image.getWidth(), image.getHeight()));
            if (doneSpriteCreations == totalSpriteCreations) {
                textureAtlas.stitchNew();
                runOnDebug(() -> {
                    VoxelConstants.getLogger().info("EntityMapImageManager: Stitching!");
                    textureAtlas.saveDebugImage();
                });
            }
        });
    }

    public void onRenderTick() {
        Runnable task;
        while ((task = spriteCreationTask.poll()) != null) {
            task.run();
        }
    }

    private void runOnDebug(Runnable runnable) {
        if (VoxelConstants.DEBUG) {
            runnable.run();
        }
    }
}
