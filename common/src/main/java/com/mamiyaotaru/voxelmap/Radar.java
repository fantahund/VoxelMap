package com.mamiyaotaru.voxelmap;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.StitcherException;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.CustomMob;
import com.mamiyaotaru.voxelmap.util.CustomMobsManager;
import com.mamiyaotaru.voxelmap.util.EnumMobs;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mamiyaotaru.voxelmap.util.ReflectionUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.BatModel;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.CamelModel;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.DolphinModel;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.EndermiteModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.GhastModel;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.HoglinModel;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.LavaSlimeModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.RabbitModel;
import net.minecraft.client.model.RavagerModel;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.SnifferModel;
import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.StriderModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.metadata.animation.VillagerMetadataSection;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Radar implements IRadar {
    private LayoutVariables layoutVariables;
    public final MapSettingsManager minimapOptions;
    public final RadarSettingsManager options;
    private final TextureAtlas textureAtlas;
    private boolean newMobs;
    private boolean completedLoading;
    private int timer = 500;
    private float direction;
    private final ArrayList<Contact> contacts = new ArrayList<>(40);
    public final HashMap<String, Integer> mpContactsSkinGetTries = new HashMap<>();
    public final HashMap<String, Integer> contactsSkinGetTries = new HashMap<>();
    private Sprite clothIcon;
    private static final int UNKNOWN = EnumMobs.UNKNOWN.ordinal();
    private final String[] armorNames = {"cloth", "clothOverlay", "clothOuter", "clothOverlayOuter", "chain", "iron", "gold", "diamond", "netherite", "turtle"};
    private boolean randomobsOptifine;
    private Map<String, Object> mapProperties;
    private Object randomEntity;
    private Class<?> randomEntityClass;
    private Method setEntityMethod;
    private Class<?> randomEntitiesPropertiesClass;
    private Method getEntityTextureMethod;
    private boolean hasCustomNPCs;
    private Class<?> entityCustomNpcClass;
    private Field modelDataField;
    private Method getEntityMethod;
    private boolean lastOutlines = true;
    private SkullModel playerSkullModel;
    private HumanoidModel<HumanoidRenderState> bipedArmorModel;
    private SkeletonModel<SkeletonRenderState> strayOverlayModel;
    private ZombieModel<ZombieRenderState> drownedOverlayModel;
    private HumanoidModel<HumanoidRenderState> piglinArmorModel;
    private DynamicTexture nativeBackedTexture = new DynamicTexture(2, 2, false);
    private final ResourceLocation nativeBackedTextureLocation = ResourceLocation.fromNamespaceAndPath("voxelmap", "tempimage");
    private final Vector3f fullbright = new Vector3f(1.0F, 1.0F, 1.0F);
    // private HashMap<List<ModelPartWithResourceLocation>, BufferedImage> cachedImages = new HashMap<>();
    // private HashMap<BufferedImage, Sprite> cachedSprites = new HashMap<>();
    private static final HashMap<UUID, BufferedImage> entityIconMap = new HashMap<>();

    private static final Int2ObjectMap<ResourceLocation> LEVEL_TO_ID = Util.make(new Int2ObjectOpenHashMap<>(), int2ObjectOpenHashMap -> {
        int2ObjectOpenHashMap.put(1, ResourceLocation.parse("stone"));
        int2ObjectOpenHashMap.put(2, ResourceLocation.parse("iron"));
        int2ObjectOpenHashMap.put(3, ResourceLocation.parse("gold"));
        int2ObjectOpenHashMap.put(4, ResourceLocation.parse("emerald"));
        int2ObjectOpenHashMap.put(5, ResourceLocation.parse("diamond"));
    });
    private static final Map<Markings, Object> TEXTURES = Util.make(Maps.newEnumMap(Markings.class), enumMap -> {
        enumMap.put(Markings.NONE, null);
        enumMap.put(Markings.WHITE, ResourceLocation.parse("textures/entity/horse/horse_markings_white.png"));
        enumMap.put(Markings.WHITE_FIELD, ResourceLocation.parse("textures/entity/horse/horse_markings_whitefield.png"));
        enumMap.put(Markings.WHITE_DOTS, ResourceLocation.parse("textures/entity/horse/horse_markings_whitedots.png"));
        enumMap.put(Markings.BLACK_DOTS, ResourceLocation.parse("textures/entity/horse/horse_markings_blackdots.png"));
    });

    public Radar() {
        this.minimapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
        this.textureAtlas = new TextureAtlas("mobs");
        this.textureAtlas.setFilter(false, false);

        try {
            Class<?> randomEntitiesClass = Class.forName("net.optifine.RandomEntities");
            Field mapPropertiesField = randomEntitiesClass.getDeclaredField("mapProperties");
            mapPropertiesField.setAccessible(true);
            this.mapProperties = (Map) mapPropertiesField.get(null);
            Field randomEntityField = randomEntitiesClass.getDeclaredField("randomEntity");
            randomEntityField.setAccessible(true);
            this.randomEntity = randomEntityField.get(null);
            Class<?> iRandomEntityClass = Class.forName("net.optifine.IRandomEntity");
            this.randomEntityClass = Class.forName("net.optifine.RandomEntity");
            Class<?>[] argClasses1 = new Class[]{Entity.class};
            this.setEntityMethod = this.randomEntityClass.getDeclaredMethod("setEntity", argClasses1);
            this.randomEntitiesPropertiesClass = Class.forName("net.optifine.RandomEntityProperties");
            Class<?>[] argClasses2 = new Class[]{ResourceLocation.class, iRandomEntityClass};
            this.getEntityTextureMethod = this.randomEntitiesPropertiesClass.getDeclaredMethod("getTextureLocation", argClasses2);
            this.randomobsOptifine = true;
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException var7) {
            this.randomobsOptifine = false;
        }

        try {
            this.entityCustomNpcClass = Class.forName("noppes.npcs.entity.EntityCustomNpc");
            Class<?> modelDataClass = Class.forName("noppes.npcs.ModelData");
            this.modelDataField = this.entityCustomNpcClass.getField("modelData");
            Class<?> entityNPCInterfaceClass = Class.forName("noppes.npcs.entity.EntityNPCInterface");
            this.getEntityMethod = modelDataClass.getMethod("getEntity", entityNPCInterfaceClass);
            this.hasCustomNPCs = true;
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException var4) {
            this.hasCustomNPCs = false;
        }

    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        // cachedImages.clear();
        // cachedSprites.clear();
        entityIconMap.clear();
        this.loadTexturePackIcons();
    }

    private void loadTexturePackIcons() {
        this.completedLoading = false;

        try {
            this.mpContactsSkinGetTries.clear();
            this.contactsSkinGetTries.clear();
            this.textureAtlas.reset();
            LayerDefinition texturedModelData12 = SkullModel.createHumanoidHeadLayer();
            ModelPart skullModelPart = texturedModelData12.bakeRoot();
            this.playerSkullModel = new SkullModel(skullModelPart);
            CubeDeformation ARMOR_DILATION = new CubeDeformation(1.0F);
            LayerDefinition texturedModelData2 = LayerDefinition.create(HumanoidModel.createMesh(ARMOR_DILATION, 0.0F), 64, 32);
            ModelPart bipedArmorModelPart = texturedModelData2.bakeRoot();
            this.bipedArmorModel = new HumanoidModel<>(bipedArmorModelPart);
            LayerDefinition strayModelData = LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.25F), 0.0F), 64, 32);
            ModelPart strayOverlayModelPart = strayModelData.bakeRoot();
            this.strayOverlayModel = new SkeletonModel<>(strayOverlayModelPart);
            LayerDefinition drownedModelData = DrownedModel.createBodyLayer(new CubeDeformation(0.25F));
            ModelPart drownedOverlayModelPart = drownedModelData.bakeRoot();
            this.drownedOverlayModel = new ZombieModel<>(drownedOverlayModelPart);
            LayerDefinition texturedModelData3 = LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.02F), 0.0F), 64, 32);
            ModelPart piglinArmorModelPart = texturedModelData3.bakeRoot();
            this.piglinArmorModel = new HumanoidModel<>(piglinArmorModelPart);
            if (ReflectionUtils.classExists("com.prupe.mcpatcher.mob.MobOverlay") && ImageUtils.loadImage(ResourceLocation.parse("mcpatcher/mob/cow/mooshroom_overlay.png"), 0, 0, 1, 1) != null) {
                EnumMobs.MOOSHROOM.secondaryResourceLocation = ResourceLocation.parse("mcpatcher/mob/cow/mooshroom_overlay.png");
            } else {
                EnumMobs.MOOSHROOM.secondaryResourceLocation = ResourceLocation.parse("textures/block/red_mushroom.png");
            }

            for (int t = 0; t < EnumMobs.values().length - 1; ++t) {
                String identifier = "minecraft." + EnumMobs.values()[t].id;
                String identifierSimple = EnumMobs.values()[t].id;
                String spriteName = identifier + EnumMobs.values()[t].resourceLocation.toString();
                spriteName = spriteName + (EnumMobs.values()[t].secondaryResourceLocation != null ? EnumMobs.values()[t].secondaryResourceLocation.toString() : "");
                BufferedImage mobImage = this.getCustomMobImage(identifier, identifierSimple);
                if (mobImage != null) {
                    Sprite sprite = this.textureAtlas.registerIconForBufferedImage(identifier + "custom", mobImage);
                    this.textureAtlas.registerMaskedIcon(spriteName, sprite);
                } else {
                    this.textureAtlas.registerFailedIcon(identifier + "custom");
                    if (EnumMobs.values()[t].expectedWidth > 0.5) {
                        mobImage = this.createImageFromTypeAndResourceLocations(EnumMobs.values()[t], EnumMobs.values()[t].resourceLocation, EnumMobs.values()[t].secondaryResourceLocation, null);
                        if (mobImage != null) {
                            float scale = mobImage.getWidth() / EnumMobs.values()[t].expectedWidth;
                            mobImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(mobImage, 4.0F / scale)), this.options.outlines, 2);
                            this.textureAtlas.registerIconForBufferedImage(spriteName, mobImage);
                        }
                    }
                }
            }

            /*BufferedImage[] armorImages = { ImageUtils.loadImage(ResourceLocation.parse("textures/models/armor/leather_layer_1.png"), 8, 8, 8, 8), ImageUtils.loadImage(ResourceLocation.parse("textures/models/armor/leather_layer_1.png"), 40, 8, 8, 8),
                    ImageUtils.loadImage(ResourceLocation.parse("textures/models/armor/leather_layer_1_overlay.png"), 8, 8, 8, 8), ImageUtils.loadImage(ResourceLocation.parse("textures/models/armor/leather_layer_1_overlay.png"), 40, 8, 8, 8) };

            for (int t = 0; t < armorImages.length; ++t) {
                float scale = armorImages[t].getWidth() / 8.0F;
                armorImages[t] = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(armorImages[t], 4.0F / scale * 47.0F / 38.0F)), this.options.outlines && t != 2 && t != 3, true, 37.6F, 37.6F, 2);
                Sprite icon = this.textureAtlas.registerIconForBufferedImage("armor " + this.armorNames[t], armorImages[t]);
                if (t == 0) {
                    this.clothIcon = icon;
                }
            }*/ //FIXME 1.21.2

            BufferedImage zombie = ImageUtils.loadImage(EnumMobs.ZOMBIE.resourceLocation, 8, 8, 8, 8, 64, 64);
            float scale = zombie.getWidth() / 8.0F;
            zombie = ImageUtils.scaleImage(zombie, 4.0F / scale * 47.0F / 38.0F);
            BufferedImage zombieHat = ImageUtils.loadImage(EnumMobs.ZOMBIE.resourceLocation, 40, 8, 8, 8, 64, 64);
            zombieHat = ImageUtils.scaleImage(zombieHat, 4.0F / scale * 47.0F / 35.0F);
            zombie = ImageUtils.addImages(ImageUtils.addImages(new BufferedImage(zombieHat.getWidth(), zombieHat.getHeight() + 8, 6), zombie, (zombieHat.getWidth() - zombie.getWidth()) / 2.0F, (zombieHat.getHeight() - zombie.getHeight()) / 2.0F, zombieHat.getWidth(), zombieHat.getHeight() + 8),
                    zombieHat, 0.0F, 0.0F, zombieHat.getWidth(), zombieHat.getHeight() + 8);
            zombieHat.flush();
            zombie = ImageUtils.fillOutline(ImageUtils.pad(zombie), this.options.outlines, true, 37.6F, 37.6F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.ZOMBIE.id + EnumMobs.ZOMBIE.resourceLocation.toString() + "head", zombie);
            BufferedImage skeleton = ImageUtils.loadImage(EnumMobs.SKELETON.resourceLocation, 8, 8, 8, 8, 64, 32);
            scale = skeleton.getWidth() / 8.0F;
            skeleton = ImageUtils.scaleImage(skeleton, 4.0F / scale * 47.0F / 38.0F);
            skeleton = ImageUtils.addImages(new BufferedImage(skeleton.getWidth(), skeleton.getHeight() + 8, 6), skeleton, 0.0F, 0.0F, skeleton.getWidth(), skeleton.getHeight() + 8);
            skeleton = ImageUtils.fillOutline(ImageUtils.pad(skeleton), this.options.outlines, true, 37.6F, 37.6F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.SKELETON.id + EnumMobs.SKELETON.resourceLocation.toString() + "head", skeleton);
            BufferedImage witherSkeleton = ImageUtils.loadImage(EnumMobs.SKELETONWITHER.resourceLocation, 8, 8, 8, 8, 64, 32);
            scale = witherSkeleton.getWidth() / 8.0F;
            witherSkeleton = ImageUtils.scaleImage(witherSkeleton, 4.0F / scale * 47.0F / 38.0F);
            witherSkeleton = ImageUtils.addImages(new BufferedImage(witherSkeleton.getWidth(), witherSkeleton.getHeight() + 8, 6), witherSkeleton, 0.0F, 0.0F, witherSkeleton.getWidth(), witherSkeleton.getHeight() + 8);
            witherSkeleton = ImageUtils.fillOutline(ImageUtils.pad(witherSkeleton), this.options.outlines, true, 37.6F, 37.6F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.SKELETONWITHER.id + EnumMobs.SKELETONWITHER.resourceLocation.toString() + "head", witherSkeleton);
            BufferedImage creeper = ImageUtils.addImages(ImageUtils.blankImage(EnumMobs.CREEPER.resourceLocation, 8, 10), ImageUtils.loadImage(EnumMobs.CREEPER.resourceLocation, 8, 8, 8, 8), 0.0F, 0.0F, 8, 10);
            scale = creeper.getWidth() / EnumMobs.CREEPER.expectedWidth;
            creeper = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(creeper, 4.0F / scale * 47.0F / 38.0F)), this.options.outlines, true, 37.6F, 37.6F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.CREEPER.id + EnumMobs.CREEPER.resourceLocation.toString() + "head", creeper);
            BufferedImage dragon = this.createImageFromTypeAndResourceLocations(EnumMobs.ENDERDRAGON, EnumMobs.ENDERDRAGON.resourceLocation, null, null);
            scale = dragon.getWidth() / EnumMobs.ENDERDRAGON.expectedWidth;
            dragon = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(dragon, 4.0F / scale)), this.options.outlines, true, 32.0F, 32.0F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.ENDERDRAGON.id + EnumMobs.ENDERDRAGON.resourceLocation.toString() + "head", dragon);
            BufferedImage sheepFur = ImageUtils.loadImage(ResourceLocation.parse("textures/entity/sheep/sheep_fur.png"), 6, 6, 6, 6);
            scale = sheepFur.getWidth() / 6.0F;
            sheepFur = ImageUtils.scaleImage(sheepFur, 4.0F / scale * 1.0625F);
            int chop = (int) Math.max(1.0F, 2.0F);
            ImageUtils.eraseArea(sheepFur, chop, chop, sheepFur.getWidth() - chop * 2, sheepFur.getHeight() - chop * 2, sheepFur.getWidth(), sheepFur.getHeight());
            sheepFur = ImageUtils.fillOutline(ImageUtils.pad(sheepFur), this.options.outlines, true, 27.5F, 27.5F, (int) Math.max(1.0F, 2.0F));
            this.textureAtlas.registerIconForBufferedImage("sheepfur", sheepFur);
            ResourceLocation fontResourceLocation = ResourceLocation.parse("textures/font/ascii.png");
            BufferedImage fontImage = ImageUtils.loadImage(fontResourceLocation, 0, 0, 128, 128, 128, 128);
            if (fontImage.getWidth() > 512 || fontImage.getHeight() > 512) {
                int maxDim = Math.max(fontImage.getWidth(), fontImage.getHeight());
                float scaleBy = 512.0F / maxDim;
                fontImage = ImageUtils.scaleImage(fontImage, scaleBy);
            }

            this.textureAtlas.stitch();
            applyFilteringParameters();
            this.completedLoading = true;
        } catch (Exception var30) {
            VoxelConstants.getLogger().error("Failed getting mobs" + var30.getLocalizedMessage(), var30);
        }

    }

    private BufferedImage createImageFromTypeAndResourceLocations(EnumMobs type, ResourceLocation resourceLocation, ResourceLocation resourceLocationSecondary, Entity entity) {
        BufferedImage mobImage = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
        BufferedImage mobImageSecondary = null;
        if (resourceLocationSecondary != null) {
            mobImageSecondary = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationSecondary);
        }

        try {
            return this.createImageFromTypeAndImages(type, mobImage, mobImageSecondary, entity);
        } catch (Exception var8) {
            return null;
        }
    }

    private BufferedImage createImageFromTypeAndImages(EnumMobs type, BufferedImage mobImage, BufferedImage mobImageSecondary, Entity entity) {
        BufferedImage image = null;
        switch (type) {
            case GENERICHOSTILE ->
                    image = ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/hostile.png"), 0, 0, 16, 16, 16, 16);
            case GENERICNEUTRAL ->
                    image = ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/neutral.png"), 0, 0, 16, 16, 16, 16);
            case GENERICTAME ->
                    image = ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/tame.png"), 0, 0, 16, 16, 16, 16);
            case BAT ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 12, 64, 64), ImageUtils.loadImage(mobImage, 25, 1, 3, 4), 0.0F, 0.0F, 8, 12), ImageUtils.flipHorizontal(ImageUtils.loadImage(mobImage, 25, 1, 3, 4)), 5.0F, 0.0F, 8, 12),
                            ImageUtils.loadImage(mobImage, 6, 6, 6, 6), 1.0F, 3.0F, 8, 12);
            case CHICKEN ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.loadImage(mobImage, 2, 3, 6, 6), ImageUtils.loadImage(mobImage, 16, 2, 4, 2), 1.0F, 2.0F, 6, 6), ImageUtils.loadImage(mobImage, 16, 6, 2, 2), 2.0F, 4.0F, 6, 6);
            case COD -> image = ImageUtils.addImages(
                    ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 16, 5, 32, 32), ImageUtils.loadImage(mobImage, 15, 3, 1, 3, 32, 32), 1.0F, 1.0F, 16, 5), ImageUtils.loadImage(mobImage, 16, 3, 3, 4, 32, 32), 2.0F, 1.0F, 16, 5),
                            ImageUtils.loadImage(mobImage, 9, 7, 7, 4, 32, 32), 5.0F, 1.0F, 16, 5), ImageUtils.loadImage(mobImage, 26, 7, 4, 4, 32, 32), 12.0F, 1.0F, 16, 5),
                    ImageUtils.loadImage(mobImage, 26, 0, 6, 1, 32, 32), 4.0F, 0.0F, 16, 5);
            case ENDERDRAGON -> image = ImageUtils.addImages(ImageUtils.addImages(
                    ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 16, 20, 256, 256), ImageUtils.loadImage(mobImage, 128, 46, 16, 16, 256, 256), 0.0F, 4.0F, 16, 16), ImageUtils.loadImage(mobImage, 192, 60, 12, 5, 256, 256), 2.0F, 11.0F, 16, 16),
                            ImageUtils.loadImage(mobImage, 192, 81, 12, 4, 256, 256), 2.0F, 16.0F, 16, 16),
                    ImageUtils.loadImage(mobImage, 6, 6, 2, 4, 256, 256), 3.0F, 0.0F, 16, 16), ImageUtils.flipHorizontal(ImageUtils.loadImage(mobImage, 6, 6, 2, 4, 256, 256)), 11.0F, 0.0F, 16, 16);
            case GHAST, GHASTATTACKING -> image = ImageUtils.loadImage(mobImage, 16, 16, 16, 16);
            case GUARDIAN ->
                    image = ImageUtils.scaleImage(ImageUtils.addImages(ImageUtils.loadImage(mobImage, 16, 16, 12, 12), ImageUtils.loadImage(mobImage, 9, 1, 2, 2), 5.0F, 5.5F, 12, 12), 0.5F);
            case GUARDIANELDER ->
                    image = ImageUtils.addImages(ImageUtils.loadImage(mobImage, 16, 16, 12, 12), ImageUtils.loadImage(mobImage, 9, 1, 2, 2), 5.0F, 5.5F, 12, 12);
            case HORSE -> image = ImageUtils.addImages(
                    ImageUtils.addImages(ImageUtils.addImages(
                            ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 16, 24, 64, 64), ImageUtils.loadImage(mobImage, 56, 38, 2, 16, 64, 64), 1.0F, 7.0F, 16, 24), ImageUtils.loadImage(mobImage, 0, 42, 7, 12, 64, 64), 3.0F, 12.0F, 16, 24),
                                    ImageUtils.loadImage(mobImage, 0, 20, 7, 5, 64, 64), 3.0F, 7.0F, 16, 24),
                            ImageUtils.loadImage(mobImage, 0, 30, 5, 5, 64, 64), 10.0F, 7.0F, 16, 24), ImageUtils.loadImage(mobImage, 19, 17, 1, 3, 64, 64), 3.0F, 4.0F, 16, 24),
                    ImageUtils.loadImage(mobImage, 0, 13, 1, 7, 64, 64), 3.0F, 0.0F, 16, 24);
            case IRONGOLEM ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 12, 128, 128), ImageUtils.loadImage(mobImage, 8, 8, 8, 10, 128, 128), 0.0F, 1.0F, 8, 12), ImageUtils.loadImage(mobImage, 26, 2, 2, 4, 128, 128), 3.0F, 8.0F, 8, 12);
            case LLAMA, LLAMATRADER -> image = ImageUtils
                    .addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 14, 128, 64), ImageUtils.loadImage(mobImage, 6, 20, 8, 8, 128, 64), 0.0F, 3.0F, 8, 14), ImageUtils.loadImage(mobImage, 9, 9, 4, 4, 128, 64), 2.0F, 5.0F, 8, 14),
                            ImageUtils.loadImage(mobImage, 19, 2, 3, 3, 128, 64), 0.0F, 0.0F, 8, 14), ImageUtils.loadImage(mobImage, 19, 2, 3, 3, 128, 64), 5.0F, 0.0F, 8, 14);
            case MAGMA ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.loadImage(mobImage, 8, 8, 8, 8), ImageUtils.loadImage(mobImage, 32, 18, 8, 1), 0.0F, 3.0F, 8, 8), ImageUtils.loadImage(mobImage, 32, 27, 8, 1), 0.0F, 4.0F, 8, 8);
            case MOOSHROOM -> {
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 40, 40), ImageUtils.loadImage(mobImage, 6, 6, 8, 8), 16.0F, 16.0F, 40, 40), ImageUtils.loadImage(mobImage, 23, 1, 1, 3), 15.0F, 15.0F, 40, 40),
                        ImageUtils.loadImage(mobImage, 23, 1, 1, 3), 24.0F, 15.0F, 40, 40);
                if (mobImageSecondary != null) {
                    BufferedImage mushroomImage;
                    if (mobImageSecondary.getWidth() != mobImageSecondary.getHeight()) {
                        mushroomImage = ImageUtils.loadImage(mobImageSecondary, 32, 0, 16, 16, 48, 16);
                    } else {
                        mushroomImage = ImageUtils.loadImage(mobImageSecondary, 0, 0, 16, 16, 16, 16);
                    }

                    float ratio = (float) image.getWidth() / mushroomImage.getWidth();
                    if (ratio < 2.5) {
                        image = ImageUtils.scaleImage(image, 2.5F / ratio);
                    } else if (ratio > 2.5) {
                        mushroomImage = ImageUtils.scaleImage(mushroomImage, ratio / 2.5F);
                    }

                    ImageUtils.addImages(image, mushroomImage, 12.0F, 0.0F, 40, 40);
                }
            }
            case PARROT -> image = ImageUtils.addImages(
                    ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 8, 32, 32), ImageUtils.loadImage(mobImage, 2, 22, 3, 5, 32, 32), 1.0F, 0.0F, 8, 8), ImageUtils.loadImage(mobImage, 10, 4, 4, 1, 32, 32), 2.0F, 4.0F, 8, 8),
                            ImageUtils.loadImage(mobImage, 2, 4, 2, 3, 32, 32), 2.0F, 5.0F, 8, 8), ImageUtils.loadImage(mobImage, 11, 8, 1, 2, 32, 32), 4.0F, 5.0F, 8, 8),
                    ImageUtils.loadImage(mobImage, 16, 8, 1, 2, 32, 32), 5.0F, 5.0F, 8, 8);
            case PHANTOM ->
                    image = ImageUtils.addImages(ImageUtils.loadImage(mobImage, 5, 5, 7, 3, 64, 64), ImageUtils.loadImage(mobImageSecondary, 5, 5, 7, 3, 64, 64), 0.0F, 0.0F, 7, 3);
            case PUFFERFISH ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 3, 3, 32, 32), ImageUtils.loadImage(mobImage, 3, 30, 3, 2, 32, 32), 0.0F, 1.0F, 3, 3), ImageUtils.loadImage(mobImage, 3, 29, 1, 1, 32, 32), 0.0F, 0.0F, 3, 3),
                            ImageUtils.loadImage(mobImage, 5, 29, 1, 1, 32, 32), 2.0F, 0.0F, 3, 3);
            case PUFFERFISHHALF -> image = ImageUtils.loadImage(mobImage, 17, 27, 5, 5, 32, 32);
            case PUFFERFISHFULL -> image = ImageUtils.loadImage(mobImage, 8, 8, 8, 8, 32, 32);
            case SALMON -> image = ImageUtils.addImages(
                    ImageUtils.addImages(ImageUtils
                                    .addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 26, 7, 32, 32), ImageUtils.loadImage(mobImage, 27, 3, 3, 4, 32, 32), 1.0F, 2.5F, 26, 7), ImageUtils.loadImage(mobImage, 11, 8, 8, 5, 32, 32), 4.0F, 2.0F, 26, 7),
                                            ImageUtils.loadImage(mobImage, 11, 21, 8, 5, 32, 32), 12.0F, 2.0F, 26, 7), ImageUtils.loadImage(mobImage, 26, 16, 6, 5, 32, 32), 20.0F, 2.0F, 26, 7),
                            ImageUtils.loadImage(mobImage, 0, 0, 2, 2, 32, 32), 10.0F, 0.0F, 26, 7),
                    ImageUtils.loadImage(mobImage, 5, 6, 3, 2, 32, 32), 12.0F, 0.0F, 26, 7);
            case SLIME -> image = ImageUtils
                    .addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 8), ImageUtils.loadImage(mobImage, 6, 22, 6, 6), 1.0F, 1.0F, 8, 8), ImageUtils.loadImage(mobImage, 34, 6, 2, 2), 5.0F, 2.0F, 8, 8),
                            ImageUtils.loadImage(mobImage, 34, 2, 2, 2), 1.0F, 2.0F, 8, 8), ImageUtils.loadImage(mobImage, 33, 9, 1, 1), 4.0F, 5.0F, 8, 8), ImageUtils.loadImage(mobImage, 8, 8, 8, 8), 0.0F, 0.0F, 8, 8);
            case TROPICALFISHA -> {
                if (entity instanceof TropicalFish fish) {
                    Color primaryColorsA = new Color(fish.getBaseColor().getTextureDiffuseColor());
                    Color secondaryColorsA = new Color(fish.getPatternColor().getTextureDiffuseColor());
                    BufferedImage baseA = ImageUtils
                            .colorify(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 10, 6, 32, 32), ImageUtils.loadImage(mobImage, 8, 6, 6, 3, 32, 32), 0.0F, 3.0F, 10, 6), ImageUtils.loadImage(mobImage, 17, 1, 5, 3, 32, 32), 1.0F, 0.0F, 10, 6),
                                    ImageUtils.loadImage(mobImage, 28, 0, 4, 3, 32, 32), 6.0F, 3.0F, 10, 6), primaryColorsA.getRed(), primaryColorsA.getGreen(), primaryColorsA.getBlue());
                    BufferedImage patternA = ImageUtils.colorify(ImageUtils.addImages(
                            ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImageSecondary, 10, 6, 32, 32), ImageUtils.loadImage(mobImageSecondary, 8, 6, 6, 3, 32, 32), 0.0F, 3.0F, 10, 6), ImageUtils.loadImage(mobImageSecondary, 17, 1, 5, 3, 32, 32), 1.0F, 0.0F, 10, 6),
                            ImageUtils.loadImage(mobImageSecondary, 28, 0, 4, 3, 32, 32), 6.0F, 3.0F, 10, 6), secondaryColorsA.getRed(), secondaryColorsA.getGreen(), secondaryColorsA.getBlue());
                    image = ImageUtils.addImages(baseA, patternA, 0.0F, 0.0F, 10, 6);
                    baseA.flush();
                    patternA.flush();
                }
            }
            case TROPICALFISHB -> {
                if (entity instanceof TropicalFish fish) {
                    Color primaryColorsB = new Color(fish.getBaseColor().getTextureDiffuseColor());
                    Color secondaryColorsB = new Color(fish.getPatternColor().getTextureDiffuseColor());
                    BufferedImage baseB = ImageUtils.colorify(ImageUtils
                                    .addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 12, 12, 32, 32), ImageUtils.loadImage(mobImage, 0, 26, 6, 6, 32, 32), 6.0F, 3.0F, 12, 12), ImageUtils.loadImage(mobImage, 20, 21, 6, 6, 32, 32), 0.0F, 3.0F, 12, 12),
                                            ImageUtils.loadImage(mobImage, 20, 18, 5, 3, 32, 32), 6.0F, 0.0F, 12, 12), ImageUtils.loadImage(mobImage, 20, 27, 5, 3, 32, 32), 6.0F, 9.0F, 12, 12),
                            primaryColorsB.getRed(), primaryColorsB.getGreen(), primaryColorsB.getBlue());
                    BufferedImage patternB = ImageUtils
                            .colorify(
                                    ImageUtils.addImages(
                                            ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImageSecondary, 12, 12, 32, 32), ImageUtils.loadImage(mobImageSecondary, 0, 26, 6, 6, 32, 32), 6.0F, 3.0F, 12, 12),
                                                    ImageUtils.loadImage(mobImageSecondary, 20, 21, 6, 6, 32, 32), 0.0F, 3.0F, 12, 12), ImageUtils.loadImage(mobImageSecondary, 20, 18, 5, 3, 32, 32), 6.0F, 0.0F, 12, 12),
                                            ImageUtils.loadImage(mobImageSecondary, 20, 27, 5, 3, 32, 32), 6.0F, 9.0F, 12, 12),
                                    secondaryColorsB.getRed(), secondaryColorsB.getGreen(), secondaryColorsB.getBlue());
                    image = ImageUtils.addImages(baseB, patternB, 0.0F, 0.0F, 12, 12);
                    baseB.flush();
                    patternB.flush();
                }

            }
            case WITHER, WITHERINVULNERABLE -> image = ImageUtils.addImages(
                    ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 24, 10, 64, 64), ImageUtils.loadImage(mobImage, 8, 8, 8, 8, 64, 64), 8.0F, 0.0F, 24, 10), ImageUtils.loadImage(mobImage, 38, 6, 6, 6, 64, 64), 0.0F, 2.0F, 24, 10),
                    ImageUtils.loadImage(mobImage, 38, 6, 6, 6, 64, 64), 18.0F, 2.0F, 24, 10);
            default -> {
            }
        }

        mobImage.flush();
        if (mobImageSecondary != null) {
            mobImageSecondary.flush();
        }

        return image;
    }

    @Override
    public void onTickInGame(GuiGraphics drawContext, Matrix4fStack matrixStack, LayoutVariables layoutVariables, float scaleProj) {
        if (this.options.radarAllowed || this.options.radarMobsAllowed || this.options.radarPlayersAllowed) {
            this.layoutVariables = layoutVariables;
            if (this.options.isChanged()) {
                this.timer = 500;
                if (this.options.outlines != this.lastOutlines) {
                    this.lastOutlines = this.options.outlines;
                    this.loadTexturePackIcons();
                }
            }

            this.direction = GameVariableAccessShim.rotationYaw() + 180.0F;

            while (this.direction >= 360.0F) {
                this.direction -= 360.0F;
            }

            while (this.direction < 0.0F) {
                this.direction += 360.0F;
            }

            if (this.completedLoading && this.timer > 95) {
                this.calculateMobs();
                this.timer = 0;
            }

            ++this.timer;
            if (this.completedLoading) {
                this.renderMapMobs(drawContext, matrixStack, this.layoutVariables.mapX, this.layoutVariables.mapY, scaleProj);
            }

            OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        }
    }

    private boolean isEntityShown(Entity entity) {
        return entity != null && !entity.isInvisibleTo(VoxelConstants.getPlayer()) && (this.options.showHostiles && (this.options.radarAllowed || this.options.radarMobsAllowed) && this.isHostile(entity)
                || this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed) && this.isPlayer(entity) || this.options.showNeutrals && this.options.radarMobsAllowed && this.isNeutral(entity));
    }

    public void calculateMobs() {
        this.contacts.clear();

        Iterable<Entity> entities = VoxelConstants.getClientWorld().entitiesForRendering();

        for (Entity entity : entities) {
            try {
                if (this.isEntityShown(entity)) {
                    int wayX = GameVariableAccessShim.xCoord() - (int) entity.position().x();
                    int wayZ = GameVariableAccessShim.zCoord() - (int) entity.position().z();
                    int wayY = GameVariableAccessShim.yCoord() - (int) entity.position().y();
                    double hypot = wayX * wayX + wayZ * wayZ + wayY * wayY;
                    hypot /= this.layoutVariables.zoomScaleAdjusted * this.layoutVariables.zoomScaleAdjusted;
                    if (hypot < 961.0) {
                        if (this.hasCustomNPCs) {
                            try {
                                if (this.entityCustomNpcClass.isInstance(entity)) {
                                    Object modelData = this.modelDataField.get(entity);
                                    LivingEntity wrappedEntity = (LivingEntity) this.getEntityMethod.invoke(modelData, entity);
                                    if (wrappedEntity != null) {
                                        entity = wrappedEntity;
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        Contact contact = new Contact((LivingEntity) entity, EnumMobs.getMobTypeByEntity(entity));
                        String unscrubbedName = TextUtils.asFormattedString(contact.entity.getDisplayName());
                        contact.setName(unscrubbedName);
                        if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                            contact.yFudge = 1;
                        }

                        contact.updateLocation();
                        boolean enabled = false;
                        if (!contact.vanillaType) {
                            String type = entity.getType().getDescriptionId();
                            CustomMob customMob = CustomMobsManager.getCustomMobByType(type);
                            if (customMob == null || customMob.enabled) {
                                enabled = true;
                            }
                        } else if (contact.type.enabled) {
                            enabled = true;
                        }

                        if (enabled) {
                            if (contact.type == EnumMobs.PLAYER) {
                                this.handleMPplayer(contact);
                            }

                            if (contact.icon == null) {
                                this.tryCustomIcon(contact);
                            }

                            if (contact.icon == null) {
                                this.tryAutoIcon(contact);
                            }

                            if (contact.icon == null) {
                                this.getGenericIcon(contact);
                            }

                            if (contact.type == EnumMobs.HORSE) {
                                contact.setRotationFactor(45);
                            }

                            String scrubbedName = TextUtils.scrubCodes(contact.entity.getName().getString());
                            if ((scrubbedName.equals("Dinnerbone") || scrubbedName.equals("Grumm")) && (!(contact.entity instanceof Player) || ((Player) contact.entity).isModelPartShown(PlayerModelPart.CAPE))) {
                                contact.setRotationFactor(contact.rotationFactor + 180);
                            }

                            if (this.options.showHelmetsPlayers && contact.type == EnumMobs.PLAYER || this.options.showHelmetsMobs && contact.type != EnumMobs.PLAYER || contact.type == EnumMobs.SHEEP) {
                                this.getArmor(contact, entity);
                            }

                            this.contacts.add(contact);
                        }
                    }
                }
            } catch (Exception var16) {
                VoxelConstants.getLogger().error(var16.getLocalizedMessage(), var16);
            }
        }

        if (this.newMobs) {
            try {
                this.textureAtlas.stitchNew();
                applyFilteringParameters();
            } catch (StitcherException var14) {
                VoxelConstants.getLogger().warn("Stitcher exception!  Resetting mobs texture atlas.");
                this.loadTexturePackIcons();
            }
        }

        this.newMobs = false;
        this.contacts.sort(Comparator.comparingInt(contact -> contact.y));
    }

    private void tryCustomIcon(Contact contact) {
        String identifier = contact.vanillaType ? "minecraft." + contact.type.id : contact.entity.getClass().getName();
        String identifierSimple = contact.vanillaType ? contact.type.id : contact.entity.getClass().getSimpleName();
        Sprite icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(identifier + "custom");
        if (icon == this.textureAtlas.getMissingImage()) {
            boolean isHostile = this.isHostile(contact.entity);
            CustomMobsManager.add(contact.entity.getType().getDescriptionId(), isHostile, !isHostile);
            BufferedImage mobSkin = this.getCustomMobImage(identifier, identifierSimple);
            if (mobSkin != null) {
                icon = this.textureAtlas.registerIconForBufferedImage(identifier + "custom", mobSkin);
                this.newMobs = true;
                contact.icon = icon;
                contact.custom = true;
            } else {
                this.textureAtlas.registerFailedIcon(identifier + "custom");
            }
        } else if (icon != this.textureAtlas.getFailedImage()) {
            contact.custom = true;
            contact.icon = icon;
        }
    }

    private BufferedImage getCustomMobImage(String identifier, String identifierSimple) {
        BufferedImage mobSkin = null;

        try {
            int intendedSize = 8;
            String fullPath = ("textures/icons/" + identifier + ".png").toLowerCase();
            InputStream is = null;

            try {
                is = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
            } catch (IOException ignored) {
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifierSimple + ".png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {
                }
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifier + "8.png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {
                }
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifierSimple + "8.png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {
                }
            }

            if (is == null) {
                intendedSize = 16;
                fullPath = ("textures/icons/" + identifier + "16.png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {
                }
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifierSimple + "16.png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {
                }
            }

            if (is == null) {
                intendedSize = 32;
                fullPath = ("textures/icons/" + identifier + "32.png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {
                }
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifierSimple + "32.png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {
                }
            }

            if (is != null) {
                mobSkin = ImageIO.read(is);
                is.close();
                mobSkin = ImageUtils.validateImage(mobSkin);
                float scale = (float) mobSkin.getWidth() / intendedSize;
                mobSkin = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(mobSkin, 4.0F / scale)), this.options.outlines, 2);
            }
        } catch (Exception var16) {
            mobSkin = null;
        }

        return mobSkin;
    }

    private void tryAutoIcon(Contact contact) {
        EntityRenderer<LivingEntity, LivingEntityRenderState> render = (EntityRenderer<LivingEntity, LivingEntityRenderState>) VoxelConstants.getMinecraft().getEntityRenderDispatcher().getRenderer(contact.entity);
        ResourceLocation resourceLocation = ((LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) render).getTextureLocation(render.createRenderState());
        resourceLocation = this.getRandomizedResourceLocationForEntity(resourceLocation, contact.entity);
        ResourceLocation resourceLocationSecondary = null;
        ResourceLocation resourceLocationTertiary = null;
        ResourceLocation resourceLocationQuaternary = null;
        String color = "";
        if (contact.type == EnumMobs.MOOSHROOM) {
            if (!((MushroomCow) contact.entity).isBaby()) {
                resourceLocationSecondary = EnumMobs.MOOSHROOM.secondaryResourceLocation;
            }
        } else if (contact.type == EnumMobs.HORSE) {
            if (contact.entity instanceof Horse horse) {
                resourceLocationSecondary = ((HorseRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(horse)).getTextureLocation((HorseRenderState) render.createRenderState(horse, 0f));
                resourceLocationTertiary = (ResourceLocation) TEXTURES.get(horse.getMarkings());
                if (this.options.showHelmetsMobs) {
                    ItemStack itemStack = horse.getBodyArmorItem();
                    Item var30 = itemStack.getItem();
                    // if (var30 instanceof AnimalArmorItem horseArmorItem) {
                        // VoxelConstants.getMinecraft().getItemRenderer().getModel(itemStack, contact.entity.level(), horse, 0);
                        // resourceLocationQuaternary = horseArmorItem.getTexture();
                        // contact.setArmorColor(DyedItemColor.getOrDefault(itemStack, -1)); // FIXME
                    // }
                }
            }
        } else if (contact.type == EnumMobs.TROPICALFISHA || contact.type == EnumMobs.TROPICALFISHB) {
            TropicalFish fish = (TropicalFish) contact.entity;
            // FIXME
            // resourceLocationSecondary = fish.getVarietyId();
            // color = Arrays.toString(fish.getBaseColorComponents()) + " " + Arrays.toString(fish.getPatternColorComponents());
        } else if (contact.type == EnumMobs.VILLAGER || contact.type == EnumMobs.ZOMBIEVILLAGER) {
            String zombie = contact.type == EnumMobs.ZOMBIEVILLAGER ? "zombie_" : "";
            VillagerData villagerData = ((VillagerDataHolder) contact.entity).getVillagerData();
            VillagerType villagerType = villagerData.type().value();
            VillagerProfession villagerProfession = villagerData.profession().value();
            resourceLocationSecondary = BuiltInRegistries.VILLAGER_TYPE.getKey(villagerType);
            resourceLocationSecondary = ResourceLocation.fromNamespaceAndPath(resourceLocationSecondary.getNamespace(), "textures/entity/" + zombie + "villager/type/" + resourceLocationSecondary.getPath() + ".png");
            if (villagerProfession != BuiltInRegistries.VILLAGER_PROFESSION.getValue(VillagerProfession.NONE) && !contact.entity.isBaby()) {
                resourceLocationTertiary = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villagerProfession);
                resourceLocationTertiary = ResourceLocation.fromNamespaceAndPath(resourceLocationTertiary.getNamespace(), "textures/entity/" + zombie + "villager/profession/" + resourceLocationTertiary.getPath() + ".png");
                if (villagerProfession != BuiltInRegistries.VILLAGER_PROFESSION.getValue(VillagerProfession.NITWIT)) {
                    resourceLocationQuaternary = LEVEL_TO_ID.get(Mth.clamp(villagerData.level(), 1, LEVEL_TO_ID.size()));
                    resourceLocationQuaternary = ResourceLocation.fromNamespaceAndPath(resourceLocationQuaternary.getNamespace(), "textures/entity/" + zombie + "villager/profession_level/" + resourceLocationQuaternary.getPath() + ".png");
                }
            }

            VillagerMetadataSection.Hat biomeHatType = this.getHatType(resourceLocationSecondary);
            VillagerMetadataSection.Hat professionHatType = this.getHatType(resourceLocationTertiary);
            boolean showBiomeHat = professionHatType == VillagerMetadataSection.Hat.NONE || professionHatType == VillagerMetadataSection.Hat.PARTIAL && biomeHatType != VillagerMetadataSection.Hat.FULL;
            if (!showBiomeHat) {
                resourceLocationSecondary = null;
            }
        } else {
            resourceLocationSecondary = contact.type.secondaryResourceLocation;
        }

        if (resourceLocationSecondary != null) {
            resourceLocationSecondary = this.getRandomizedResourceLocationForEntity(resourceLocationSecondary, contact.entity);
        }

        if (resourceLocationTertiary != null) {
            resourceLocationTertiary = this.getRandomizedResourceLocationForEntity(resourceLocationTertiary, contact.entity);
        }

        if (resourceLocationQuaternary != null) {
            resourceLocationQuaternary = this.getRandomizedResourceLocationForEntity(resourceLocationQuaternary, contact.entity);
        }


        String entityName = contact.vanillaType ? "minecraft." + contact.type.id : contact.entity.getClass().getName();
        String resourceLocationString = (resourceLocation != null ? resourceLocation.toString() : "") + (resourceLocationSecondary != null ? resourceLocationSecondary.toString() : "");
        resourceLocationString = resourceLocationString + (resourceLocationTertiary != null ? resourceLocationTertiary.toString() : "") + (resourceLocationQuaternary != null ? resourceLocationQuaternary.toString() : "");
        resourceLocationString = resourceLocationString + (contact.armorColor != -1 ? contact.armorColor : "");
        String name = entityName + color + resourceLocationString;
        Sprite icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(name);
        if (icon == this.textureAtlas.getMissingImage()) {
            if (VoxelConstants.DEBUG) {
                VoxelConstants.getLogger().info("Radar: Creating icon: " + name);
            }
            Integer checkCount = this.contactsSkinGetTries.get(name);
            if (checkCount == null) {
                checkCount = 0;
            }

            BufferedImage mobImage = null;
            if (contact.type == EnumMobs.HORSE) {
                BufferedImage base = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
                if (resourceLocationSecondary != null && base != null) {
                    BufferedImage variant = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationSecondary);
                    variant = ImageUtils.scaleImage(variant, (float) base.getWidth() / variant.getWidth(), (float) base.getHeight() / variant.getHeight());
                    base = ImageUtils.addImages(base, variant, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                    variant.flush();
                }

                if (resourceLocationTertiary != null && base != null) {
                    BufferedImage pattern = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationTertiary);
                    pattern = ImageUtils.scaleImage(pattern, (float) base.getWidth() / pattern.getWidth(), (float) base.getHeight() / pattern.getHeight());
                    pattern = ImageUtils.colorify(pattern, contact.armorColor);
                    base = ImageUtils.addImages(base, pattern, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                    pattern.flush();
                }

                if (resourceLocationQuaternary != null && base != null) {
                    BufferedImage armor = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationQuaternary);
                    armor = ImageUtils.scaleImage(armor, (float) base.getWidth() / armor.getWidth(), (float) base.getHeight() / armor.getHeight());
                    armor = ImageUtils.colorify(armor, contact.armorColor);
                    base = ImageUtils.addImages(base, armor, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                    armor.flush();
                }

                mobImage = this.createImageFromTypeAndImages(contact.type, base, null, contact.entity);
                base.flush();
            } else if (contact.type.expectedWidth > 0.5) {
                mobImage = this.createImageFromTypeAndResourceLocations(contact.type, resourceLocation, resourceLocationSecondary, contact.entity);
            }

            if (mobImage != null) {
                mobImage = this.trimAndOutlineImage(contact, mobImage, false, true);
            } else {
                mobImage = this.createAutoIconImageFromResourceLocations(contact, render, resourceLocation, resourceLocationSecondary, resourceLocationTertiary, resourceLocationQuaternary);
            }

            if (mobImage != null) {
                // icon = cachedSprites.get(mobImage);
                // if (icon == null) {
                try {
                    icon = this.textureAtlas.registerIconForBufferedImage(name, mobImage);
                    contact.icon = icon;
                    this.newMobs = true;
                    this.contactsSkinGetTries.remove(name);
                } catch (Exception var16) {
                    checkCount = checkCount + 1;
                    if (checkCount > 4) {
                        this.textureAtlas.registerFailedIcon(name);
                        this.contactsSkinGetTries.remove(name);
                    } else {
                        this.contactsSkinGetTries.put(name, checkCount);
                    }
                }
                // } else {
                // contact.icon = icon;
                // this.contactsSkinGetTries.remove(name);
                // }
            } else {
                checkCount = checkCount + 1;
                if (checkCount > 4) {
                    this.textureAtlas.registerFailedIcon(name);
                    this.contactsSkinGetTries.remove(name);
                } else {
                    this.contactsSkinGetTries.put(name, checkCount);
                }
            }
        } else if (icon != this.textureAtlas.getFailedImage()) {
            contact.icon = icon;
        }

    }

    public VillagerMetadataSection.Hat getHatType(ResourceLocation resourceLocation) {
        VillagerMetadataSection.Hat hatType = VillagerMetadataSection.Hat.NONE;
        if (resourceLocation != null) {
            try {
                Optional<Resource> resource = VoxelConstants.getMinecraft().getResourceManager().getResource(resourceLocation);
                if (resource.isPresent()) {
                    VillagerMetadataSection villagerResourceMetadata = resource.get().metadata().getSection(VillagerMetadataSection.TYPE).orElse(null);
                    if (villagerResourceMetadata != null) {
                        hatType = villagerResourceMetadata.hat();
                    }

                    resource.get().openAsReader().close();
                }
            } catch (IOException | ClassCastException ignored) {
                hatType = VillagerMetadataSection.Hat.NONE;
            }
        }

        return hatType;
    }

    private BufferedImage createAutoIconImageFromResourceLocations(Contact contact, EntityRenderer<LivingEntity, LivingEntityRenderState> entityRenderer, ResourceLocation... resourceLocations) {
        Entity entity = contact.entity;
        EnumMobs type = contact.type;
        UUID entityUUID = contact.uuid;
        if (type != EnumMobs.UNKNOWN && entityIconMap.containsKey(entityUUID)) {
            return entityIconMap.get(entityUUID);
        }

        if (type == EnumMobs.UNKNOWN) {
            VoxelConstants.getLogger().info("Unknown Entity: " + entity.getType());
        }

        BufferedImage headImage = null;
        // boolean cachedImage = false;
        // List<ModelPartWithResourceLocation> bitsList = null;
        Model model = null;
        if (entityRenderer instanceof LivingEntityRenderer<?, ?, ?> render) {
            try {
                model = render.getModel();
                ArrayList<Field> submodels = ReflectionUtils.getFieldsByType(model, Model.class, ModelPart.class);
                ArrayList<Field> submodelArrays = ReflectionUtils.getFieldsByType(model, Model.class, ModelPart[].class);
                ModelPart[] headBits = null;
                ArrayList<ModelPartWithResourceLocation> headPartsWithResourceLocationList = new ArrayList<>();
                Properties properties = new Properties();
                String fullName = contact.vanillaType ? "minecraft." + type.id : entity.getClass().getName();
                String simpleName = contact.vanillaType ? type.id : entity.getClass().getSimpleName();
                String fullPath = ("textures/icons/" + fullName + ".properties").toLowerCase();

                ResourceManager resourceManager = VoxelConstants.getMinecraft().getResourceManager();
                Optional<Resource> resource = resourceManager.getResource(ResourceLocation.parse(fullPath));

                if (resource.isEmpty()) {
                    fullPath = ("textures/icons/" + simpleName + ".properties").toLowerCase();
                    resource = resourceManager.getResource(ResourceLocation.parse(fullPath));
                }
                if (resource.isPresent()) {
                    try (InputStream is = resource.get().open()) {
                        properties.load(is);
                        is.close();
                        String subModelNames = properties.getProperty("models", "").toLowerCase();
                        String[] submodelNamesArray = subModelNames.split(",");
                        List<String> subModelNamesList = Arrays.asList(submodelNamesArray);
                        HashSet<String> subModelNamesSet = new HashSet<>(subModelNamesList);
                        ArrayList<ModelPart> headPartsArrayList = new ArrayList<>();

                        for (Field submodelArray : submodelArrays) {
                            String name = submodelArray.getName().toLowerCase();
                            if (subModelNamesSet.contains(name) || subModelNames.equals("all")) {
                                ModelPart[] submodelArrayValue = (ModelPart[]) submodelArray.get(model);
                                if (submodelArrayValue != null) {
                                    Collections.addAll(headPartsArrayList, submodelArrayValue);
                                }
                            }
                        }

                        for (Field submodel : submodels) {
                            String name = submodel.getName().toLowerCase();
                            if ((subModelNamesSet.contains(name) || subModelNames.equals("all")) && submodel.get(model) != null) {
                                Object modelPartObjekt = submodel.get(model);
                                if (modelPartObjekt instanceof ModelPart modelPart) {
                                    headPartsArrayList.add(modelPart);
                                }
                            }
                        }

                        if (!headPartsArrayList.isEmpty()) {
                            headBits = headPartsArrayList.toArray(new ModelPart[0]);
                        }
                    }
                }

                if (headBits == null) {
                    if (model instanceof PlayerModel) {
                        boolean showHat = true;
                        if (entity instanceof Player player) {
                            showHat = player.isModelPartShown(PlayerModelPart.HAT);
                        }

                        if (showHat) {
                            headBits = new ModelPart[]{((PlayerModel) model).head, ((PlayerModel) model).hat};
                        } else {
                            headBits = new ModelPart[]{((PlayerModel) model).head};
                        }
                    } else if (type == EnumMobs.STRAY) {
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(((SkeletonModel<?>) model).head, resourceLocations[0]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(((SkeletonModel<?>) model).hat, resourceLocations[0]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(this.strayOverlayModel.head, resourceLocations[1]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(this.strayOverlayModel.hat, resourceLocations[1]));
                    } else if (type == EnumMobs.DROWNED) {
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(((DrownedModel) model).head, resourceLocations[0]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(((DrownedModel) model).hat, resourceLocations[0]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(this.drownedOverlayModel.head, resourceLocations[1]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(this.drownedOverlayModel.hat, resourceLocations[1]));
                    } else if (model instanceof AxolotlModel axolotlModel) {
                        headBits = new ModelPart[]{axolotlModel.head};
                    } else if (model instanceof BatModel batEntityModel) {
                        headBits = new ModelPart[]{batEntityModel.root().getChild("head")};
                    } else if (model instanceof BeeModel beeModel) {
                        headBits = new ModelPart[]{beeModel.bone.getChild("body")};
                    } else if (model instanceof HumanoidModel<?> bipedEntityModel) {
                        headBits = new ModelPart[]{bipedEntityModel.head, bipedEntityModel.hat};
                    } else if (model instanceof BlazeModel blazeEntityModel) {
                        headBits = new ModelPart[]{blazeEntityModel.root().getChild("head")};
                    } else if (model instanceof ChickenModel chickenModel) {
                        headBits = new ModelPart[]{chickenModel.head};
                    } else if (model instanceof CreeperModel creeperEntityModel) {
                        headBits = new ModelPart[]{creeperEntityModel.root().getChild("head")};
                    } else if (model instanceof DolphinModel dolphinEntityModel) {
                        headBits = new ModelPart[]{dolphinEntityModel.root().getChild("body").getChild("head")};
                    } else if (model instanceof EndermiteModel endermiteEntityModel) {
                        headBits = new ModelPart[]{endermiteEntityModel.root().getChild("segment0"), endermiteEntityModel.root().getChild("segment1")};
                    } else if (model instanceof GhastModel ghastEntityModel) {
                        headBits = new ModelPart[]{ghastEntityModel.root()};
                    } else if (model instanceof GuardianModel guardianEntityModel) {
                        headBits = new ModelPart[]{guardianEntityModel.root().getChild("head")};
                    } else if (model instanceof HoglinModel hoglinModel) {
                        headBits = new ModelPart[]{hoglinModel.head};
                    } else if (model instanceof HorseModel horseEntityModel) {
                        headBits = StreamSupport.stream(horseEntityModel.headParts.children.values().spliterator(), false).toArray(ModelPart[]::new);
                    } else if (model instanceof IllagerModel<?> illagerEntityModel) {
                        headBits = new ModelPart[]{illagerEntityModel.root().getChild("head")};
                    } else if (model instanceof IronGolemModel ironGolemEntityModel) {
                        headBits = new ModelPart[]{ironGolemEntityModel.root().getChild("head")};
                    } else if (model instanceof LavaSlimeModel lavaSlimeModel) {
                        headBits = lavaSlimeModel.bodyCubes;
                    } else if (model instanceof OcelotModel ocelotModel) {
                        headBits = new ModelPart[]{ocelotModel.head};
                    } else if (model instanceof PhantomModel phantomEntityModel) {
                        headBits = new ModelPart[]{phantomEntityModel.root().getChild("body")};
                    } else if (model instanceof RabbitModel rabbitModel) {
                        headBits = new ModelPart[] { rabbitModel.root().getChild("head") };
                    } else if (model instanceof RavagerModel ravagerEntityModel) {
                        headBits = new ModelPart[]{ravagerEntityModel.root().getChild("neck").getChild("head")};
                    } else if (model instanceof ShulkerModel shulkerEntityModel) {
                        headBits = new ModelPart[]{shulkerEntityModel.head};
                    } else if (model instanceof SilverfishModel silverFishEntityModel) {
                        headBits = new ModelPart[]{silverFishEntityModel.root().getChild("segment0"), silverFishEntityModel.root().getChild("segment1")};
                    } else if (model instanceof SlimeModel slimeEntityModel) {
                        headBits = new ModelPart[]{slimeEntityModel.root()};
                    } else if (model instanceof SnifferModel snifferModel) {
                        headBits = new ModelPart[]{snifferModel.root().getChild("bone").getChild("body").getChild("head")};
                    } else if (model instanceof SnowGolemModel snowGolemEntityModel) {
                        headBits = new ModelPart[]{snowGolemEntityModel.root().getChild("head")};
                    } else if (model instanceof SpiderModel spiderEntityModel) {
                        headBits = new ModelPart[]{spiderEntityModel.root().getChild("head"), spiderEntityModel.root().getChild("body0")};
                    } else if (model instanceof SquidModel squidEntityModel) {
                        headBits = new ModelPart[]{squidEntityModel.root().getChild("body")};
                    } else if (model instanceof WardenModel wardenEntityModel) {
                        headBits = new ModelPart[]{wardenEntityModel.root().getChild("bone").getChild("body").getChild("head")};
                    } else if (model instanceof StriderModel striderEntityModel) {
                        headBits = new ModelPart[]{striderEntityModel.root().getChild("body")};
                    } else if (model instanceof VillagerModel villagerResemblingModel) {
                        headBits = new ModelPart[]{villagerResemblingModel.getHead()};
                    } else if (model instanceof WolfModel wolfModel) {
                        headBits = new ModelPart[]{wolfModel.head};
                    } else if (model instanceof CamelModel camelModel) {
                        headBits = new ModelPart[] { camelModel.root().getChild("body").getChild("head") };
                    } else if (model instanceof EndermanModel endermanModel) {
                        headBits = new ModelPart[] { endermanModel.root().getChild("head") };
                        endermanModel.root().getChild("head").getChild("hat").visible = false;
                    } else if (model instanceof QuadrupedModel<?> quadrupedModel) {
                        headBits = new ModelPart[]{quadrupedModel.head};
                    } else if (model instanceof EntityModel<?> singlePartEntityModel) {
                        try {
                            headBits = new ModelPart[]{singlePartEntityModel.root().getChild("head")};
                        } catch (Exception ignored) {
                            try {
                                headBits = new ModelPart[] { singlePartEntityModel.root().getChild("body").getChild("head") };
                            } catch (Exception ignored2) {
                                try {
                                    headBits = new ModelPart[] { singlePartEntityModel.root().getChild("body") };
                                } catch (Exception ignored3) {
                                }
                            }
                        }
                    }
                }

                if (headBits == null) {
                    ArrayList<ModelPart> headPartsArrayList = new ArrayList<>();
                    ArrayList<ModelPart> purge = new ArrayList<>();

                    for (Field submodelArray : submodelArrays) {
                        String name = submodelArray.getName().toLowerCase();
                        if (name.contains("head") | name.contains("eye") | name.contains("mouth") | name.contains("teeth") | name.contains("tooth") | name.contains("tusk") | name.contains("jaw") | name.contains("mand") | name.contains("nose") | name.contains("beak") | name.contains("snout")
                                | name.contains("muzzle") | (!name.contains("rear") && name.contains("ear")) | name.contains("trunk") | name.contains("mane") | name.contains("horn") | name.contains("antler")) {
                            ModelPart[] submodelArrayValue = (ModelPart[]) submodelArray.get(model);
                            if (submodelArrayValue != null) {
                                headPartsArrayList.add(submodelArrayValue[0]);
                            }
                        }
                    }

                    for (Field submodel : submodels) {
                        String name = submodel.getName().toLowerCase();
                        String nameS = submodel.getName();
                        if (name.contains("head") | name.contains("eye") | name.contains("mouth") | name.contains("teeth") | name.contains("tooth") | name.contains("tusk") | name.contains("jaw") | name.contains("mand") | name.contains("nose") | name.contains("beak") | name.contains("snout")
                                | name.contains("muzzle") | (!name.contains("rear") && name.contains("ear")) | name.contains("trunk") | name.contains("mane") | name.contains("horn") | name.contains("antler") | nameS.equals("REar") | nameS.equals("Trout")
                                && !nameS.equals("LeftSmallEar") & !nameS.equals("RightSmallEar") & !nameS.equals("BHead") & !nameS.equals("BSnout") & !nameS.equals("BMouth") & !nameS.equals("BMouthOpen") & !nameS.equals("BLEar") & !nameS.equals("BREar") & !nameS.equals("CHead")
                                & !nameS.equals("CSnout") & !nameS.equals("CMouth") & !nameS.equals("CMouthOpen") & !nameS.equals("CLEar") & !nameS.equals("CREar")
                                && submodel.get(model) != null) {
                            headPartsArrayList.add((ModelPart) submodel.get(model));
                        }
                    }

                    if (headPartsArrayList.isEmpty()) {
                        int pos = model instanceof EntityModel ? 1 : 0;
                        if (submodels.size() > pos) {
                            if (submodels.get(pos).get(model) != null) {
                                headPartsArrayList.add((ModelPart) submodels.get(pos).get(model));
                            }
                        } else if (!submodelArrays.isEmpty() && submodelArrays.get(0).get(model) != null) {
                            ModelPart[] submodelArrayValue = (ModelPart[]) submodelArrays.get(0).get(model);
                            if (submodelArrayValue.length > 0) {
                                headPartsArrayList.add(submodelArrayValue[0]);
                            }
                        }
                    }

                    for (ModelPart bit : headPartsArrayList) {
                        purge.addAll(bit.children.values());
                    }

                    headPartsArrayList.removeAll(purge);
                    headBits = headPartsArrayList.toArray(new ModelPart[0]);
                }

                if ((headBits.length > 0 || !headPartsWithResourceLocationList.isEmpty()) && resourceLocations[0] != null) {
                    String scaleString = properties.getProperty("scale", "1");
                    float scale = Float.parseFloat(scaleString);
                    Direction facing = Direction.NORTH;
                    String facingString = properties.getProperty("facing", "front");
                    if (facingString.equals("top")) {
                        facing = Direction.UP;
                    } else if (facingString.equals("side")) {
                        facing = Direction.EAST;
                    }

                    ResourceLocation resourceLocation = this.combineResourceLocations(resourceLocations);
                    for (ModelPart headBit : headBits) {
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(headBit, resourceLocation));
                    }

                    ModelPartWithResourceLocation[] headBitsWithLocations = headPartsWithResourceLocationList.toArray(new ModelPartWithResourceLocation[0]);
                    // bitsList = Arrays.asList(headBitsWithLocations);
                    // headImage = cachedImages.get(bitsList);
                    // if (headImage == null) {
                    boolean success = this.drawModel(scale, 1000, (LivingEntity) entity, facing, model, headBitsWithLocations);
                    if (model instanceof EndermanModel endermanModel) {
                        endermanModel.root().getChild("head").getChild("hat").visible = true;
                    }
                    if (VoxelConstants.DEBUG) {
                        ImageUtils.saveImage(type.id, OpenGL.Utils.fboTextureId, 0, 512, 512);
                    }
                    if (success) {
                        headImage = ImageUtils.createBufferedImageFromGLID(OpenGL.Utils.fboTextureId);
                    }
                        // System.out.println("cache miss!");
                    // } else {
                    // cachedImage = true;
                    // // System.out.println("cache hit!");
                    // }
                }
            } catch (Exception exception) {
                VoxelConstants.getLogger().error(exception);
            }
        }
        // if (!cachedImage) {
        if (headImage != null) {
            headImage = this.trimAndOutlineImage(contact, headImage, true, model instanceof HumanoidModel);

            // cachedImages.put(bitsList, headImage);
        }
        // }

        entityIconMap.put(entityUUID, headImage);
        return headImage;
    }

    public static BufferedImage resizeBufferedImage(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return dimg;
    }

    private ResourceLocation combineResourceLocations(ResourceLocation... resourceLocations) {
        ResourceLocation resourceLocation = resourceLocations[0];
        if (resourceLocations.length > 1) {
            boolean hasAdditional = false;

            try {
                BufferedImage base = null;

                for (int t = 1; t < resourceLocations.length; ++t) {
                    if (resourceLocations[t] != null) {
                        if (!hasAdditional) {
                            base = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
                        }

                        hasAdditional = true;
                        BufferedImage overlay = ImageUtils.createBufferedImageFromResourceLocation(resourceLocations[t]);
                        float xScale = ((float) base.getWidth() / overlay.getWidth());
                        float yScale = ((float) base.getHeight() / overlay.getHeight());
                        if (xScale != 1.0F || yScale != 1.0F) {
                            overlay = ImageUtils.scaleImage(overlay, xScale, yScale);
                        }

                        ImageUtils.addImages(base, overlay, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                        overlay.flush();
                    }
                }

                if (hasAdditional) {
                    NativeImage nativeImage = OpenGL.Utils.nativeImageFromBufferedImage(base);
                    base.flush();
                    this.nativeBackedTexture.close();
                    this.nativeBackedTexture = new DynamicTexture(nativeImage);
                    OpenGL.Utils.register(this.nativeBackedTextureLocation, this.nativeBackedTexture);
                    resourceLocation = this.nativeBackedTextureLocation;
                }
            } catch (Exception var9) {
                VoxelConstants.getLogger().warn(var9);
            }
        }

        return resourceLocation;
    }

    private boolean drawModel(float scale, int captureDepth, LivingEntity livingEntity, Direction facing, Model model, ModelPartWithResourceLocation[] headBits) {
        boolean failed = false;
        float size = 64.0F * scale;
        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.Utils.fboTextureId);
        int width = OpenGL.glGetTexLevelParameteri(OpenGL.GL11_GL_TEXTURE_2D, 0, OpenGL.GL11_GL_TRANSFORM_BIT);
        int height = OpenGL.glGetTexLevelParameteri(OpenGL.GL11_GL_TEXTURE_2D, 0, OpenGL.GL11_GL_TEXTURE_HEIGHT);
        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, 0);
        OpenGL.glViewport(0, 0, width, height);
        Matrix4f minimapProjectionMatrix = RenderSystem.getProjectionMatrix();
        Matrix4f matrix4f = new Matrix4f().ortho(0.0F, width, height, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.ORTHOGRAPHIC);
        Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushMatrix();
        matrixStack.identity();
        matrixStack.translate(0.0f, 0.0f, -3000.0f + captureDepth);
        OpenGL.Utils.bindFramebuffer();
        OpenGL.glDepthMask(true);
        OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        OpenGL.glDisable(OpenGL.GL11_GL_CULL_FACE);
        OpenGL.glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
        OpenGL.glClearDepth(1.0);
        OpenGL.glClear(OpenGL.GL11_GL_COLOR_BUFFER_BIT | OpenGL.GL11_GL_DEPTH_BUFFER_BIT);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
        matrixStack.pushMatrix();
        matrixStack.translate(width / 2f, height / 2f, 0.0f);
        matrixStack.scale(size, size, size);
        matrixStack.rotate(Axis.ZP.rotationDegrees(180.0F));
        matrixStack.rotate(Axis.YP.rotationDegrees(180.0F));
        if (facing == Direction.EAST) {
            matrixStack.rotate(Axis.YP.rotationDegrees(-90.0F));
        } else if (facing == Direction.UP) {
            matrixStack.rotate(Axis.XP.rotationDegrees(90.0F));
        }

        Vector4f fullbright2 = new Vector4f(fullbright.x, fullbright.y, fullbright.z, 0);
        fullbright2.mul(matrixStack);
        Vector3f fullbright3 = new Vector3f(fullbright2.x, fullbright2.y, fullbright2.z);
        RenderSystem.setShaderLights(fullbright3, fullbright3);

        try {
            PoseStack newMatrixStack = new PoseStack();
            MultiBufferSource.BufferSource immediate = VoxelConstants.getMinecraft().renderBuffers().bufferSource();
            float offsetByY = model instanceof EndermanModel ? 8.0F : (!(model instanceof HumanoidModel) && !(model instanceof SkullModel) ? 0.0F : 4.0F);
            float maxY = 0.0F;
            float minY = 0.0F;

            for (ModelPartWithResourceLocation headBit : headBits) {
                if (headBit.modelPart.y < minY) {
                    minY = headBit.modelPart.y;
                }

                if (headBit.modelPart.y > maxY) {
                    maxY = headBit.modelPart.y;
                }
            }

            if (minY < -25.0F) {
                offsetByY = -25.0F - minY;
            } else if (maxY > 25.0F) {
                offsetByY = 25.0F - maxY;
            }

            for (ModelPartWithResourceLocation headBit : headBits) {
                VertexConsumer vertexConsumer = immediate.getBuffer(model.renderType(headBit.resourceLocation));
                if (model instanceof EntityModel entityModel) {
                    entityModel.setupAnim(VoxelConstants.getMinecraft().getEntityRenderDispatcher().getRenderer(livingEntity).createRenderState(livingEntity, 0));
                }

                float y = headBit.modelPart.y;
                float xRot = headBit.modelPart.xRot;
                float yRot = headBit.modelPart.yRot;
                float zRot = headBit.modelPart.zRot;
                headBit.modelPart.y += offsetByY;
                headBit.modelPart.xRot = 0;
                headBit.modelPart.yRot = 0;
                headBit.modelPart.zRot = 0;
                headBit.modelPart.render(newMatrixStack, vertexConsumer, 0xF000F0, OverlayTexture.NO_OVERLAY);
                headBit.modelPart.y = y;
                headBit.modelPart.xRot = xRot;
                headBit.modelPart.yRot = yRot;
                headBit.modelPart.zRot = zRot;

                immediate.endBatch();
            }
        } catch (Exception var25) {
            VoxelConstants.getLogger().warn("Error attempting to render head bits for " + livingEntity.getClass().getSimpleName(), var25);
            failed = true;
        }

        matrixStack.popMatrix();
        matrixStack.popMatrix();
        OpenGL.glEnable(OpenGL.GL11_GL_CULL_FACE);
        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glDepthMask(false);
        OpenGL.Utils.unbindFramebuffer();
        RenderSystem.setProjectionMatrix(minimapProjectionMatrix, ProjectionType.ORTHOGRAPHIC);
        OpenGL.glViewport(0, 0, VoxelConstants.getMinecraft().getWindow().getWidth(), VoxelConstants.getMinecraft().getWindow().getHeight());
        return !failed;
    }

    private void getGenericIcon(Contact contact) {
        contact.type = this.getUnknownMobNeutrality(contact.entity);
        String name = "minecraft." + contact.type.id + contact.type.resourceLocation.toString();
        contact.icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(name);
    }

    private ResourceLocation getRandomizedResourceLocationForEntity(ResourceLocation resourceLocation, Entity entity) {
        try {
            if (this.randomobsOptifine) {
                Object randomEntitiesProperties = this.mapProperties.get(resourceLocation.getPath());
                if (randomEntitiesProperties != null) {
                    this.setEntityMethod.invoke(this.randomEntityClass.cast(this.randomEntity), entity);
                    resourceLocation = (ResourceLocation) this.getEntityTextureMethod.invoke(this.randomEntitiesPropertiesClass.cast(randomEntitiesProperties), resourceLocation, this.randomEntityClass.cast(this.randomEntity));
                }
            }
        } catch (Exception ignored) {
        }

        return resourceLocation;
    }

    private BufferedImage trimAndOutlineImage(Contact contact, BufferedImage image, boolean auto, boolean centered) {
        if (auto) {
            image = centered ? ImageUtils.trimCentered(image) : ImageUtils.trim(image);
            double acceptableMax = 64.0;
            if (ImageUtils.percentageOfEdgePixelsThatAreSolid(image) < 30.0F) {
                acceptableMax = 128.0;
            }

            int maxDimension = Math.max(image.getWidth(), image.getHeight());
            float scale = (float) Math.ceil(maxDimension / acceptableMax);
            return ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(image, 1.0F / scale)), this.options.outlines, 2);
        } else {
            float scale = image.getWidth() / contact.type.expectedWidth;
            return ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(image, 4.0F / scale)), this.options.outlines, 2);
        }
    }

    private void handleMPplayer(Contact contact) {
        AbstractClientPlayer player = (AbstractClientPlayer) contact.entity;
        GameProfile gameProfile = player.getGameProfile();
        UUID uuid = gameProfile.getId();
        contact.setUUID(uuid);
        String playerName = this.scrubCodes(gameProfile.getName());
        Sprite icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(playerName);
        Integer checkCount;
        if (icon == this.textureAtlas.getMissingImage()) {
            checkCount = this.mpContactsSkinGetTries.get(playerName);
            if (checkCount == null) {
                checkCount = 0;
            }

            if (checkCount < 5) {
                AbstractTexture imageData; //TODO 1.21.4

                try {
                    ResourceLocation skinIdentifier = VoxelConstants.getMinecraft().getSkinManager().getInsecureSkin(player.getGameProfile()).texture();
                    if (skinIdentifier == DefaultPlayerSkin.get(player.getUUID()).texture()) {
                        throw new Exception("failed to get skin: skin is default");
                    }

                    imageData = VoxelConstants.getMinecraft().getTextureManager().getTexture(skinIdentifier);
                    if (imageData == null) {
                        throw new Exception("failed to get skin: image data was null");
                    }

                    EntityRenderer<LivingEntity, LivingEntityRenderState> render = (EntityRenderer<LivingEntity, LivingEntityRenderState>) VoxelConstants.getMinecraft().getEntityRenderDispatcher().getRenderer(contact.entity);
                    BufferedImage skinImage = this.createAutoIconImageFromResourceLocations(contact, render, skinIdentifier, null);
                    icon = this.textureAtlas.registerIconForBufferedImage(playerName, skinImage);
                    this.newMobs = true;
                    this.mpContactsSkinGetTries.remove(playerName);
                } catch (Exception var11) {
                    icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("minecraft." + EnumMobs.PLAYER.id + EnumMobs.PLAYER.resourceLocation.toString());
                    checkCount = checkCount + 1;
                    this.mpContactsSkinGetTries.put(playerName, checkCount);
                }

                contact.icon = icon;
            }
        } else {
            contact.icon = icon;
        }

    }

    private void getArmor(Contact contact, Entity entity) {
        Sprite icon = null;
        ItemStack stack = ((LivingEntity) entity).getItemBySlot(EquipmentSlot.HEAD);
        Item helmet = null;
        if (stack != null && stack.getCount() > 0) {
            helmet = stack.getItem();
        }

        if (contact.type == EnumMobs.SHEEP) {
            Sheep sheepEntity = (Sheep) contact.entity;
            if (!sheepEntity.isSheared()) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("sheepfur");
                int sheepColors = Sheep.getColor(sheepEntity.getColor());
                contact.setArmorColor(sheepColors);
            }
        } else if (helmet != null) {
            if (helmet == Items.SKELETON_SKULL) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("minecraft." + EnumMobs.SKELETON.id + EnumMobs.SKELETON.resourceLocation.toString() + "head");
            } else if (helmet == Items.WITHER_SKELETON_SKULL) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("minecraft." + EnumMobs.SKELETONWITHER.id + EnumMobs.SKELETONWITHER.resourceLocation.toString() + "head");
            } else if (helmet == Items.ZOMBIE_HEAD) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("minecraft." + EnumMobs.ZOMBIE.id + EnumMobs.ZOMBIE.resourceLocation.toString() + "head");
            } else if (helmet == Items.CREEPER_HEAD) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("minecraft." + EnumMobs.CREEPER.id + EnumMobs.CREEPER.resourceLocation.toString() + "head");
            } else if (helmet == Items.DRAGON_HEAD) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("minecraft." + EnumMobs.ENDERDRAGON.id + EnumMobs.ENDERDRAGON.resourceLocation.toString() + "head");
            } else if (helmet == Items.PLAYER_HEAD) {
                GameProfile gameProfile = null;
                ResolvableProfile profileComponent = stack.get(DataComponents.PROFILE);
                if (profileComponent != null && profileComponent.isResolved()) {
                    gameProfile = profileComponent.gameProfile();
                }

                ResourceLocation resourceLocation = DefaultPlayerSkin.getDefaultTexture();
                if (gameProfile != null) {
                    resourceLocation = VoxelConstants.getMinecraft().getSkinManager().getInsecureSkin(gameProfile).texture();
                }

                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("minecraft." + EnumMobs.PLAYER.id + resourceLocation.toString() + "head");
                if (icon == this.textureAtlas.getMissingImage()) {
                    ModelPart outer = this.playerSkullModel.head;
                    ModelPartWithResourceLocation[] headBits = {new ModelPartWithResourceLocation(outer, resourceLocation)};
                    boolean success = this.drawModel(1.1875F, 1000, contact.entity, Direction.NORTH, this.playerSkullModel, headBits);
                    if (success) {
                        BufferedImage headImage = ImageUtils.createBufferedImageFromGLID(OpenGL.Utils.fboTextureId);
                        headImage = this.trimAndOutlineImage(new Contact(VoxelConstants.getPlayer(), EnumMobs.PLAYER), headImage, true, true);
                        icon = this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.PLAYER.id + resourceLocation + "head", headImage);
                        this.newMobs = true;
                    }
                }
            } /*else if (helmet instanceof Armor helmetArmor) {
                int armorType = this.getArmorType(helmetArmor);
                if (armorType != UNKNOWN) {
                    icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("armor " + this.armorNames[armorType]);
                } else {
                    boolean isPiglin = contact.type == EnumMobs.PIGLIN || contact.type == EnumMobs.PIGLINZOMBIE;
                    icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("armor " + helmet.getDescriptionId() + (isPiglin ? "_piglin" : ""));
                    if (icon == this.textureAtlas.getMissingImage()) {
                        icon = this.createUnknownArmorIcons(contact, stack, helmet);
                    } else if (icon == this.textureAtlas.getFailedImage()) {
                        icon = null;
                    }
                }

                contact.setArmorColor(DyedItemColor.getOrDefault(stack, -1));
                // FIXME 1.21.4
                // } else if (helmet instanceof BlockItem blockItem) {
                // Block block = blockItem.getBlock();
                // BlockState blockState = block.defaultBlockState();
                // int stateID = Block.getId(blockState);
                // icon = this.textureAtlas.getAtlasSprite("blockArmor " + stateID);
                // if (icon == this.textureAtlas.getMissingImage()) {
                // BufferedImage blockImage = VoxelConstants.getVoxelMapInstance().getColorManager().getBlockImage(blockState, stack, entity.level(), 4.9473686F, -8.0F);
                // if (blockImage != null) {
                // int width = blockImage.getWidth();
                // int height = blockImage.getHeight();
                // ImageUtils.eraseArea(blockImage, width / 2 - 15, height / 2 - 15, 30, 30, width, height);
                // BufferedImage blockImageFront = VoxelConstants.getVoxelMapInstance().getColorManager().getBlockImage(blockState, stack, entity.level(), 4.9473686F, 7.25F);
                // blockImageFront = blockImageFront.getSubimage(width / 2 - 15, height / 2 - 15, 30, 30);
                // ImageUtils.addImages(blockImage, blockImageFront, (width / 2f - 15), (height / 2f - 15), width, height);
                // blockImageFront.flush();
                // blockImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.trimCentered(blockImage)), this.options.outlines, true, 37.6F, 37.6F, 2);
                // icon = this.textureAtlas.registerIconForBufferedImage("blockArmor " + stateID, blockImage);
                // this.newMobs = true;
                // }
                // }
            }*/
        }

        contact.armorIcon = icon;
    }

    private Sprite createUnknownArmorIcons(Contact contact, ItemStack stack, Item helmet) {
        Sprite icon = null;
        boolean isPiglin = contact.type == EnumMobs.PIGLIN || contact.type == EnumMobs.PIGLINZOMBIE;

        ResourceLocation resourceLocation = null;

        try {
            String materialName = helmet.builtInRegistryHolder().getRegisteredName(); // FIXME 1.21.2 ???
            String domain = "minecraft";
            int sep = materialName.indexOf(58);
            if (sep != -1) {
                domain = materialName.substring(0, sep);
                materialName = materialName.substring(sep + 1);
                if (materialName.startsWith("leather")) {
                    materialName = "leather";
                } else if (materialName.startsWith("iron")) {
                    materialName = "iron";
                } else if (materialName.startsWith("gold")) {
                    materialName = "gold";
                } else if (materialName.startsWith("diamond")) {
                    materialName = "diamond";
                } else if (materialName.startsWith("netherite")) {
                    materialName = "netherite";
                } else if (materialName.startsWith("chainmail")) {
                    materialName = "chainmail";
                } else if (materialName.startsWith("turtle")) {
                    materialName = "turtle_scute";
                }
            }
            // System.out.println("createUnknownArmorIconsmaterialName materialName " + materialName);

            String resourcePath = String.format("%s:textures/entity/equipment/humanoid/%s.png", domain, materialName);

            resourceLocation = ResourceLocation.parse(resourcePath);
        } catch (RuntimeException ignored) {
        }

        HumanoidModel<HumanoidRenderState> modelBiped;

        float intendedWidth = 9.0F;
        float intendedHeight = 9.0F;

        if (isPiglin) {
            modelBiped = this.piglinArmorModel;
            intendedWidth = 11.5F;
        } else {
            modelBiped = this.bipedArmorModel;
        }

        if (modelBiped != null && resourceLocation != null) {
            ModelPartWithResourceLocation[] headBitsWithResourceLocation = {new ModelPartWithResourceLocation(modelBiped.head, resourceLocation), new ModelPartWithResourceLocation(modelBiped.hat, resourceLocation)};
            this.drawModel(1.0F, 2, contact.entity, Direction.NORTH, modelBiped, headBitsWithResourceLocation);
            BufferedImage armorImage = ImageUtils.createBufferedImageFromGLID(OpenGL.Utils.fboTextureId);
            armorImage = armorImage.getSubimage(200, 200, 112, 112);
            armorImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.trimCentered(armorImage)), this.options.outlines, true, intendedWidth * 4.0F, intendedHeight * 4.0F, 2);
            icon = this.textureAtlas.registerIconForBufferedImage("armor " + helmet.getDescriptionId() + (isPiglin ? "_piglin" : ""), armorImage);
            this.newMobs = true;
        }

        if (icon == null && resourceLocation != null) {
            BufferedImage armorTexture = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
            if (armorTexture != null) {
                if (!isPiglin) {
                    armorTexture = ImageUtils.addImages(ImageUtils.loadImage(armorTexture, 8, 8, 8, 8), ImageUtils.loadImage(armorTexture, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
                    float scale = armorTexture.getWidth() / 8.0F;
                    BufferedImage armorImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(armorTexture, 4.0F / scale * 47.0F / 38.0F)), this.options.outlines, true, 37.6F, 37.6F, 2);
                    icon = this.textureAtlas.registerIconForBufferedImage("armor " + resourceLocation, armorImage);
                } else {
                    armorTexture = ImageUtils.addImages(ImageUtils.loadImage(armorTexture, 8, 8, 8, 8), ImageUtils.loadImage(armorTexture, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
                    float scale = armorTexture.getWidth() / 8.0F;
                    BufferedImage armorImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(armorTexture, 4.0F / scale * 47.0F / 38.0F)), this.options.outlines, true, 47.0F, 37.6F, 2);
                    icon = this.textureAtlas.registerIconForBufferedImage("armor " + resourceLocation + "_piglin", armorImage);
                }

                this.newMobs = true;
            }
        }

        if (icon == null) {
            VoxelConstants.getLogger().warn("can't get texture for custom armor type: " + helmet.getClass());
            this.textureAtlas.registerFailedIcon("armor " + helmet.getDescriptionId() + helmet.getClass().getName());
        }

        return icon;
    }

    private String scrubCodes(String string) {
        return string.replaceAll("(\\xA7.)", "");
    }

    private EnumMobs getUnknownMobNeutrality(Entity entity) {
        if (this.isHostile(entity)) {
            return EnumMobs.GENERICHOSTILE;
        } else {
            if (entity instanceof TamableAnimal tameableEntity) {
                if (tameableEntity.isTame() && (VoxelConstants.getMinecraft().hasSingleplayerServer() || tameableEntity.getOwner().equals(VoxelConstants.getPlayer()))) {
                    return EnumMobs.GENERICTAME;
                }
            }

            return EnumMobs.GENERICNEUTRAL;
        }
    }

    /*private int getArmorType(AnimalArmorItem helmet) {
        return helmet.getDescriptionId().equals("item.minecraft.leather_helmet") ? 0 : UNKNOWN;
    }*/

    public void renderMapMobs(GuiGraphics drawContext, Matrix4fStack matrixStack, int x, int y, float scaleProj) {
        double max = this.layoutVariables.zoomScaleAdjusted * 32.0;
        double lastX = GameVariableAccessShim.xCoordDouble();
        double lastZ = GameVariableAccessShim.zCoordDouble();
        int lastY = GameVariableAccessShim.yCoord();

        for (Contact contact : this.contacts) {
            RenderSystem.setShader(CoreShaders.POSITION_TEX);
            OpenGL.Utils.disp2(this.textureAtlas.getId());
            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);

            RenderSystem.setShader(CoreShaders.POSITION_TEX);
            contact.updateLocation();
            double contactX = contact.x;
            double contactZ = contact.z;
            int contactY = contact.y;
            double wayX = lastX - contactX;
            double wayZ = lastZ - contactZ;
            int wayY = lastY - contactY;
            double entityMax = max;
            if (contact.type == EnumMobs.PHANTOM) {
                entityMax *= 2;
            }
            double adjustedDiff = entityMax - Math.max(Math.abs(wayY), 0);
            contact.brightness = (float) Math.max(adjustedDiff / entityMax, 0.0);
            contact.brightness *= contact.brightness;
            contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
            contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ) / this.layoutVariables.zoomScaleAdjusted;
            if (wayY < 0) {
                OpenGL.glColor4f(1.0F, 1.0F, 1.0F, contact.brightness);
            } else {
                if (contact.brightness < 0.3f) {
                    contact.brightness = 0.3f;
                }
                OpenGL.glColor3f(contact.brightness, contact.brightness, contact.brightness);
            }

            if (this.minimapOptions.rotates) {
                contact.angle += this.direction;
            } else if (this.minimapOptions.oldNorth) {
                contact.angle -= 90.0F;
            }

            boolean inRange;
            if (!this.minimapOptions.squareMap) {
                inRange = contact.distance < 31.0;
            } else {
                double radLocate = Math.toRadians(contact.angle);
                double dispX = contact.distance * Math.cos(radLocate);
                double dispY = contact.distance * Math.sin(radLocate);
                inRange = Math.abs(dispX) <= 28.5 && Math.abs(dispY) <= 28.5;
            }

            if (inRange) {
                try {
                    matrixStack.pushMatrix();
                    if (this.options.filtering) {
                        matrixStack.translate(x, y, 0.0f);
                        matrixStack.rotate(Axis.ZP.rotationDegrees(-contact.angle));
                        matrixStack.translate(0.0f, (float) -contact.distance, 0.0f);
                        matrixStack.rotate(Axis.ZP.rotationDegrees(contact.angle + contact.rotationFactor));
                        matrixStack.translate((-x), (-y), 0.0f);
                    } else {
                        wayX = Math.sin(Math.toRadians(contact.angle)) * contact.distance;
                        wayZ = Math.cos(Math.toRadians(contact.angle)) * contact.distance;
                        matrixStack.translate((float) Math.round(-wayX * this.layoutVariables.scScale) / this.layoutVariables.scScale, (float) Math.round(-wayZ * this.layoutVariables.scScale) / this.layoutVariables.scScale, 0.0f);
                    }

                    float yOffset = 0.0F;
                    if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                        yOffset = -4.0F;
                    }

                    if (Stream.of(EnumMobs.GHAST, EnumMobs.GHASTATTACKING, EnumMobs.WITHER, EnumMobs.WITHERINVULNERABLE, EnumMobs.VEX, EnumMobs.VEXCHARGING, EnumMobs.PUFFERFISH, EnumMobs.PUFFERFISHHALF, EnumMobs.PUFFERFISHFULL).anyMatch(enumMobs -> contact.type == enumMobs)) {
                        if (contact.type != EnumMobs.GHAST && contact.type != EnumMobs.GHASTATTACKING) {
                            if (contact.type != EnumMobs.WITHER && contact.type != EnumMobs.WITHERINVULNERABLE) {
                                if (contact.type != EnumMobs.VEX && contact.type != EnumMobs.VEXCHARGING) {
                                    int size = ((Pufferfish) contact.entity).getPuffState();
                                    switch (size) {
                                        case 0 -> contact.type = EnumMobs.PUFFERFISH;
                                        case 1 -> contact.type = EnumMobs.PUFFERFISHHALF;
                                        case 2 -> contact.type = EnumMobs.PUFFERFISHFULL;
                                    }
                                } else {
                                    if (contact.entity instanceof Vex vex) {
                                        contact.type = vex.isCharging() ? EnumMobs.VEXCHARGING : EnumMobs.VEX;
                                    }
                                }
                            } else {
                                if (contact.entity instanceof WitherBoss witherBoss) {
                                    contact.type = witherBoss.getInvulnerableTicks() > 0 ? EnumMobs.WITHERINVULNERABLE : EnumMobs.WITHER;
                                }
                            }
                        } else {
                            if (contact.entity instanceof Ghast ghast) {
                                contact.type = ghast.isCharging() ? EnumMobs.GHASTATTACKING : EnumMobs.GHAST;
                            }
                        }

                        this.tryAutoIcon(contact);
                        this.tryCustomIcon(contact);
                        if (this.newMobs) {
                            try {
                                this.textureAtlas.stitchNew();
                            } catch (StitcherException var45) {
                                VoxelConstants.getLogger().warn("Stitcher exception in render method!  Resetting mobs texture atlas.");
                                this.loadTexturePackIcons();
                            }

                            OpenGL.Utils.disp2(this.textureAtlas.getId());
                        }

                        this.newMobs = false;
                    }

                    // this.applyFilteringParameters();
                    OpenGL.Utils.drawPre();
                    OpenGL.Utils.setMap(contact.icon, x, y + yOffset, ((int) (contact.icon.getIconWidth() / 4.0F)));
                    OpenGL.Utils.drawPost();
                    if ((this.options.showHelmetsPlayers && contact.type == EnumMobs.PLAYER || this.options.showHelmetsMobs && contact.type != EnumMobs.PLAYER || contact.type == EnumMobs.SHEEP) && contact.armorIcon != null) {
                        Sprite icon = contact.armorIcon;
                        float armorOffset = 0.0F;
                        if (contact.type == EnumMobs.ZOMBIEVILLAGER) {
                            armorOffset = -0.5F;
                        }

                        float armorScale = 1.0F;
                        float red = 1.0F;
                        float green = 1.0F;
                        float blue = 1.0F;
                        if (contact.armorColor != -1) {
                            red = (contact.armorColor >> 16 & 0xFF) / 255.0F;
                            green = (contact.armorColor >> 8 & 0xFF) / 255.0F;
                            blue = (contact.armorColor & 0xFF) / 255.0F;
                            if (contact.type == EnumMobs.SHEEP) {
                                Sheep sheepEntity = (Sheep) contact.entity;
                                if (sheepEntity.hasCustomName() && "jeb_".equals(sheepEntity.getName().getString())) {
                                    int semiRandom = sheepEntity.tickCount / 25 + sheepEntity.getId();
                                    int numDyeColors = DyeColor.values().length;
                                    int colorID1 = semiRandom % numDyeColors;
                                    int colorID2 = (semiRandom + 1) % numDyeColors;
                                    float lerpVal = ((sheepEntity.tickCount % 25) + VoxelConstants.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false)) / 25.0F;
                                    Color sheepColors1 = new Color(Sheep.getColor(DyeColor.byId(colorID1)));
                                    Color sheepColors2 = new Color(Sheep.getColor(DyeColor.byId(colorID2)));
                                    red = (sheepColors1.getRed() * (1.0F - lerpVal) + sheepColors2.getRed() * lerpVal) / 255.0F;
                                    green = (sheepColors1.getGreen() * (1.0F - lerpVal) + sheepColors2.getGreen() * lerpVal) / 255.0F;
                                    blue = (sheepColors1.getBlue() * (1.0F - lerpVal) + sheepColors2.getBlue() * lerpVal) / 255.0F;
                                }

                                armorScale = 1.04F;
                            }

                            if (wayY < 0) {
                                OpenGL.glColor4f(red, green, blue, contact.brightness);
                            } else {
                                OpenGL.glColor3f(red * contact.brightness, green * contact.brightness, blue * contact.brightness);
                            }
                        }

                        // this.applyFilteringParameters();
                        OpenGL.Utils.drawPre();
                        OpenGL.Utils.setMap(icon, x, y + yOffset + armorOffset, ((int) (icon.getIconWidth() / 4.0F * armorScale)));
                        OpenGL.Utils.drawPost();
                        if (icon == this.clothIcon) {
                            if (wayY < 0) {
                                OpenGL.glColor4f(1.0F, 1.0F, 1.0F, contact.brightness);
                            } else {
                                OpenGL.glColor3f(contact.brightness, contact.brightness, contact.brightness);
                            }

                            icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("armor " + this.armorNames[2]);
                            // this.applyFilteringParameters();
                            OpenGL.Utils.drawPre();
                            OpenGL.Utils.setMap(icon, x, y + yOffset + armorOffset, icon.getIconWidth() / 4.0F * armorScale);
                            OpenGL.Utils.drawPost();
                            if (wayY < 0) {
                                OpenGL.glColor4f(red, green, blue, contact.brightness);
                            } else {
                                OpenGL.glColor3f(red * contact.brightness, green * contact.brightness, blue * contact.brightness);
                            }

                            icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("armor " + this.armorNames[1]);
                            // this.applyFilteringParameters();
                            OpenGL.Utils.drawPre();
                            OpenGL.Utils.setMap(icon, x, y + yOffset + armorOffset, icon.getIconWidth() / 4.0F * armorScale * 40.0F / 37.0F);
                            OpenGL.Utils.drawPost();
                            OpenGL.glColor3f(1.0F, 1.0F, 1.0F);
                            icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("armor " + this.armorNames[3]);
                            // this.applyFilteringParameters();
                            OpenGL.Utils.drawPre();
                            OpenGL.Utils.setMap(icon, x, y + yOffset + armorOffset, icon.getIconWidth() / 4.0F * armorScale * 40.0F / 37.0F);
                            OpenGL.Utils.drawPost();
                        }
                    }

                    if (contact.name != null && ((this.options.showPlayerNames && contact.type == EnumMobs.PLAYER) || (this.options.showMobNames && contact.type != EnumMobs.PLAYER && contact.entity.hasCustomName()))) {

                        float scaleFactor = this.layoutVariables.scScale / this.options.fontScale;
                        matrixStack.scale(1.0F / scaleFactor, 1.0F / scaleFactor, 1.0F);

                        String name = contact.entity.getDisplayName().getString();
                        int m = VoxelConstants.getMinecraft().font.width(name) / 2;

                        PoseStack textMatrixStack = drawContext.pose();
                        textMatrixStack.pushPose();
                        textMatrixStack.setIdentity();
                        textMatrixStack.scale(scaleProj, scaleProj, 1.0F);

                        if (this.options.filtering) {
                            textMatrixStack.translate(x, y, 0.0f);
                            textMatrixStack.last().pose().rotate(Axis.ZP.rotationDegrees(-contact.angle));
                            textMatrixStack.translate(0.0f, (float) -contact.distance, 0.0f);
                            textMatrixStack.last().pose().rotate(Axis.ZP.rotationDegrees(contact.angle + contact.rotationFactor));
                            textMatrixStack.translate((-x), (-y), 0.0f);
                        } else {
                            wayX = Math.sin(Math.toRadians(contact.angle)) * contact.distance;
                            wayZ = Math.cos(Math.toRadians(contact.angle)) * contact.distance;
                            textMatrixStack.translate((float) Math.round(-wayX * this.layoutVariables.scScale) / this.layoutVariables.scScale, (float) Math.round(-wayZ * this.layoutVariables.scScale) / this.layoutVariables.scScale, 0.0f);
                        }

                        textMatrixStack.translate(0, 0, 900);
                        textMatrixStack.scale(1.0F / scaleFactor, 1.0F / scaleFactor, 1.0F);
                        drawContext.drawString(VoxelConstants.getMinecraft().font, name, (int) (x * scaleFactor - m), (int) ((y + 3) * scaleFactor), 0xffffffff, false);
                        textMatrixStack.popPose();
                    }
                } catch (Exception e) {
                    VoxelConstants.getLogger().error("Error rendering mob icon! " + e.getLocalizedMessage() + " contact type " + contact.type, e);
                } finally {
                    matrixStack.popMatrix();
                }
            }
        }
    }

    private void applyFilteringParameters() {
        if (this.options.filtering) {
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_S, OpenGL.GL12_GL_CLAMP_TO_EDGE);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_T, OpenGL.GL12_GL_CLAMP_TO_EDGE);
        } else {
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_NEAREST);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_NEAREST);
        }

    }

    private boolean isHostile(Entity entity) {
        if (entity instanceof ZombifiedPiglin zombifiedPiglinEntity) {
            return zombifiedPiglinEntity.getPersistentAngerTarget() != null && zombifiedPiglinEntity.getPersistentAngerTarget().equals(VoxelConstants.getPlayer().getUUID());
        } else if (entity instanceof Enemy) {
            return true;
        } else if (entity instanceof Bee beeEntity) {
            return beeEntity.isAngry();
        } else {
            if (entity instanceof PolarBear polarBearEntity) {
                for (PolarBear object : polarBearEntity.level().getEntitiesOfClass(PolarBear.class, polarBearEntity.getBoundingBox().inflate(8.0, 4.0, 8.0))) {
                    if (object.isBaby()) {
                        return true;
                    }
                }
            }

            if (entity instanceof Rabbit rabbitEntity) {
                return rabbitEntity.getVariant() == Rabbit.Variant.EVIL;
            } else if (entity instanceof Wolf wolfEntity) {
                return wolfEntity.isAngry();
            } else {
                return false;
            }
        }
    }

    private boolean isPlayer(Entity entity) {
        return entity instanceof RemotePlayer;
    }

    private boolean isNeutral(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        } else {
            return !(entity instanceof Player) && !this.isHostile(entity);
        }
    }

    private record ModelPartWithResourceLocation(ModelPart modelPart, ResourceLocation resourceLocation) {
    }
}
