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
import com.mamiyaotaru.voxelmap.rendering.VoxelMapPipelines;
import com.mamiyaotaru.voxelmap.rendering.VoxelMapSamplers;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EntityMapImageManager {
    public static final Identifier resourceTextureAtlasMarker = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "atlas/mobs");
    private final Minecraft minecraft = Minecraft.getInstance();
    private final TextureAtlas textureAtlas;
    private final StringBuilder stringBuilder = new StringBuilder();

    private final HashMap<EntityType<?>, EntityVariantDataFactory> entityVariantDataFactories = new HashMap<>();
    private final HashMap<Item, ArmorVariantDataFactory> armorVariantDataFactories = new HashMap<>();
    private final HashMap<EntityType<?>, Properties> customMobProperties = new HashMap<>();

    private final EntityMeshBuilder meshBuilder = new EntityMeshBuilder();
    private final EntityMeshRenderer meshRenderer = new EntityMeshRenderer();

    private int totalSpriteCreations;
    private int doneSpriteCreations;
    private final ConcurrentLinkedQueue<Runnable> spriteCreationTask = new ConcurrentLinkedQueue<>();

    public EntityMapImageManager() {
        textureAtlas = new TextureAtlas("mobsmap", resourceTextureAtlasMarker);
    }

    public void reset() {
        if (VoxelConstants.DEBUG) VoxelConstants.getLogger().info("EntityMapImageManager: Resetting");

        textureAtlas.reset();
        textureAtlas.registerIconForBufferedImage("hostile", ImageUtils.loadImage(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/radar/hostile.png"), 0, 0, 16, 16, 16, 16));
        textureAtlas.registerIconForBufferedImage("neutral", ImageUtils.loadImage(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/radar/neutral.png"), 0, 0, 16, 16, 16, 16));
        textureAtlas.registerIconForBufferedImage("tame", ImageUtils.loadImage(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/radar/tame.png"), 0, 0, 16, 16, 16, 16));
        textureAtlas.stitch();

        boolean useFiltering = Boolean.parseBoolean(VoxelConstants.getVoxelMapInstance().getImageProperties().getProperty("radarIconFiltering", "true"));
        textureAtlas.sampler = useFiltering ? VoxelMapSamplers.LINEAR_CLAMP : VoxelMapSamplers.NEAREST_CLAMP;

        entityVariantDataFactories.clear();
        customMobProperties.clear();

        addVariantDataFactory(new EntityVariantDataFactory(EntityTypes.BOGGED, Identifier.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png"), 0xFFFFFFFF));
        addVariantDataFactory(new EntityVariantDataFactory(EntityTypes.DROWNED, Identifier.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png"), 0xFFFFFFFF));
        addVariantDataFactory(new EntityVariantDataFactory(EntityTypes.ENDERMAN, Identifier.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png"), 0xFFFFFFFF));
        addVariantDataFactory(new EnderDragonVariantDataFactory(EntityTypes.ENDER_DRAGON));
        addVariantDataFactory(new HorseVariantDataFactory(EntityTypes.HORSE));
        addVariantDataFactory(new TropicalFishVariantDataFactory(EntityTypes.TROPICAL_FISH));
        addVariantDataFactory(new VillagerVariantDataFactory(EntityTypes.VILLAGER));
        addVariantDataFactory(new VillagerVariantDataFactory(EntityTypes.ZOMBIE_VILLAGER));

        addVariantDataFactory(new ArmorVariantDataFactory(Items.LEATHER_HELMET, Identifier.withDefaultNamespace("textures/entity/equipment/humanoid/leather_overlay.png"), 0xFFFFFFFF));

        if (VoxelConstants.DEBUG) {
            VoxelConstants.getLogger().info("EntityMapImageManager: Resetting");
            BuiltInRegistries.ENTITY_TYPE.forEach(t -> requestImageForMobType(t, true));
        }
    }

    // Mob image

    private void addVariantDataFactory(EntityVariantDataFactory factory) {
        entityVariantDataFactories.put(factory.type(), factory);
    }

    private String getMobIdentifier(Entity entity) {
        stringBuilder.setLength(0);

        // Common state
        stringBuilder.append(",scale:").append(getUniqueMobScale(entity));
        stringBuilder.append(",isBaby:").append(entity instanceof LivingEntity le && le.isBaby());

        // Unique state
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
    private VariantDataHolder getOrCreateVariantData(Entity entity, EntityRenderer renderer, boolean addBorder) {
        if (entity instanceof AbstractClientPlayer player) {
            return new EntityVariantData(entity.getType(), "", player.getSkin().body().texturePath(), 0xFFFFFFFF, addBorder);
        }

        EntityRenderState renderState = renderer.createRenderState(entity, 0.5F);
        String id = getMobIdentifier(entity);

        EntityVariantDataFactory factory = entityVariantDataFactories.get(entity.getType());
        if (factory != null) {
            EntityVariantData data = factory.create(entity, renderer, renderState, id, addBorder);
            if (data != null) {
                return data;
            }
        }
        return EntityVariantDataFactory.createSimple(entity, renderer, renderState, id, addBorder);
    }

    public Sprite requestImageForMobType(EntityType<?> type, boolean addBorder) {
        try {
            if (minecraft.level != null && type.create(minecraft.level, EntitySpawnReason.LOAD) instanceof LivingEntity entity) {
                entity.setId(-1);
                return requestImageForMob(entity, addBorder);
            }
        } catch (Exception e) {
            VoxelConstants.getLogger().warn("Failed to load entity for icon: {}", type.getDescriptionId());
        }
        return null;
    }

    public Sprite requestImageForMob(Entity entity, boolean addBorder) {
        Sprite customIcon = tryCustomMobIcon(entity.getType(), addBorder);
        if (customIcon != null && customIcon.getTextureData() != null && customIcon != textureAtlas.getMissingImage()) {
            return customIcon;
        }

        EntityRenderer<?, ?> baseRenderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        VariantDataHolder dataHolder = getOrCreateVariantData(entity, baseRenderer, addBorder);

        Sprite existing = textureAtlas.getAtlasSpriteIncludingYetToBeStitched(dataHolder);
        if (existing != null && existing != textureAtlas.getMissingImage()) {
            return existing;
        }
        if (VoxelConstants.DEBUG) VoxelConstants.getLogger().info("EntityMapImageManager: Rendering Mob of type {}", dataHolder.name());

        Sprite sprite = textureAtlas.registerEmptyIcon(dataHolder);

        meshRenderer.setupMatrix(1.0F / getUniqueMobScale(entity), getCustomMobProperties(entity.getType()));
        meshRenderer.beginBatch(VoxelMapPipelines.ENTITY_ICON, dataHolder);
        meshBuilder.buildEntityMeshes(meshRenderer.matrix(), meshRenderer.vertexBuffer(), entity, baseRenderer);
        meshRenderer.endBatch((image) -> postProcessRenderedMobImage(entity, sprite, image, addBorder));

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

            image = ImageUtils.trim(image);
            image = ImageUtils.fillOutline(ImageUtils.pad(image), addBorder, 2);

            addSpriteCreationTask(sprite, image);
        });
    }

    // Armor image

    private void addVariantDataFactory(ArmorVariantDataFactory factory) {
        armorVariantDataFactories.put(factory.type(), factory);
    }

    private String getArmorIdentifier(ItemStack itemStack) {
        return "";
    }

    private VariantDataHolder getOrCreateVariantData(ItemStack itemStack, boolean addBorder) {
        String id = getArmorIdentifier(itemStack);

        ArmorVariantDataFactory factory = armorVariantDataFactories.get(itemStack.getItem());
        if (factory != null) {
            ArmorVariantData data = factory.create(itemStack, id, addBorder);
            if (data != null) {
                return data;
            }
        }
        return ArmorVariantDataFactory.createSimple(itemStack, id, addBorder);
    }

    public Sprite requestImageForArmor(Entity entity, boolean addBorder) {
        ItemStack itemStack;
        if (!(entity instanceof LivingEntity livingEntity) || (itemStack = livingEntity.getItemBySlot(EquipmentSlot.HEAD)).isEmpty()) {
            return null;
        }

        VariantDataHolder dataHolder = getOrCreateVariantData(itemStack, addBorder);

        Sprite existing = textureAtlas.getAtlasSpriteIncludingYetToBeStitched(dataHolder);
        if (existing != null && existing != textureAtlas.getMissingImage()) {
            return existing;
        }
        Sprite sprite = textureAtlas.registerEmptyIcon(dataHolder);

        meshRenderer.setupMatrix(1.0F, getCustomMobProperties(entity.getType()));
        meshRenderer.beginBatch(VoxelMapPipelines.ENTITY_ICON_CULLED, dataHolder);
        meshBuilder.buildArmorMeshes(meshRenderer.matrix(), meshRenderer.vertexBuffer(), itemStack);
        meshRenderer.endBatch((image) ->  postProcessRenderedArmorImage(itemStack, sprite, image, addBorder));

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
            if (VoxelConstants.DEBUG) VoxelConstants.getLogger().info("EntityMapImageManager: BufferedImage: ({} / {}) added to texture atlas {} ({} * {})", doneSpriteCreations, totalSpriteCreations, sprite.getIconName(), image.getWidth(), image.getHeight());
            if (doneSpriteCreations == totalSpriteCreations) {
                textureAtlas.stitchNew();
                if (VoxelConstants.DEBUG) {
                    VoxelConstants.getLogger().info("EntityMapImageManager: Stitching!");
                    textureAtlas.saveDebugImage();
                }
            }
        });
    }

    public void onRenderTick() {
        Runnable task;
        while ((task = spriteCreationTask.poll()) != null) {
            task.run();
        }
    }
}
