package com.mamiyaotaru.voxelmap;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import com.mamiyaotaru.voxelmap.textures.FontRendererWithAtlas;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.StitcherException;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.CustomMob;
import com.mamiyaotaru.voxelmap.util.CustomMobsManager;
import com.mamiyaotaru.voxelmap.util.EnumMobs;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.ReflectionUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.feature.VillagerResourceMetadata;
import net.minecraft.client.render.entity.model.AxolotlEntityModel;
import net.minecraft.client.render.entity.model.BatEntityModel;
import net.minecraft.client.render.entity.model.BeeEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.BlazeEntityModel;
import net.minecraft.client.render.entity.model.ChickenEntityModel;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
import net.minecraft.client.render.entity.model.DolphinEntityModel;
import net.minecraft.client.render.entity.model.DrownedEntityModel;
import net.minecraft.client.render.entity.model.EndermanEntityModel;
import net.minecraft.client.render.entity.model.EndermiteEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.GhastEntityModel;
import net.minecraft.client.render.entity.model.GuardianEntityModel;
import net.minecraft.client.render.entity.model.HoglinEntityModel;
import net.minecraft.client.render.entity.model.HorseEntityModel;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.client.render.entity.model.IronGolemEntityModel;
import net.minecraft.client.render.entity.model.MagmaCubeEntityModel;
import net.minecraft.client.render.entity.model.OcelotEntityModel;
import net.minecraft.client.render.entity.model.PhantomEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.QuadrupedEntityModel;
import net.minecraft.client.render.entity.model.RabbitEntityModel;
import net.minecraft.client.render.entity.model.RavagerEntityModel;
import net.minecraft.client.render.entity.model.ShulkerEntityModel;
import net.minecraft.client.render.entity.model.SilverfishEntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.render.entity.model.SkeletonEntityModel;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import net.minecraft.client.render.entity.model.SlimeEntityModel;
import net.minecraft.client.render.entity.model.SnowGolemEntityModel;
import net.minecraft.client.render.entity.model.SpiderEntityModel;
import net.minecraft.client.render.entity.model.SquidEntityModel;
import net.minecraft.client.render.entity.model.StriderEntityModel;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.client.render.entity.model.WolfEntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeableArmorItem;
import net.minecraft.item.DyeableHorseArmorItem;
import net.minecraft.item.HorseArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerDataContainer;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
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
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.StreamSupport;

public class Radar implements IRadar {
    private MinecraftClient game;
    private IVoxelMap master = null;
    private LayoutVariables layoutVariables = null;
    public MapSettingsManager minimapOptions = null;
    public RadarSettingsManager options = null;
    private FontRendererWithAtlas fontRenderer;
    private TextureAtlas textureAtlas;
    private boolean newMobs = false;
    private boolean enabled = true;
    private boolean completedLoading = false;
    private int timer = 500;
    private float direction = 0.0F;
    private ArrayList<Contact> contacts = new ArrayList(40);
    public HashMap mpContactsSkinGetTries = new HashMap();
    public HashMap contactsSkinGetTries = new HashMap();
    private Sprite clothIcon = null;
    private static final int CLOTH = 0;
    private static final int UNKNOWN = EnumMobs.UNKNOWN.ordinal();
    private String[] armorNames = new String[]{"cloth", "clothOverlay", "clothOuter", "clothOverlayOuter", "chain", "iron", "gold", "diamond", "netherite", "turtle"};
    private final float iconScale = 4.0F;
    private boolean randomobsOptifine = false;
    private Class randomEntitiesClass = null;
    private Field mapPropertiesField = null;
    private java.util.Map mapProperties = null;
    private Field randomEntityField = null;
    private Object randomEntity = null;
    private Class iRandomEntityClass = null;
    private Class randomEntityClass = null;
    private Method setEntityMethod = null;
    private Class randomEntitiesPropertiesClass = null;
    private Method getEntityTextureMethod = null;
    private boolean hasCustomNPCs = false;
    private Class entityCustomNpcClass = null;
    private Class modelDataClass = null;
    private Class entityNPCInterfaceClass = null;
    private Field modelDataField = null;
    private Method getEntityMethod = null;
    private boolean lastOutlines = true;
    UUID devUUID = UUID.fromString("9b37abb9-2487-4712-bb96-21a1e0b2023c");
    private SkullEntityModel playerSkullModel;
    private BipedEntityModel bipedArmorModel;
    private SkeletonEntityModel strayOverlayModel;
    private DrownedEntityModel drownedOverlayModel;
    private BipedEntityModel piglinArmorModel;
    private NativeImageBackedTexture nativeBackedTexture = new NativeImageBackedTexture(2, 2, false);
    private final Identifier nativeBackedTextureLocation = new Identifier("voxelmap", "tempimage");
    private final Vec3f fullbright = new Vec3f(1.0F, 1.0F, 1.0F);

    private final Logger logger = org.apache.logging.log4j.LogManager.getLogger();

    private static final Int2ObjectMap LEVEL_TO_ID = (Int2ObjectMap) Util.make(new Int2ObjectOpenHashMap(), int2ObjectOpenHashMap -> {
        int2ObjectOpenHashMap.put(1, new Identifier("stone"));
        int2ObjectOpenHashMap.put(2, new Identifier("iron"));
        int2ObjectOpenHashMap.put(3, new Identifier("gold"));
        int2ObjectOpenHashMap.put(4, new Identifier("emerald"));
        int2ObjectOpenHashMap.put(5, new Identifier("diamond"));
    });
    private static final java.util.Map TEXTURES = (java.util.Map) Util.make(Maps.newEnumMap(HorseMarking.class), enumMap -> {
        enumMap.put(HorseMarking.NONE, (Object) null);
        enumMap.put(HorseMarking.WHITE, new Identifier("textures/entity/horse/horse_markings_white.png"));
        enumMap.put(HorseMarking.WHITE_FIELD, new Identifier("textures/entity/horse/horse_markings_whitefield.png"));
        enumMap.put(HorseMarking.WHITE_DOTS, new Identifier("textures/entity/horse/horse_markings_whitedots.png"));
        enumMap.put(HorseMarking.BLACK_DOTS, new Identifier("textures/entity/horse/horse_markings_blackdots.png"));
    });

    public Radar(IVoxelMap master) {
        this.master = master;
        this.minimapOptions = master.getMapOptions();
        this.options = master.getRadarOptions();
        this.game = MinecraftClient.getInstance();
        this.fontRenderer = new FontRendererWithAtlas(this.game.getTextureManager(), new Identifier("textures/font/ascii.png"));
        this.textureAtlas = new TextureAtlas("mobs");
        this.textureAtlas.setFilter(false, false);

        try {
            this.randomEntitiesClass = Class.forName("net.optifine.RandomEntities");
            this.mapPropertiesField = this.randomEntitiesClass.getDeclaredField("mapProperties");
            this.mapPropertiesField.setAccessible(true);
            this.mapProperties = (java.util.Map) this.mapPropertiesField.get((Object) null);
            this.randomEntityField = this.randomEntitiesClass.getDeclaredField("randomEntity");
            this.randomEntityField.setAccessible(true);
            this.randomEntity = this.randomEntityField.get((Object) null);
            this.iRandomEntityClass = Class.forName("net.optifine.IRandomEntity");
            this.randomEntityClass = Class.forName("net.optifine.RandomEntity");
            Class[] argClasses1 = new Class[]{Entity.class};
            this.setEntityMethod = this.randomEntityClass.getDeclaredMethod("setEntity", argClasses1);
            this.randomEntitiesPropertiesClass = Class.forName("net.optifine.RandomEntityProperties");
            Class[] argClasses2 = new Class[]{Identifier.class, this.iRandomEntityClass};
            this.getEntityTextureMethod = this.randomEntitiesPropertiesClass.getDeclaredMethod("getTextureLocation", argClasses2);
            this.randomobsOptifine = true;
        } catch (ClassNotFoundException var7) {
            this.randomobsOptifine = false;
        } catch (NoSuchMethodException var8) {
            this.randomobsOptifine = false;
        } catch (NoSuchFieldException var9) {
            this.randomobsOptifine = false;
        } catch (SecurityException var10) {
            this.randomobsOptifine = false;
        } catch (IllegalArgumentException var11) {
            this.randomobsOptifine = false;
        } catch (IllegalAccessException var12) {
            this.randomobsOptifine = false;
        }

        try {
            this.entityCustomNpcClass = Class.forName("noppes.npcs.entity.EntityCustomNpc");
            this.modelDataClass = Class.forName("noppes.npcs.ModelData");
            this.modelDataField = this.entityCustomNpcClass.getField("modelData");
            this.entityNPCInterfaceClass = Class.forName("noppes.npcs.entity.EntityNPCInterface");
            this.getEntityMethod = this.modelDataClass.getMethod("getEntity", this.entityNPCInterfaceClass);
            this.hasCustomNPCs = true;
        } catch (ClassNotFoundException var4) {
            this.hasCustomNPCs = false;
        } catch (NoSuchFieldException var5) {
            this.hasCustomNPCs = false;
        } catch (NoSuchMethodException var6) {
            this.hasCustomNPCs = false;
        }

    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.loadTexturePackIcons();
        this.fontRenderer.onResourceManagerReload(resourceManager);
    }

    private void loadTexturePackIcons() {
        this.completedLoading = false;

        try {
            this.mpContactsSkinGetTries.clear();
            this.contactsSkinGetTries.clear();
            this.textureAtlas.reset();
            TexturedModelData texturedModelData12 = SkullEntityModel.getHeadTexturedModelData();
            ModelPart skullModelPart = texturedModelData12.createModel();
            this.playerSkullModel = new SkullEntityModel(skullModelPart);
            Dilation ARMOR_DILATION = new Dilation(1.0F);
            TexturedModelData texturedModelData2 = TexturedModelData.of(BipedEntityModel.getModelData(ARMOR_DILATION, 0.0F), 64, 32);
            ModelPart bipedArmorModelPart = texturedModelData2.createModel();
            this.bipedArmorModel = new BipedEntityModel(bipedArmorModelPart);
            TexturedModelData strayModelData = TexturedModelData.of(BipedEntityModel.getModelData(new Dilation(0.25F), 0.0F), 64, 32);
            ModelPart strayOverlayModelPart = strayModelData.createModel();
            this.strayOverlayModel = new SkeletonEntityModel(strayOverlayModelPart);
            TexturedModelData drownedModelData = DrownedEntityModel.getTexturedModelData(new Dilation(0.25F));
            ModelPart drownedOverlayModelPart = drownedModelData.createModel();
            this.drownedOverlayModel = new DrownedEntityModel(drownedOverlayModelPart);
            TexturedModelData texturedModelData3 = TexturedModelData.of(BipedEntityModel.getModelData(new Dilation(1.02F), 0.0F), 64, 32);
            ModelPart piglinArmorModelPart = texturedModelData3.createModel();
            this.piglinArmorModel = new BipedEntityModel(piglinArmorModelPart);
            if (ReflectionUtils.classExists("com.prupe.mcpatcher.mob.MobOverlay") && ImageUtils.loadImage(new Identifier("mcpatcher/mob/cow/mooshroom_overlay.png"), 0, 0, 1, 1) != null) {
                EnumMobs.MOOSHROOM.secondaryResourceLocation = new Identifier("mcpatcher/mob/cow/mooshroom_overlay.png");
            } else {
                EnumMobs.MOOSHROOM.secondaryResourceLocation = new Identifier("textures/block/red_mushroom.png");
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
                    if ((double) EnumMobs.values()[t].expectedWidth > 0.5) {
                        mobImage = this.createImageFromTypeAndResourceLocations(EnumMobs.values()[t], EnumMobs.values()[t].resourceLocation, EnumMobs.values()[t].secondaryResourceLocation, (Entity) null);
                        if (mobImage != null) {
                            float scale = (float) mobImage.getWidth() / EnumMobs.values()[t].expectedWidth;
                            mobImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(mobImage, 4.0F / scale)), this.options.outlines, 2);
                            this.textureAtlas.registerIconForBufferedImage(spriteName, mobImage);
                        }
                    }
                }
            }

            BufferedImage[] armorImages = new BufferedImage[]{ImageUtils.loadImage(new Identifier("textures/models/armor/leather_layer_1.png"), 8, 8, 8, 8), ImageUtils.loadImage(new Identifier("textures/models/armor/leather_layer_1.png"), 40, 8, 8, 8), ImageUtils.loadImage(new Identifier("textures/models/armor/leather_layer_1_overlay.png"), 8, 8, 8, 8), ImageUtils.loadImage(new Identifier("textures/models/armor/leather_layer_1_overlay.png"), 40, 8, 8, 8)};

            for (int t = 0; t < armorImages.length; ++t) {
                float scale = (float) armorImages[t].getWidth() / 8.0F;
                armorImages[t] = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(armorImages[t], 4.0F / scale * 47.0F / 38.0F)), this.options.outlines && t != 2 && t != 3, true, 37.6F, 37.6F, 2);
                Sprite icon = this.textureAtlas.registerIconForBufferedImage("armor " + this.armorNames[t], armorImages[t]);
                if (t == 0) {
                    this.clothIcon = icon;
                }
            }

            BufferedImage zombie = ImageUtils.loadImage(EnumMobs.ZOMBIE.resourceLocation, 8, 8, 8, 8, 64, 64);
            float scale = (float) zombie.getWidth() / 8.0F;
            zombie = ImageUtils.scaleImage(zombie, 4.0F / scale * 47.0F / 38.0F);
            BufferedImage zombieHat = ImageUtils.loadImage(EnumMobs.ZOMBIE.resourceLocation, 40, 8, 8, 8, 64, 64);
            zombieHat = ImageUtils.scaleImage(zombieHat, 4.0F / scale * 47.0F / 35.0F);
            zombie = ImageUtils.addImages(ImageUtils.addImages(new BufferedImage(zombieHat.getWidth(), zombieHat.getHeight() + 8, 6), zombie, (float) (zombieHat.getWidth() - zombie.getWidth()) / 2.0F, (float) (zombieHat.getHeight() - zombie.getHeight()) / 2.0F, zombieHat.getWidth(), zombieHat.getHeight() + 8), zombieHat, 0.0F, 0.0F, zombieHat.getWidth(), zombieHat.getHeight() + 8);
            zombieHat.flush();
            zombie = ImageUtils.fillOutline(ImageUtils.pad(zombie), this.options.outlines, true, 37.6F, 37.6F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.ZOMBIE.id + EnumMobs.ZOMBIE.resourceLocation.toString() + "head", zombie);
            BufferedImage skeleton = ImageUtils.loadImage(EnumMobs.SKELETON.resourceLocation, 8, 8, 8, 8, 64, 32);
            scale = (float) skeleton.getWidth() / 8.0F;
            skeleton = ImageUtils.scaleImage(skeleton, 4.0F / scale * 47.0F / 38.0F);
            skeleton = ImageUtils.addImages(new BufferedImage(skeleton.getWidth(), skeleton.getHeight() + 8, 6), skeleton, 0.0F, 0.0F, skeleton.getWidth(), skeleton.getHeight() + 8);
            skeleton = ImageUtils.fillOutline(ImageUtils.pad(skeleton), this.options.outlines, true, 37.6F, 37.6F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.SKELETON.id + EnumMobs.SKELETON.resourceLocation.toString() + "head", skeleton);
            BufferedImage witherSkeleton = ImageUtils.loadImage(EnumMobs.SKELETONWITHER.resourceLocation, 8, 8, 8, 8, 64, 32);
            scale = (float) witherSkeleton.getWidth() / 8.0F;
            witherSkeleton = ImageUtils.scaleImage(witherSkeleton, 4.0F / scale * 47.0F / 38.0F);
            witherSkeleton = ImageUtils.addImages(new BufferedImage(witherSkeleton.getWidth(), witherSkeleton.getHeight() + 8, 6), witherSkeleton, 0.0F, 0.0F, witherSkeleton.getWidth(), witherSkeleton.getHeight() + 8);
            witherSkeleton = ImageUtils.fillOutline(ImageUtils.pad(witherSkeleton), this.options.outlines, true, 37.6F, 37.6F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.SKELETONWITHER.id + EnumMobs.SKELETONWITHER.resourceLocation.toString() + "head", witherSkeleton);
            BufferedImage creeper = ImageUtils.addImages(ImageUtils.blankImage(EnumMobs.CREEPER.resourceLocation, 8, 10), ImageUtils.loadImage(EnumMobs.CREEPER.resourceLocation, 8, 8, 8, 8), 0.0F, 0.0F, 8, 10);
            scale = (float) creeper.getWidth() / EnumMobs.CREEPER.expectedWidth;
            creeper = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(creeper, 4.0F / scale * 47.0F / 38.0F)), this.options.outlines, true, 37.6F, 37.6F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.CREEPER.id + EnumMobs.CREEPER.resourceLocation.toString() + "head", creeper);
            BufferedImage dragon = this.createImageFromTypeAndResourceLocations(EnumMobs.ENDERDRAGON, EnumMobs.ENDERDRAGON.resourceLocation, (Identifier) null, (Entity) null);
            scale = (float) dragon.getWidth() / EnumMobs.ENDERDRAGON.expectedWidth;
            dragon = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(dragon, 4.0F / scale)), this.options.outlines, true, 32.0F, 32.0F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.ENDERDRAGON.id + EnumMobs.ENDERDRAGON.resourceLocation.toString() + "head", dragon);
            BufferedImage sheepFur = ImageUtils.loadImage(new Identifier("textures/entity/sheep/sheep_fur.png"), 6, 6, 6, 6);
            scale = (float) sheepFur.getWidth() / 6.0F;
            sheepFur = ImageUtils.scaleImage(sheepFur, 4.0F / scale * 1.0625F);
            int chop = (int) Math.max(1.0F, 2.0F);
            sheepFur = ImageUtils.eraseArea(sheepFur, chop, chop, sheepFur.getWidth() - chop * 2, sheepFur.getHeight() - chop * 2, sheepFur.getWidth(), sheepFur.getHeight());
            sheepFur = ImageUtils.fillOutline(ImageUtils.pad(sheepFur), this.options.outlines, true, 27.5F, 27.5F, (int) Math.max(1.0F, 2.0F));
            this.textureAtlas.registerIconForBufferedImage("sheepfur", sheepFur);
            BufferedImage crown = ImageUtils.loadImage(new Identifier("voxelmap", "images/radar/crown.png"), 0, 0, 16, 16, 16, 16);
            crown = ImageUtils.fillOutline(ImageUtils.scaleImage(crown, 2.0F), this.options.outlines, true, 32.0F, 32.0F, 2);
            this.textureAtlas.registerIconForBufferedImage("crown", crown);
            BufferedImage glow = ImageUtils.loadImage(new Identifier("voxelmap", "images/radar/glow.png"), 0, 0, 16, 16, 16, 16);
            glow = ImageUtils.fillOutline(glow, this.options.outlines, true, 32.0F, 32.0F, 2);
            this.textureAtlas.registerIconForBufferedImage("glow", glow);
            Identifier fontResourceLocation = new Identifier("textures/font/ascii.png");
            BufferedImage fontImage = ImageUtils.loadImage(fontResourceLocation, 0, 0, 128, 128, 128, 128);
            if (fontImage.getWidth() > 512 || fontImage.getHeight() > 512) {
                int maxDim = Math.max(fontImage.getWidth(), fontImage.getHeight());
                float scaleBy = 512.0F / (float) maxDim;
                fontImage = ImageUtils.scaleImage(fontImage, scaleBy);
            }

            fontImage = ImageUtils.addImages(new BufferedImage(fontImage.getWidth() + 2, fontImage.getHeight() + 2, fontImage.getType()), fontImage, 1.0F, 1.0F, fontImage.getWidth() + 2, fontImage.getHeight() + 2);
            Sprite fontSprite = this.textureAtlas.registerIconForBufferedImage(fontResourceLocation.toString(), fontImage);
            Identifier blankResourceLocation = new Identifier("voxelmap", "images/radar/solid.png");
            BufferedImage blankImage = ImageUtils.loadImage(blankResourceLocation, 0, 0, 8, 8, 8, 8);
            Sprite blankSprite = this.textureAtlas.registerIconForBufferedImage(blankResourceLocation.toString(), blankImage);
            this.fontRenderer.setSprites(fontSprite, blankSprite);
            this.fontRenderer.setFontRef(this.textureAtlas.getGlId());
            this.textureAtlas.stitch();
            this.completedLoading = true;
        } catch (Exception var30) {
            System.err.println("Failed getting mobs " + var30.getLocalizedMessage());
            var30.printStackTrace();
        }

    }

    private BufferedImage createImageFromTypeAndResourceLocations(EnumMobs type, Identifier resourceLocation, Identifier resourceLocationSecondary, Entity entity) {
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
            case GENERICHOSTILE:
                image = ImageUtils.loadImage(new Identifier("voxelmap", "images/radar/hostile.png"), 0, 0, 16, 16, 16, 16);
                break;
            case GENERICNEUTRAL:
                image = ImageUtils.loadImage(new Identifier("voxelmap", "images/radar/neutral.png"), 0, 0, 16, 16, 16, 16);
                break;
            case GENERICTAME:
                image = ImageUtils.loadImage(new Identifier("voxelmap", "images/radar/tame.png"), 0, 0, 16, 16, 16, 16);
                break;
            case BAT:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 12, 64, 64), ImageUtils.loadImage(mobImage, 25, 1, 3, 4), 0.0F, 0.0F, 8, 12), ImageUtils.flipHorizontal(ImageUtils.loadImage(mobImage, 25, 1, 3, 4)), 5.0F, 0.0F, 8, 12), ImageUtils.loadImage(mobImage, 6, 6, 6, 6), 1.0F, 3.0F, 8, 12);
                break;
            case CHICKEN:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.loadImage(mobImage, 2, 3, 6, 6), ImageUtils.loadImage(mobImage, 16, 2, 4, 2), 1.0F, 2.0F, 6, 6), ImageUtils.loadImage(mobImage, 16, 6, 2, 2), 2.0F, 4.0F, 6, 6);
                break;
            case COD:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 16, 5, 32, 32), ImageUtils.loadImage(mobImage, 15, 3, 1, 3, 32, 32), 1.0F, 1.0F, 16, 5), ImageUtils.loadImage(mobImage, 16, 3, 3, 4, 32, 32), 2.0F, 1.0F, 16, 5), ImageUtils.loadImage(mobImage, 9, 7, 7, 4, 32, 32), 5.0F, 1.0F, 16, 5), ImageUtils.loadImage(mobImage, 26, 7, 4, 4, 32, 32), 12.0F, 1.0F, 16, 5), ImageUtils.loadImage(mobImage, 26, 0, 6, 1, 32, 32), 4.0F, 0.0F, 16, 5);
                break;
            case ENDERDRAGON:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 16, 20, 256, 256), ImageUtils.loadImage(mobImage, 128, 46, 16, 16, 256, 256), 0.0F, 4.0F, 16, 16), ImageUtils.loadImage(mobImage, 192, 60, 12, 5, 256, 256), 2.0F, 11.0F, 16, 16), ImageUtils.loadImage(mobImage, 192, 81, 12, 4, 256, 256), 2.0F, 16.0F, 16, 16), ImageUtils.loadImage(mobImage, 6, 6, 2, 4, 256, 256), 3.0F, 0.0F, 16, 16), ImageUtils.flipHorizontal(ImageUtils.loadImage(mobImage, 6, 6, 2, 4, 256, 256)), 11.0F, 0.0F, 16, 16);
                break;
            case GHAST:
                image = ImageUtils.loadImage(mobImage, 16, 16, 16, 16);
                break;
            case GHASTATTACKING:
                image = ImageUtils.loadImage(mobImage, 16, 16, 16, 16);
                break;
            case GUARDIAN:
                image = ImageUtils.scaleImage(ImageUtils.addImages(ImageUtils.loadImage(mobImage, 16, 16, 12, 12), ImageUtils.loadImage(mobImage, 9, 1, 2, 2), 5.0F, 5.5F, 12, 12), 0.5F);
                break;
            case GUARDIANELDER:
                image = ImageUtils.addImages(ImageUtils.loadImage(mobImage, 16, 16, 12, 12), ImageUtils.loadImage(mobImage, 9, 1, 2, 2), 5.0F, 5.5F, 12, 12);
                break;
            case HORSE:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 16, 24, 64, 64), ImageUtils.loadImage(mobImage, 56, 38, 2, 16, 64, 64), 1.0F, 7.0F, 16, 24), ImageUtils.loadImage(mobImage, 0, 42, 7, 12, 64, 64), 3.0F, 12.0F, 16, 24), ImageUtils.loadImage(mobImage, 0, 20, 7, 5, 64, 64), 3.0F, 7.0F, 16, 24), ImageUtils.loadImage(mobImage, 0, 30, 5, 5, 64, 64), 10.0F, 7.0F, 16, 24), ImageUtils.loadImage(mobImage, 19, 17, 1, 3, 64, 64), 3.0F, 4.0F, 16, 24), ImageUtils.loadImage(mobImage, 0, 13, 1, 7, 64, 64), 3.0F, 0.0F, 16, 24);
                break;
            case IRONGOLEM:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 12, 128, 128), ImageUtils.loadImage(mobImage, 8, 8, 8, 10, 128, 128), 0.0F, 1.0F, 8, 12), ImageUtils.loadImage(mobImage, 26, 2, 2, 4, 128, 128), 3.0F, 8.0F, 8, 12);
                break;
            case LLAMA:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 14, 128, 64), ImageUtils.loadImage(mobImage, 6, 20, 8, 8, 128, 64), 0.0F, 3.0F, 8, 14), ImageUtils.loadImage(mobImage, 9, 9, 4, 4, 128, 64), 2.0F, 5.0F, 8, 14), ImageUtils.loadImage(mobImage, 19, 2, 3, 3, 128, 64), 0.0F, 0.0F, 8, 14), ImageUtils.loadImage(mobImage, 19, 2, 3, 3, 128, 64), 5.0F, 0.0F, 8, 14);
                break;
            case LLAMATRADER:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 14, 128, 64), ImageUtils.loadImage(mobImage, 6, 20, 8, 8, 128, 64), 0.0F, 3.0F, 8, 14), ImageUtils.loadImage(mobImage, 9, 9, 4, 4, 128, 64), 2.0F, 5.0F, 8, 14), ImageUtils.loadImage(mobImage, 19, 2, 3, 3, 128, 64), 0.0F, 0.0F, 8, 14), ImageUtils.loadImage(mobImage, 19, 2, 3, 3, 128, 64), 5.0F, 0.0F, 8, 14);
                break;
            case MAGMA:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.loadImage(mobImage, 8, 8, 8, 8), ImageUtils.loadImage(mobImage, 32, 18, 8, 1), 0.0F, 3.0F, 8, 8), ImageUtils.loadImage(mobImage, 32, 27, 8, 1), 0.0F, 4.0F, 8, 8);
                break;
            case MOOSHROOM:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 40, 40), ImageUtils.loadImage(mobImage, 6, 6, 8, 8), 16.0F, 16.0F, 40, 40), ImageUtils.loadImage(mobImage, 23, 1, 1, 3), 15.0F, 15.0F, 40, 40), ImageUtils.loadImage(mobImage, 23, 1, 1, 3), 24.0F, 15.0F, 40, 40);
                if (mobImageSecondary != null) {
                    BufferedImage mushroomImage;
                    if (mobImageSecondary.getWidth() != mobImageSecondary.getHeight()) {
                        mushroomImage = ImageUtils.loadImage(mobImageSecondary, 32, 0, 16, 16, 48, 16);
                    } else {
                        mushroomImage = ImageUtils.loadImage(mobImageSecondary, 0, 0, 16, 16, 16, 16);
                    }

                    float ratio = (float) image.getWidth() / (float) mushroomImage.getWidth();
                    if ((double) ratio < 2.5) {
                        image = ImageUtils.scaleImage(image, 2.5F / ratio);
                    } else if ((double) ratio > 2.5) {
                        mushroomImage = ImageUtils.scaleImage(mushroomImage, ratio / 2.5F);
                    }

                    image = ImageUtils.addImages(image, mushroomImage, 12.0F, 0.0F, 40, 40);
                }
                break;
            case PARROT:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 8, 32, 32), ImageUtils.loadImage(mobImage, 2, 22, 3, 5, 32, 32), 1.0F, 0.0F, 8, 8), ImageUtils.loadImage(mobImage, 10, 4, 4, 1, 32, 32), 2.0F, 4.0F, 8, 8), ImageUtils.loadImage(mobImage, 2, 4, 2, 3, 32, 32), 2.0F, 5.0F, 8, 8), ImageUtils.loadImage(mobImage, 11, 8, 1, 2, 32, 32), 4.0F, 5.0F, 8, 8), ImageUtils.loadImage(mobImage, 16, 8, 1, 2, 32, 32), 5.0F, 5.0F, 8, 8);
                break;
            case PHANTOM:
                image = ImageUtils.addImages(ImageUtils.loadImage(mobImage, 5, 5, 7, 3, 64, 64), ImageUtils.loadImage(mobImageSecondary, 5, 5, 7, 3, 64, 64), 0.0F, 0.0F, 7, 3);
                break;
            case PUFFERFISH:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 3, 3, 32, 32), ImageUtils.loadImage(mobImage, 3, 30, 3, 2, 32, 32), 0.0F, 1.0F, 3, 3), ImageUtils.loadImage(mobImage, 3, 29, 1, 1, 32, 32), 0.0F, 0.0F, 3, 3), ImageUtils.loadImage(mobImage, 5, 29, 1, 1, 32, 32), 2.0F, 0.0F, 3, 3);
                break;
            case PUFFERFISHHALF:
                image = ImageUtils.loadImage(mobImage, 17, 27, 5, 5, 32, 32);
                break;
            case PUFFERFISHFULL:
                image = ImageUtils.loadImage(mobImage, 8, 8, 8, 8, 32, 32);
                break;
            case SALMON:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 26, 7, 32, 32), ImageUtils.loadImage(mobImage, 27, 3, 3, 4, 32, 32), 1.0F, 2.5F, 26, 7), ImageUtils.loadImage(mobImage, 11, 8, 8, 5, 32, 32), 4.0F, 2.0F, 26, 7), ImageUtils.loadImage(mobImage, 11, 21, 8, 5, 32, 32), 12.0F, 2.0F, 26, 7), ImageUtils.loadImage(mobImage, 26, 16, 6, 5, 32, 32), 20.0F, 2.0F, 26, 7), ImageUtils.loadImage(mobImage, 0, 0, 2, 2, 32, 32), 10.0F, 0.0F, 26, 7), ImageUtils.loadImage(mobImage, 5, 6, 3, 2, 32, 32), 12.0F, 0.0F, 26, 7);
                break;
            case SLIME:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 8), ImageUtils.loadImage(mobImage, 6, 22, 6, 6), 1.0F, 1.0F, 8, 8), ImageUtils.loadImage(mobImage, 34, 6, 2, 2), 5.0F, 2.0F, 8, 8), ImageUtils.loadImage(mobImage, 34, 2, 2, 2), 1.0F, 2.0F, 8, 8), ImageUtils.loadImage(mobImage, 33, 9, 1, 1), 4.0F, 5.0F, 8, 8), ImageUtils.loadImage(mobImage, 8, 8, 8, 8), 0.0F, 0.0F, 8, 8);
                break;
            case TROPICALFISHA:
                float[] primaryColorsA = new float[]{0.9765F, 0.502F, 0.1137F};
                float[] secondaryColorsA = new float[]{0.9765F, 1.0F, 0.9961F};
                if (entity != null && entity instanceof TropicalFishEntity) {
                    TropicalFishEntity fish = (TropicalFishEntity) entity;
                    primaryColorsA = fish.getBaseColorComponents();
                    secondaryColorsA = fish.getPatternColorComponents();
                }

                BufferedImage baseA = ImageUtils.colorify(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 10, 6, 32, 32), ImageUtils.loadImage(mobImage, 8, 6, 6, 3, 32, 32), 0.0F, 3.0F, 10, 6), ImageUtils.loadImage(mobImage, 17, 1, 5, 3, 32, 32), 1.0F, 0.0F, 10, 6), ImageUtils.loadImage(mobImage, 28, 0, 4, 3, 32, 32), 6.0F, 3.0F, 10, 6), primaryColorsA[0], primaryColorsA[1], primaryColorsA[2]);
                BufferedImage patternA = ImageUtils.colorify(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImageSecondary, 10, 6, 32, 32), ImageUtils.loadImage(mobImageSecondary, 8, 6, 6, 3, 32, 32), 0.0F, 3.0F, 10, 6), ImageUtils.loadImage(mobImageSecondary, 17, 1, 5, 3, 32, 32), 1.0F, 0.0F, 10, 6), ImageUtils.loadImage(mobImageSecondary, 28, 0, 4, 3, 32, 32), 6.0F, 3.0F, 10, 6), secondaryColorsA[0], secondaryColorsA[1], secondaryColorsA[2]);
                image = ImageUtils.addImages(baseA, patternA, 0.0F, 0.0F, 10, 6);
                baseA.flush();
                patternA.flush();
                break;
            case TROPICALFISHB:
                float[] primaryColorsB = new float[]{0.5373F, 0.1961F, 0.7216F};
                float[] secondaryColorsB = new float[]{0.9961F, 0.8471F, 0.2392F};
                if (entity != null && entity instanceof TropicalFishEntity) {
                    TropicalFishEntity fish = (TropicalFishEntity) entity;
                    primaryColorsB = fish.getBaseColorComponents();
                    secondaryColorsB = fish.getPatternColorComponents();
                }

                BufferedImage baseB = ImageUtils.colorify(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 12, 12, 32, 32), ImageUtils.loadImage(mobImage, 0, 26, 6, 6, 32, 32), 6.0F, 3.0F, 12, 12), ImageUtils.loadImage(mobImage, 20, 21, 6, 6, 32, 32), 0.0F, 3.0F, 12, 12), ImageUtils.loadImage(mobImage, 20, 18, 5, 3, 32, 32), 6.0F, 0.0F, 12, 12), ImageUtils.loadImage(mobImage, 20, 27, 5, 3, 32, 32), 6.0F, 9.0F, 12, 12), primaryColorsB[0], primaryColorsB[1], primaryColorsB[2]);
                BufferedImage patternB = ImageUtils.colorify(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImageSecondary, 12, 12, 32, 32), ImageUtils.loadImage(mobImageSecondary, 0, 26, 6, 6, 32, 32), 6.0F, 3.0F, 12, 12), ImageUtils.loadImage(mobImageSecondary, 20, 21, 6, 6, 32, 32), 0.0F, 3.0F, 12, 12), ImageUtils.loadImage(mobImageSecondary, 20, 18, 5, 3, 32, 32), 6.0F, 0.0F, 12, 12), ImageUtils.loadImage(mobImageSecondary, 20, 27, 5, 3, 32, 32), 6.0F, 9.0F, 12, 12), secondaryColorsB[0], secondaryColorsB[1], secondaryColorsB[2]);
                image = ImageUtils.addImages(baseB, patternB, 0.0F, 0.0F, 12, 12);
                baseB.flush();
                patternB.flush();
                break;
            case WITHER:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 24, 10, 64, 64), ImageUtils.loadImage(mobImage, 8, 8, 8, 8, 64, 64), 8.0F, 0.0F, 24, 10), ImageUtils.loadImage(mobImage, 38, 6, 6, 6, 64, 64), 0.0F, 2.0F, 24, 10), ImageUtils.loadImage(mobImage, 38, 6, 6, 6, 64, 64), 18.0F, 2.0F, 24, 10);
                break;
            case WITHERINVULNERABLE:
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 24, 10, 64, 64), ImageUtils.loadImage(mobImage, 8, 8, 8, 8, 64, 64), 8.0F, 0.0F, 24, 10), ImageUtils.loadImage(mobImage, 38, 6, 6, 6, 64, 64), 0.0F, 2.0F, 24, 10), ImageUtils.loadImage(mobImage, 38, 6, 6, 6, 64, 64), 18.0F, 2.0F, 24, 10);
                break;
            default:
                image = null;
        }

        mobImage.flush();
        if (mobImageSecondary != null) {
            mobImageSecondary.flush();
        }

        return image;
    }

    @Override
    public void onTickInGame(MatrixStack matrixStack, MinecraftClient mc, LayoutVariables layoutVariables) {
        if (this.options.radarAllowed || this.options.radarMobsAllowed || this.options.radarPlayersAllowed) {
            if (this.game == null) {
                this.game = mc;
            }

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

            if (this.enabled) {
                if (this.completedLoading && this.timer > 95) {
                    this.calculateMobs();
                    this.timer = 0;
                }

                ++this.timer;
                if (this.completedLoading) {
                    this.renderMapMobs(matrixStack, this.layoutVariables.mapX, this.layoutVariables.mapY);
                }

                GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }

        }
    }

    private int chkLen(String paramStr) {
        return this.fontRenderer.getStringWidth(paramStr);
    }

    private void write(String paramStr, float x, float y, int color) {
        GLShim.glTexParameteri(3553, 10241, 9728);
        GLShim.glTexParameteri(3553, 10240, 9728);
        this.fontRenderer.drawStringWithShadow(paramStr, x, y, color);
    }

    private boolean isEntityShown(Entity entity) {
        return entity != null && !entity.isInvisibleTo(this.game.player) && (this.options.showHostiles && (this.options.radarAllowed || this.options.radarMobsAllowed) && this.isHostile(entity) || this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed) && this.isPlayer(entity) || this.options.showNeutrals && this.options.radarMobsAllowed && this.isNeutral(entity));
    }

    public void calculateMobs() {
        this.contacts.clear();

        for (Entity entity : this.game.world.getEntities()) {
            try {
                if (this.isEntityShown(entity)) {
                    int wayX = GameVariableAccessShim.xCoord() - (int) entity.getPos().getX();
                    int wayZ = GameVariableAccessShim.zCoord() - (int) entity.getPos().getZ();
                    int wayY = GameVariableAccessShim.yCoord() - (int) entity.getPos().getY();
                    double hypot = (double) (wayX * wayX + wayZ * wayZ + wayY * wayY);
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
                            } catch (Exception var15) {
                            }
                        }

                        Contact contact = new Contact(entity, EnumMobs.getMobTypeByEntity(entity));
                        String unscrubbedName = TextUtils.asFormattedString(contact.entity.getDisplayName());
                        contact.setName(unscrubbedName);
                        if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                            contact.yFudge = 1;
                        }

                        contact.updateLocation();
                        boolean enabled = false;
                        if (!contact.vanillaType) {
                            String type = entity.getType().getTranslationKey();
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
                            if (scrubbedName != null && (scrubbedName.equals("Dinnerbone") || scrubbedName.equals("Grumm")) && (!(contact.entity instanceof PlayerEntity) || ((PlayerEntity) contact.entity).isPartVisible(PlayerModelPart.CAPE))) {
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
                System.err.println(var16.getLocalizedMessage());
                var16.printStackTrace();
            }
        }

        if (this.newMobs) {
            try {
                this.textureAtlas.stitchNew();
            } catch (StitcherException var14) {
                System.err.println("Stitcher exception!  Resetting mobs texture atlas.");
                this.loadTexturePackIcons();
            }
        }

        this.newMobs = false;
        Collections.sort(this.contacts, new Comparator<Contact>() {
            public int compare(Contact contact1, Contact contact2) {
                return contact1.y - contact2.y;
            }
        });
    }

    private void tryCustomIcon(Contact contact) {
        String identifier = contact.vanillaType ? "minecraft." + contact.type.id : contact.entity.getClass().getName();
        String identifierSimple = contact.vanillaType ? contact.type.id : contact.entity.getClass().getSimpleName();
        Sprite icon = this.textureAtlas.getAtlasSprite(identifier + "custom");
        if (icon == this.textureAtlas.getMissingImage()) {
            boolean isHostile = this.isHostile(contact.entity);
            CustomMobsManager.add(contact.entity.getType().getTranslationKey(), isHostile, !isHostile);
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
                is = this.game.getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
            } catch (IOException var15) {
                is = null;
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifierSimple + ".png").toLowerCase();

                try {
                    is = this.game.getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException var14) {
                    is = null;
                }
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifier + "8.png").toLowerCase();

                try {
                    is = this.game.getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException var13) {
                    is = null;
                }
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifierSimple + "8.png").toLowerCase();

                try {
                    is = this.game.getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException var12) {
                    is = null;
                }
            }

            if (is == null) {
                intendedSize = 16;
                fullPath = ("textures/icons/" + identifier + "16.png").toLowerCase();

                try {
                    is = this.game.getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException var11) {
                    is = null;
                }
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifierSimple + "16.png").toLowerCase();

                try {
                    is = this.game.getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException var10) {
                    is = null;
                }
            }

            if (is == null) {
                intendedSize = 32;
                fullPath = ("textures/icons/" + identifier + "32.png").toLowerCase();

                try {
                    is = this.game.getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException var9) {
                    is = null;
                }
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifierSimple + "32.png").toLowerCase();

                try {
                    is = this.game.getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException var8) {
                    is = null;
                }
            }

            if (is != null) {
                mobSkin = ImageIO.read(is);
                is.close();
                mobSkin = ImageUtils.validateImage(mobSkin);
                float scale = (float) mobSkin.getWidth() / (float) intendedSize;
                mobSkin = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(mobSkin, 4.0F / scale)), this.options.outlines, 2);
            }
        } catch (Exception var16) {
            mobSkin = null;
        }

        return mobSkin;
    }

    private void tryAutoIcon(Contact contact) {
        EntityRenderer render = this.game.getEntityRenderDispatcher().getRenderer(contact.entity);
        Identifier resourceLocation = render.getTexture(contact.entity);
        resourceLocation = this.getRandomizedResourceLocationForEntity(resourceLocation, contact.entity);
        Identifier resourceLocationSecondary = null;
        Identifier resourceLocationTertiary = null;
        Identifier resourceLocationQuaternary = null;
        String color = "";
        if (contact.type.secondaryResourceLocation != null) {
            if (contact.type == EnumMobs.MOOSHROOM) {
                if (!((MooshroomEntity) contact.entity).isBaby()) {
                    resourceLocationSecondary = EnumMobs.MOOSHROOM.secondaryResourceLocation;
                } else {
                    resourceLocationSecondary = null;
                }
            } else if (contact.type != EnumMobs.TROPICALFISHA && contact.type != EnumMobs.TROPICALFISHB) {
                label166:
                {
                    if (contact.type == EnumMobs.HORSE) {
                        Entity var22 = contact.entity;
                        if (var22 instanceof HorseEntity) {
                            HorseEntity horse = (HorseEntity) var22;
                            resourceLocationSecondary = (Identifier) TEXTURES.get(horse.getMarking());
                            ItemStack itemStack = horse.getArmorType();
                            if (this.options.showHelmetsMobs) {
                                Item var30 = itemStack.getItem();
                                if (var30 instanceof HorseArmorItem) {
                                    HorseArmorItem horseArmorItem = (HorseArmorItem) var30;
                                    resourceLocationTertiary = horseArmorItem.getEntityTexture();
                                    if (horseArmorItem instanceof DyeableHorseArmorItem) {
                                        DyeableHorseArmorItem dyableHorseArmorItem = (DyeableHorseArmorItem) horseArmorItem;
                                        contact.armorColor = dyableHorseArmorItem.getColor(itemStack);
                                    }
                                }
                            }
                            break label166;
                        }
                    }

                    if (contact.type != EnumMobs.VILLAGER && contact.type != EnumMobs.ZOMBIEVILLAGER) {
                        resourceLocationSecondary = contact.type.secondaryResourceLocation;
                    } else {
                        String zombie = contact.type == EnumMobs.ZOMBIEVILLAGER ? "zombie_" : "";
                        VillagerData villagerData = ((VillagerDataContainer) contact.entity).getVillagerData();
                        VillagerType villagerType = villagerData.getType();
                        VillagerProfession villagerProfession = villagerData.getProfession();
                        resourceLocationSecondary = Registry.VILLAGER_TYPE.getId(villagerType);
                        resourceLocationSecondary = new Identifier(resourceLocationSecondary.getNamespace(), "textures/entity/" + zombie + "villager/type/" + resourceLocationSecondary.getPath() + ".png");
                        if (villagerProfession != VillagerProfession.NONE && !((LivingEntity) contact.entity).isBaby()) {
                            resourceLocationTertiary = Registry.VILLAGER_PROFESSION.getId(villagerProfession);
                            resourceLocationTertiary = new Identifier(resourceLocationTertiary.getNamespace(), "textures/entity/" + zombie + "villager/profession/" + resourceLocationTertiary.getPath() + ".png");
                            if (villagerProfession != VillagerProfession.NITWIT) {
                                resourceLocationQuaternary = (Identifier) LEVEL_TO_ID.get(MathHelper.clamp(villagerData.getLevel(), 1, LEVEL_TO_ID.size()));
                                resourceLocationQuaternary = new Identifier(resourceLocationQuaternary.getNamespace(), "textures/entity/" + zombie + "villager/profession_level/" + resourceLocationQuaternary.getPath() + ".png");
                            }
                        }

                        VillagerResourceMetadata.HatType biomeHatType = this.getHatType(resourceLocationSecondary);
                        VillagerResourceMetadata.HatType professionHatType = this.getHatType(resourceLocationTertiary);
                        boolean showBiomeHat = professionHatType == VillagerResourceMetadata.HatType.NONE || professionHatType == VillagerResourceMetadata.HatType.PARTIAL && biomeHatType != VillagerResourceMetadata.HatType.FULL;
                        if (!showBiomeHat) {
                            resourceLocationSecondary = null;
                        }
                    }
                }
            } else {
                TropicalFishEntity fish = (TropicalFishEntity) contact.entity;
                resourceLocationSecondary = fish.getVarietyId();
                color = fish.getBaseColorComponents() + " " + fish.getPatternColorComponents();
            }

            if (resourceLocationSecondary != null) {
                resourceLocationSecondary = this.getRandomizedResourceLocationForEntity(resourceLocationSecondary, (LivingEntity) contact.entity);
            }

            if (resourceLocationTertiary != null) {
                resourceLocationTertiary = this.getRandomizedResourceLocationForEntity(resourceLocationTertiary, (LivingEntity) contact.entity);
            }

            if (resourceLocationQuaternary != null) {
                resourceLocationQuaternary = this.getRandomizedResourceLocationForEntity(resourceLocationQuaternary, (LivingEntity) contact.entity);
            }
        }

        String entityName = contact.vanillaType ? "minecraft." + contact.type.id : contact.entity.getClass().getName();
        String resourceLocationString = (resourceLocation != null ? resourceLocation.toString() : "") + (resourceLocationSecondary != null ? resourceLocationSecondary.toString() : "");
        resourceLocationString = resourceLocationString + (resourceLocationTertiary != null ? resourceLocationTertiary.toString() : "") + (resourceLocationQuaternary != null ? resourceLocationQuaternary.toString() : "");
        resourceLocationString = resourceLocationString + (contact.armorColor != -1 ? contact.armorColor : "");
        String name = entityName + color + resourceLocationString;
        Sprite icon = this.textureAtlas.getAtlasSprite(name);
        if (icon == this.textureAtlas.getMissingImage()) {
            Integer checkCount = (Integer) this.contactsSkinGetTries.get(name);
            if (checkCount == null) {
                checkCount = 0;
            }

            BufferedImage mobImage = null;
            if (contact.type == EnumMobs.HORSE) {
                BufferedImage base = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
                if (resourceLocationSecondary != null) {
                    BufferedImage pattern = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationSecondary);
                    pattern = ImageUtils.scaleImage(pattern, (float) base.getWidth() / (float) pattern.getWidth(), (float) base.getHeight() / (float) pattern.getHeight());
                    base = ImageUtils.addImages(base, pattern, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                    pattern.flush();
                }

                if (resourceLocationTertiary != null) {
                    BufferedImage armor = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationTertiary);
                    armor = ImageUtils.scaleImage(armor, (float) base.getWidth() / (float) armor.getWidth(), (float) base.getHeight() / (float) armor.getHeight());
                    armor = ImageUtils.colorify(armor, contact.armorColor);
                    base = ImageUtils.addImages(base, armor, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                    armor.flush();
                }

                mobImage = this.createImageFromTypeAndImages(contact.type, base, (BufferedImage) null, contact.entity);
                base.flush();
            } else if ((double) contact.type.expectedWidth > 0.5) {
                mobImage = this.createImageFromTypeAndResourceLocations(contact.type, resourceLocation, resourceLocationSecondary, contact.entity);
            }

            if (mobImage != null) {
                mobImage = this.trimAndOutlineImage(contact, mobImage, false, true);
            } else {
                mobImage = this.createAutoIconImageFromResourceLocations(contact, render, resourceLocation, resourceLocationSecondary, resourceLocationTertiary, resourceLocationQuaternary);
            }

            if (mobImage != null) {
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

    public VillagerResourceMetadata.HatType getHatType(Identifier resourceLocation) {
        VillagerResourceMetadata.HatType hatType = VillagerResourceMetadata.HatType.NONE;
        if (resourceLocation != null) {
            try {
                Optional<Resource> resource = this.game.getResourceManager().getResource(resourceLocation);
                if (resource != null) {
                    VillagerResourceMetadata villagerResourceMetadata = (VillagerResourceMetadata) resource.get().getMetadata();
                    if (villagerResourceMetadata != null) {
                        hatType = villagerResourceMetadata.getHatType();
                    }

                    resource.get().getReader().close();
                }
            } catch (IOException var5) {
            }
        }

        return hatType;
    }

    private BufferedImage createAutoIconImageFromResourceLocations(Contact contact, EntityRenderer render, Identifier... resourceLocations) {
        BufferedImage headImage = null;
        EntityModel model = null;
        if (render instanceof LivingEntityRenderer) {
            try {
                model = ((LivingEntityRenderer) render).getModel();
                ArrayList<Field> submodels = ReflectionUtils.getFieldsByType(model, Model.class, ModelPart.class);
                ArrayList<Field> submodelArrays = ReflectionUtils.getFieldsByType(model, Model.class, ModelPart[].class);
                ModelPart[] headBits = null;
                ArrayList headPartsWithResourceLocationList = new ArrayList();
                Properties properties = new Properties();
                String fullName = contact.vanillaType ? "minecraft." + contact.type.id : contact.entity.getClass().getName();
                String simpleName = contact.vanillaType ? contact.type.id : contact.entity.getClass().getSimpleName();
                String fullPath = ("textures/icons/" + fullName + ".properties").toLowerCase();
                InputStream is = null;

                try {
                    is = this.game.getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException var43) {
                    is = null;
                }

                if (is == null) {
                    fullPath = ("textures/icons/" + simpleName + ".properties").toLowerCase();

                    try {
                        is = this.game.getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                    } catch (IOException var42) {
                        is = null;
                    }
                }

                if (is != null) {
                    properties.load(is);
                    is.close();
                    String subModelNames = properties.getProperty("models", "").toLowerCase();
                    String[] submodelNamesArray = subModelNames.split(",");
                    List subModelNamesList = Arrays.asList(submodelNamesArray);
                    HashSet subModelNamesSet = new HashSet();
                    subModelNamesSet.addAll(subModelNamesList);
                    ArrayList headPartsArrayList = new ArrayList();

                    for (Field submodelArray : submodelArrays) {
                        String name = submodelArray.getName().toLowerCase();
                        if (subModelNamesSet.contains(name) || subModelNames.equals("all")) {
                            ModelPart[] submodelArrayValue = (ModelPart[]) submodelArray.get(model);
                            if (submodelArrayValue != null) {
                                for (int t = 0; t < submodelArrayValue.length; ++t) {
                                    headPartsArrayList.add(submodelArrayValue[t]);
                                }
                            }
                        }
                    }

                    for (Field submodel : submodels) {
                        String name = submodel.getName().toLowerCase();
                        if ((subModelNamesSet.contains(name) || subModelNames.equals("all")) && submodel.get(model) != null) {
                            headPartsArrayList.add((ModelPart) submodel.get(model));
                        }
                    }

                    if (headPartsArrayList.size() > 0) {
                        headBits = (ModelPart[]) headPartsArrayList.toArray(new ModelPart[headPartsArrayList.size()]);
                    }
                }

                if (headBits == null) {
                    if (model instanceof PlayerEntityModel) {
                        boolean showHat = true;
                        Entity var39 = contact.entity;
                        if (var39 instanceof PlayerEntity) {
                            PlayerEntity player = (PlayerEntity) var39;
                            showHat = player.isPartVisible(PlayerModelPart.HAT);
                        }

                        if (showHat) {
                            headBits = new ModelPart[]{((PlayerEntityModel) model).head, ((PlayerEntityModel) model).hat};
                        } else {
                            headBits = new ModelPart[]{((PlayerEntityModel) model).head};
                        }
                    } else if (contact.type == EnumMobs.STRAY) {
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(((SkeletonEntityModel) model).head, resourceLocations[0]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(((SkeletonEntityModel) model).hat, resourceLocations[0]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(this.strayOverlayModel.head, resourceLocations[1]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(this.strayOverlayModel.hat, resourceLocations[1]));
                    } else if (contact.type == EnumMobs.DROWNED) {
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(((DrownedEntityModel) model).head, resourceLocations[0]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(((DrownedEntityModel) model).hat, resourceLocations[0]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(this.drownedOverlayModel.head, resourceLocations[1]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(this.drownedOverlayModel.hat, resourceLocations[1]));
                    } else if (model instanceof AxolotlEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, AxolotlEntityModel.class, ModelPart.class, 6)};
                    } else if (model instanceof BatEntityModel) {
                        BatEntityModel batEntityModel = (BatEntityModel) model;
                        headBits = new ModelPart[]{batEntityModel.getPart().getChild("head")};
                    } else if (model instanceof BeeEntityModel) {
                        headBits = new ModelPart[]{((ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, BeeEntityModel.class, ModelPart.class, 0)).getChild("body")};
                    } else if (model instanceof BipedEntityModel) {
                        BipedEntityModel bipedEntityModel = (BipedEntityModel) model;
                        headBits = new ModelPart[]{bipedEntityModel.head, bipedEntityModel.hat};
                    } else if (model instanceof BlazeEntityModel) {
                        BlazeEntityModel blazeEntityModel = (BlazeEntityModel) model;
                        headBits = new ModelPart[]{blazeEntityModel.getPart().getChild("head")};
                    } else if (model instanceof ChickenEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, ChickenEntityModel.class, ModelPart.class)};
                    } else if (model instanceof CreeperEntityModel) {
                        CreeperEntityModel creeperEntityModel = (CreeperEntityModel) model;
                        headBits = new ModelPart[]{creeperEntityModel.getPart().getChild("head")};
                    } else if (model instanceof DolphinEntityModel) {
                        DolphinEntityModel dolphinEntityModel = (DolphinEntityModel) model;
                        headBits = new ModelPart[]{dolphinEntityModel.getPart().getChild("body").getChild("head")};
                    } else if (model instanceof EndermiteEntityModel) {
                        EndermiteEntityModel endermiteEntityModel = (EndermiteEntityModel) model;
                        headBits = new ModelPart[]{endermiteEntityModel.getPart().getChild("segment0"), endermiteEntityModel.getPart().getChild("segment1")};
                    } else if (model instanceof GhastEntityModel) {
                        GhastEntityModel ghastEntityModel = (GhastEntityModel) model;
                        headBits = new ModelPart[]{ghastEntityModel.getPart()};
                    } else if (model instanceof GuardianEntityModel) {
                        GuardianEntityModel guardianEntityModel = (GuardianEntityModel) model;
                        headBits = new ModelPart[]{guardianEntityModel.getPart().getChild("head")};
                    } else if (model instanceof HoglinEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, HoglinEntityModel.class, ModelPart.class)};
                    } else if (model instanceof HorseEntityModel) {
                        HorseEntityModel horseEntityModel = (HorseEntityModel) model;
                        headBits = (ModelPart[]) StreamSupport.stream(horseEntityModel.getHeadParts().spliterator(), false).toArray(x$0 -> new ModelPart[x$0]);
                    } else if (model instanceof IllagerEntityModel) {
                        IllagerEntityModel illagerEntityModel = (IllagerEntityModel) model;
                        headBits = new ModelPart[]{illagerEntityModel.getPart().getChild("head")};
                    } else if (model instanceof IronGolemEntityModel) {
                        IronGolemEntityModel ironGolemEntityModel = (IronGolemEntityModel) model;
                        headBits = new ModelPart[]{ironGolemEntityModel.getPart().getChild("head")};
                    } else if (model instanceof MagmaCubeEntityModel) {
                        headBits = (ModelPart[]) ReflectionUtils.getPrivateFieldValueByType(model, MagmaCubeEntityModel.class, ModelPart[].class);
                    } else if (model instanceof OcelotEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, OcelotEntityModel.class, ModelPart.class, 6)};
                    } else if (model instanceof PhantomEntityModel) {
                        PhantomEntityModel phantomEntityModel = (PhantomEntityModel) model;
                        headBits = new ModelPart[]{phantomEntityModel.getPart().getChild("body")};
                    } else if (model instanceof RabbitEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, RabbitEntityModel.class, ModelPart.class, 7), (ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, RabbitEntityModel.class, ModelPart.class, 8), (ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, RabbitEntityModel.class, ModelPart.class, 9), (ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, RabbitEntityModel.class, ModelPart.class, 11)};
                    } else if (model instanceof RavagerEntityModel) {
                        RavagerEntityModel ravagerEntityModel = (RavagerEntityModel) model;
                        headBits = new ModelPart[]{ravagerEntityModel.getPart().getChild("neck").getChild("head")};
                    } else if (model instanceof ShulkerEntityModel) {
                        ShulkerEntityModel shulkerEntityModel = (ShulkerEntityModel) model;
                        headBits = new ModelPart[]{shulkerEntityModel.getHead()};
                    } else if (model instanceof SilverfishEntityModel) {
                        SilverfishEntityModel silverFishEntityModel = (SilverfishEntityModel) model;
                        headBits = new ModelPart[]{silverFishEntityModel.getPart().getChild("segment0"), silverFishEntityModel.getPart().getChild("segment1")};
                    } else if (model instanceof SlimeEntityModel) {
                        SlimeEntityModel slimeEntityModel = (SlimeEntityModel) model;
                        headBits = new ModelPart[]{slimeEntityModel.getPart()};
                    } else if (model instanceof SnowGolemEntityModel) {
                        SnowGolemEntityModel snowGolemEntityModel = (SnowGolemEntityModel) model;
                        headBits = new ModelPart[]{snowGolemEntityModel.getPart().getChild("head")};
                    } else if (model instanceof SpiderEntityModel) {
                        SpiderEntityModel spiderEntityModel = (SpiderEntityModel) model;
                        headBits = new ModelPart[]{spiderEntityModel.getPart().getChild("head"), spiderEntityModel.getPart().getChild("body0")};
                    } else if (model instanceof SquidEntityModel) {
                        SquidEntityModel squidEntityModel = (SquidEntityModel) model;
                        headBits = new ModelPart[]{squidEntityModel.getPart().getChild("body")};
                    } else if (model instanceof StriderEntityModel) {
                        StriderEntityModel striderEntityModel = (StriderEntityModel) model;
                        headBits = new ModelPart[]{striderEntityModel.getPart().getChild("body")};
                    } else if (model instanceof VillagerResemblingModel) {
                        VillagerResemblingModel villagerResemblingModel = (VillagerResemblingModel) model;
                        headBits = new ModelPart[]{villagerResemblingModel.getHead()};
                    } else if (model instanceof WolfEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, WolfEntityModel.class, ModelPart.class)};
                    } else if (model instanceof QuadrupedEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, QuadrupedEntityModel.class, ModelPart.class)};
                    } else if (model instanceof SinglePartEntityModel) {
                        SinglePartEntityModel singlePartEntityModel = (SinglePartEntityModel) model;

                        try {
                            headBits = new ModelPart[]{singlePartEntityModel.getPart().getChild("head")};
                        } catch (Exception var41) {
                        }
                    }
                }

                if (headBits == null) {
                    ArrayList<ModelPart> headPartsArrayList = new ArrayList();
                    ArrayList purge = new ArrayList();

                    for (Field submodelArray : submodelArrays) {
                        String name = submodelArray.getName().toLowerCase();
                        if (name.contains("head") | name.contains("eye") | name.contains("mouth") | name.contains("teeth") | name.contains("tooth") | name.contains("tusk") | name.contains("jaw") | name.contains("mand") | name.contains("nose") | name.contains("beak") | name.contains("snout") | name.contains("muzzle") | (!name.contains("rear") && name.contains("ear")) | name.contains("trunk") | name.contains("mane") | name.contains("horn") | name.contains("antler")) {
                            ModelPart[] submodelArrayValue = (ModelPart[]) submodelArray.get(model);
                            if (submodelArrayValue != null && submodelArrayValue.length >= 0) {
                                headPartsArrayList.add(submodelArrayValue[0]);
                            }
                        }
                    }

                    for (Field submodel : submodels) {
                        String name = submodel.getName().toLowerCase();
                        String nameS = submodel.getName();
                        if (name.contains("head") | name.contains("eye") | name.contains("mouth") | name.contains("teeth") | name.contains("tooth") | name.contains("tusk") | name.contains("jaw") | name.contains("mand") | name.contains("nose") | name.contains("beak") | name.contains("snout") | name.contains("muzzle") | (!name.contains("rear") && name.contains("ear")) | name.contains("trunk") | name.contains("mane") | name.contains("horn") | name.contains("antler") | nameS.equals("REar") | nameS.equals("Trout") && !nameS.equals("LeftSmallEar") & !nameS.equals("RightSmallEar") & !nameS.equals("BHead") & !nameS.equals("BSnout") & !nameS.equals("BMouth") & !nameS.equals("BMouthOpen") & !nameS.equals("BLEar") & !nameS.equals("BREar") & !nameS.equals("CHead") & !nameS.equals("CSnout") & !nameS.equals("CMouth") & !nameS.equals("CMouthOpen") & !nameS.equals("CLEar") & !nameS.equals("CREar") && submodel.get(model) != null) {
                            headPartsArrayList.add((ModelPart) submodel.get(model));
                        }
                    }

                    if (headPartsArrayList.size() == 0) {
                        int pos = model instanceof SinglePartEntityModel ? 1 : 0;
                        if (submodels.size() > pos) {
                            if (((Field) submodels.get(pos)).get(model) != null) {
                                headPartsArrayList.add((ModelPart) ((Field) submodels.get(pos)).get(model));
                            }
                        } else if (submodelArrays.size() > 0 && ((Field) submodelArrays.get(0)).get(model) != null) {
                            ModelPart[] submodelArrayValue = (ModelPart[]) ((Field) submodelArrays.get(0)).get(model);
                            if (submodelArrayValue.length > 0) {
                                headPartsArrayList.add(submodelArrayValue[0]);
                            }
                        }
                    }

                    for (ModelPart bit : headPartsArrayList) {
                        try {
                            Object childrenObj = ReflectionUtils.getPrivateFieldValueByType(bit, ModelPart.class, ObjectList.class, 1);
                            if (childrenObj != null) {
                                List children = (List) childrenObj;
                                purge.addAll(children);
                            }
                        } catch (Exception var40) {
                        }
                    }

                    headPartsArrayList.removeAll(purge);
                    headBits = (ModelPart[]) headPartsArrayList.toArray(new ModelPart[headPartsArrayList.size()]);
                }

                if (contact.entity != null && model != null && (headBits != null && headBits.length > 0 || headPartsWithResourceLocationList.size() > 0) && resourceLocations[0] != null) {
                    String scaleString = properties.getProperty("scale", "1");
                    float scale = Float.parseFloat(scaleString);
                    Direction facing = Direction.NORTH;
                    String facingString = properties.getProperty("facing", "front");
                    if (facingString.equals("top")) {
                        facing = Direction.UP;
                    } else if (facingString.equals("side")) {
                        facing = Direction.EAST;
                    }

                    Identifier resourceLocation = this.combineResourceLocations(resourceLocations);
                    if (headBits != null) {
                        for (int t = 0; t < headBits.length; ++t) {
                            headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(headBits[t], resourceLocation));
                        }
                    }

                    ModelPartWithResourceLocation[] headBitsWithLocations = (ModelPartWithResourceLocation[]) headPartsWithResourceLocationList.toArray(new ModelPartWithResourceLocation[headPartsWithResourceLocationList.size()]);
                    boolean success = this.drawModel(scale, 1000, (LivingEntity) contact.entity, facing, model, headBitsWithLocations);
                    ImageUtils.saveImage(contact.type.id, GLUtils.fboTextureID, 0, 512, 512);
                    if (success) {
                        headImage = ImageUtils.createBufferedImageFromGLID(GLUtils.fboTextureID);
                    }
                }
            } catch (Exception var44) {
                headImage = null;
                var44.printStackTrace();
            }
        }

        if (headImage != null) {
            headImage = this.trimAndOutlineImage(contact, headImage, true, model != null && model instanceof BipedEntityModel);
        }

        return headImage;
    }

    private Identifier combineResourceLocations(Identifier... resourceLocations) {
        Identifier resourceLocation = resourceLocations[0];
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
                        float xScale = (float) (base.getWidth() / overlay.getWidth());
                        float yScale = (float) (base.getHeight() / overlay.getHeight());
                        if (xScale != 1.0F || yScale != 1.0F) {
                            overlay = ImageUtils.scaleImage(overlay, xScale, yScale);
                        }

                        base = ImageUtils.addImages(base, overlay, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                        overlay.flush();
                    }
                }

                if (hasAdditional) {
                    NativeImage nativeImage = GLUtils.nativeImageFromBufferedImage(base);
                    base.flush();
                    this.nativeBackedTexture.close();
                    this.nativeBackedTexture = new NativeImageBackedTexture(nativeImage);
                    GLUtils.register(this.nativeBackedTextureLocation, this.nativeBackedTexture);
                    resourceLocation = this.nativeBackedTextureLocation;
                }
            } catch (Exception var9) {
                var9.printStackTrace();
            }
        }

        return resourceLocation;
    }

    private boolean drawModel(float scale, int captureDepth, LivingEntity livingEntity, Direction facing, Model model, ModelPartWithResourceLocation[] headBits) {
        boolean failed = false;
        float size = 64.0F * scale;
        GLShim.glBindTexture(3553, GLUtils.fboTextureID);
        int width = GLShim.glGetTexLevelParameteri(3553, 0, 4096);
        int height = GLShim.glGetTexLevelParameteri(3553, 0, 4097);
        GLShim.glBindTexture(3553, 0);
        GLShim.glViewport(0, 0, width, height);
        Matrix4f minimapProjectionMatrix = RenderSystem.getProjectionMatrix();
        Matrix4f matrix4f = Matrix4f.projectionMatrix(0.0F, (float) width, 0.0F, (float) height, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f);
        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.loadIdentity();
        matrixStack.translate(0.0, 0.0, -3000.0 + (double) captureDepth);
        RenderSystem.applyModelViewMatrix();
        GLUtils.bindFrameBuffer();
        GLShim.glDepthMask(true);
        GLShim.glEnable(2929);
        GLShim.glEnable(3553);
        GLShim.glEnable(3042);
        GLShim.glDisable(2884);
        GLShim.glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
        GLShim.glClearDepth(1.0);
        GLShim.glClear(16640);
        GLShim.glBlendFunc(770, 771);
        matrixStack.push();
        matrixStack.translate((double) (width / 2), (double) (height / 2), 0.0);
        matrixStack.scale(size, size, size);
        matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180.0F));
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F));
        if (facing == Direction.EAST) {
            matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-90.0F));
        } else if (facing == Direction.UP) {
            matrixStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
        }

        RenderSystem.applyModelViewMatrix();
        Vector4f fullbright2 = new Vector4f(this.fullbright);
        fullbright2.transform(matrixStack.peek().getPositionMatrix());
        Vec3f fullbright3 = new Vec3f(fullbright2);
        RenderSystem.setShaderLights(fullbright3, fullbright3);

        try {
            MatrixStack newMatrixStack = new MatrixStack();
            VertexConsumerProvider.Immediate immediate = this.game.getBufferBuilders().getEntityVertexConsumers();
            float offsetByY = model instanceof EndermanEntityModel ? 8.0F : (!(model instanceof BipedEntityModel) && !(model instanceof SkullEntityModel) ? 0.0F : 4.0F);
            float maxY = 0.0F;
            float minY = 0.0F;

            for (int t = 0; t < headBits.length; ++t) {
                if (headBits[t].modelPart.pivotY < minY) {
                    minY = headBits[t].modelPart.pivotY;
                }

                if (headBits[t].modelPart.pivotY > maxY) {
                    maxY = headBits[t].modelPart.pivotY;
                }
            }

            if (minY < -25.0F) {
                offsetByY = -25.0F - minY;
            } else if (maxY > 25.0F) {
                offsetByY = 25.0F - maxY;
            }

            for (int t = 0; t < headBits.length; ++t) {
                VertexConsumer vertexConsumer = immediate.getBuffer(model.getLayer(headBits[t].resourceLocation));
                if (model instanceof EntityModel) {
                    EntityModel entityModel = (EntityModel) model;
                    entityModel.setAngles(livingEntity, 0.0F, 0.0F, 163.0F, 360.0F, 0.0F);
                }

                float y = headBits[t].modelPart.pivotY;
                headBits[t].modelPart.pivotY += offsetByY;
                headBits[t].modelPart.render(newMatrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV);
                headBits[t].modelPart.pivotY = y;
                immediate.draw();
            }
        } catch (Exception var25) {
            System.out.println("Error attempting to render head bits for " + livingEntity.getClass().getSimpleName());
            var25.printStackTrace();
            failed = true;
        }

        matrixStack.pop();
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        GLShim.glEnable(2884);
        GLShim.glDisable(2929);
        GLShim.glDepthMask(false);
        GLUtils.unbindFrameBuffer();
        RenderSystem.setProjectionMatrix(minimapProjectionMatrix);
        GLShim.glViewport(0, 0, this.game.getWindow().getFramebufferWidth(), this.game.getWindow().getFramebufferHeight());
        return !failed;
    }

    private void getGenericIcon(Contact contact) {
        contact.type = this.getUnknownMobNeutrality(contact.entity);
        String name = "minecraft." + contact.type.id + contact.type.resourceLocation.toString();
        contact.icon = this.textureAtlas.getAtlasSprite(name);
    }

    private Identifier getRandomizedResourceLocationForEntity(Identifier resourceLocation, Entity entity) {
        try {
            if (this.randomobsOptifine) {
                Object randomEntitiesProperties = this.mapProperties.get(resourceLocation.getPath());
                if (randomEntitiesProperties != null) {
                    this.setEntityMethod.invoke(this.randomEntityClass.cast(this.randomEntity), entity);
                    resourceLocation = (Identifier) this.getEntityTextureMethod.invoke(this.randomEntitiesPropertiesClass.cast(randomEntitiesProperties), resourceLocation, this.randomEntityClass.cast(this.randomEntity));
                }
            }
        } catch (Exception var4) {
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
            float scale = (float) Math.ceil((double) maxDimension / acceptableMax);
            return ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(image, 1.0F / scale)), this.options.outlines, 2);
        } else {
            float scale = (float) image.getWidth() / contact.type.expectedWidth;
            return ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(image, 4.0F / scale)), this.options.outlines, 2);
        }
    }

    private void handleMPplayer(Contact contact) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) contact.entity;
        GameProfile gameProfile = player.getGameProfile();
        UUID uuid = gameProfile.getId();
        contact.setUUID(uuid);
        String playerName = this.scrubCodes(gameProfile.getName());
        Sprite icon = this.textureAtlas.getAtlasSprite(playerName);
        Integer checkCount = 0;
        if (icon == this.textureAtlas.getMissingImage()) {
            checkCount = (Integer) this.mpContactsSkinGetTries.get(playerName);
            if (checkCount == null) {
                checkCount = 0;
            }

            if (checkCount < 5) {
                PlayerSkinTexture imageData = null;

                try {
                    if (player.getSkinTexture() == DefaultSkinHelper.getTexture(player.getUuid())) {
                        throw new Exception("failed to get skin: skin is default");
                    }

                    AbstractClientPlayerEntity.loadSkin(player.getSkinTexture(), player.getName().getString());
                    imageData = (PlayerSkinTexture) MinecraftClient.getInstance().getTextureManager().getTexture(player.getSkinTexture());
                    if (imageData == null) {
                        throw new Exception("failed to get skin: image data was null");
                    }

                    EntityRenderer render = this.game.getEntityRenderDispatcher().getRenderer(contact.entity);
                    BufferedImage skinImage = this.createAutoIconImageFromResourceLocations(contact, render, player.getSkinTexture(), null);
                    icon = this.textureAtlas.registerIconForBufferedImage(playerName, skinImage);
                    this.newMobs = true;
                    this.mpContactsSkinGetTries.remove(playerName);
                } catch (Exception var11) {
                    icon = this.textureAtlas.getAtlasSprite("minecraft." + EnumMobs.PLAYER.id + EnumMobs.PLAYER.resourceLocation.toString());
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
        ItemStack stack = ((LivingEntity) entity).getEquippedStack(EquipmentSlot.HEAD);
        Item helmet = null;
        if (stack != null && stack.getCount() > 0) {
            helmet = stack.getItem();
        }

        if (contact.type == EnumMobs.SHEEP) {
            SheepEntity sheepEntity = (SheepEntity) contact.entity;
            if (!sheepEntity.isSheared()) {
                icon = this.textureAtlas.getAtlasSprite("sheepfur");
                float[] sheepColors = SheepEntity.getRgbColor(sheepEntity.getColor());
                contact.setArmorColor((int) (sheepColors[0] * 255.0F) << 16 | (int) (sheepColors[1] * 255.0F) << 8 | (int) (sheepColors[2] * 255.0F));
            }
        } else if (helmet != null) {
            if (helmet == Items.SKELETON_SKULL) {
                icon = this.textureAtlas.getAtlasSprite("minecraft." + EnumMobs.SKELETON.id + EnumMobs.SKELETON.resourceLocation.toString() + "head");
            } else if (helmet == Items.WITHER_SKELETON_SKULL) {
                icon = this.textureAtlas.getAtlasSprite("minecraft." + EnumMobs.SKELETONWITHER.id + EnumMobs.SKELETONWITHER.resourceLocation.toString() + "head");
            } else if (helmet == Items.ZOMBIE_HEAD) {
                icon = this.textureAtlas.getAtlasSprite("minecraft." + EnumMobs.ZOMBIE.id + EnumMobs.ZOMBIE.resourceLocation.toString() + "head");
            } else if (helmet == Items.CREEPER_HEAD) {
                icon = this.textureAtlas.getAtlasSprite("minecraft." + EnumMobs.CREEPER.id + EnumMobs.CREEPER.resourceLocation.toString() + "head");
            } else if (helmet == Items.DRAGON_HEAD) {
                icon = this.textureAtlas.getAtlasSprite("minecraft." + EnumMobs.ENDERDRAGON.id + EnumMobs.ENDERDRAGON.resourceLocation.toString() + "head");
            } else if (helmet == Items.PLAYER_HEAD) {
                GameProfile gameProfile = null;
                if (stack.hasNbt()) {
                    NbtCompound nbttagcompound = stack.getNbt();
                    if (nbttagcompound.contains("SkullOwner", 10)) {
                        gameProfile = NbtHelper.toGameProfile(nbttagcompound.getCompound("SkullOwner"));
                    } else if (nbttagcompound.contains("SkullOwner", 8)) {
                        String name = nbttagcompound.getString("SkullOwner");
                        if (name != null && !name.equals("")) {
                            gameProfile = new GameProfile((UUID) null, name);
                            nbttagcompound.remove("SkullOwner");
                            SkullBlockEntity.loadProperties(gameProfile, gameProfilex -> nbttagcompound.put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), gameProfilex)));
                        }
                    }
                }

                Identifier resourceLocation = DefaultSkinHelper.getTexture();
                if (gameProfile != null) {
                    java.util.Map map = this.game.getSkinProvider().getTextures(gameProfile);
                    if (map.containsKey(Type.SKIN)) {
                        resourceLocation = this.game.getSkinProvider().loadSkin((MinecraftProfileTexture) map.get(Type.SKIN), Type.SKIN);
                    }
                }

                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("minecraft." + EnumMobs.PLAYER.id + resourceLocation.toString() + "head");
                if (icon == this.textureAtlas.getMissingImage()) {
                    ModelPart inner = (ModelPart) ReflectionUtils.getPrivateFieldValueByType(this.playerSkullModel, SkullEntityModel.class, ModelPart.class, 0);
                    ModelPart outer = (ModelPart) ReflectionUtils.getPrivateFieldValueByType(this.playerSkullModel, SkullEntityModel.class, ModelPart.class, 1);
                    ModelPartWithResourceLocation[] headBits = new ModelPartWithResourceLocation[]{new ModelPartWithResourceLocation(inner, resourceLocation), new ModelPartWithResourceLocation(outer, resourceLocation)};
                    boolean success = this.drawModel(1.1875F, 1000, (LivingEntity) contact.entity, Direction.NORTH, this.playerSkullModel, headBits);
                    if (success) {
                        BufferedImage headImage = ImageUtils.createBufferedImageFromGLID(GLUtils.fboTextureID);
                        headImage = this.trimAndOutlineImage(new Contact(this.game.player, EnumMobs.PLAYER), headImage, true, true);
                        icon = this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.PLAYER.id + resourceLocation.toString() + "head", headImage);
                        this.newMobs = true;
                    }
                }
            } else if (helmet instanceof ArmorItem) {
                ArmorItem helmetArmor = (ArmorItem) helmet;
                int armorType = this.getArmorType(helmetArmor);
                if (armorType != UNKNOWN) {
                    icon = this.textureAtlas.getAtlasSprite("armor " + this.armorNames[armorType]);
                } else {
                    boolean isPiglin = contact.type == EnumMobs.PIGLIN || contact.type == EnumMobs.PIGLINZOMBIE;
                    icon = this.textureAtlas.getAtlasSprite("armor " + helmet.getTranslationKey() + (isPiglin ? "_piglin" : ""));
                    if (icon == this.textureAtlas.getMissingImage()) {
                        icon = this.createUnknownArmorIcons(contact, stack, helmet);
                    } else if (icon == this.textureAtlas.getFailedImage()) {
                        icon = null;
                    }
                }

                if (helmetArmor instanceof DyeableArmorItem) {
                    DyeableArmorItem dyeableHelmetArmor = (DyeableArmorItem) helmetArmor;
                    contact.setArmorColor(dyeableHelmetArmor.getColor(stack));
                }
            } else if (helmet instanceof BlockItem) {
                BlockItem blockItem = (BlockItem) helmet;
                Block block = blockItem.getBlock();
                BlockState blockState = block.getDefaultState();
                int stateID = Block.getRawIdFromState(blockState);
                icon = this.textureAtlas.getAtlasSprite("blockArmor " + stateID);
                if (icon == this.textureAtlas.getMissingImage()) {
                    BufferedImage blockImage = this.master.getColorManager().getBlockImage(blockState, stack, entity.world, 4.9473686F, -8.0F);
                    if (blockImage != null) {
                        int width = blockImage.getWidth();
                        int height = blockImage.getHeight();
                        blockImage = ImageUtils.eraseArea(blockImage, width / 2 - 15, height / 2 - 15, 30, 30, width, height);
                        BufferedImage blockImageFront = this.master.getColorManager().getBlockImage(blockState, stack, entity.world, 4.9473686F, 7.25F);
                        blockImageFront = blockImageFront.getSubimage(width / 2 - 15, height / 2 - 15, 30, 30);
                        blockImage = ImageUtils.addImages(blockImage, blockImageFront, (float) (width / 2 - 15), (float) (height / 2 - 15), width, height);
                        blockImageFront.flush();
                        blockImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.trimCentered(blockImage)), this.options.outlines, true, 37.6F, 37.6F, 2);
                        icon = this.textureAtlas.registerIconForBufferedImage("blockArmor " + stateID, blockImage);
                        this.newMobs = true;
                    }
                }
            }
        }

        contact.armorIcon = icon;
    }

    private Sprite createUnknownArmorIcons(Contact contact, ItemStack stack, Item helmet) {
        Sprite icon = null;
        boolean isPiglin = contact.type == EnumMobs.PIGLIN || contact.type == EnumMobs.PIGLINZOMBIE;
        Method m = null;

        try {
            Class c = Class.forName("net.minecraftforge.client.ForgeHooksClient");
            m = c.getMethod("getArmorTexture", Entity.class, ItemStack.class, String.class, EquipmentSlot.class, String.class);
        } catch (Exception var19) {
        }

        Method getResourceLocation = m;
        Identifier resourceLocation = null;

        try {
            String materialName = ((ArmorItem) helmet).getMaterial().getName();
            String domain = "minecraft";
            int sep = materialName.indexOf(58);
            if (sep != -1) {
                domain = materialName.substring(0, sep);
                materialName = materialName.substring(sep + 1);
            }

            String suffix = null;
            suffix = suffix == null ? "" : "_" + suffix;
            String resourcePath = String.format("%s:textures/models/armor/%s_layer_%d%s.png", domain, materialName, 1, suffix);
            if (getResourceLocation != null) {
                resourcePath = (String) getResourceLocation.invoke((Object) null, contact.entity, stack, resourcePath, EquipmentSlot.HEAD, null);
            }

            resourceLocation = new Identifier(resourcePath);
        } catch (Exception var18) {
        }

        m = null;

        try {
            Class c = Class.forName("net.minecraftforge.client.ForgeHooksClient");
            m = c.getMethod("getArmorModel", LivingEntity.class, ItemStack.class, EquipmentSlot.class, BipedEntityModel.class);
        } catch (Exception var17) {
        }

        Method getModel = m;
        BipedEntityModel modelBiped = null;

        try {
            if (getModel != null) {
                modelBiped = (BipedEntityModel) getModel.invoke((Object) null, contact.entity, stack, EquipmentSlot.HEAD, null);
            }
        } catch (Exception var16) {
        }

        float intendedWidth = 9.0F;
        float intendedHeight = 9.0F;
        if (modelBiped == null) {
            if (!isPiglin) {
                modelBiped = this.bipedArmorModel;
            } else {
                modelBiped = this.piglinArmorModel;
                intendedWidth = 11.5F;
            }
        }

        if (modelBiped != null && resourceLocation != null) {
            ModelPartWithResourceLocation[] headBitsWithResourceLocation = new ModelPartWithResourceLocation[]{new ModelPartWithResourceLocation(modelBiped.head, resourceLocation), new ModelPartWithResourceLocation(modelBiped.hat, resourceLocation)};
            this.drawModel(1.0F, 2, (LivingEntity) contact.entity, Direction.NORTH, modelBiped, headBitsWithResourceLocation);
            BufferedImage armorImage = ImageUtils.createBufferedImageFromGLID(GLUtils.fboTextureID);
            armorImage = armorImage.getSubimage(200, 200, 112, 112);
            armorImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.trimCentered(armorImage)), this.options.outlines, true, intendedWidth * 4.0F, intendedHeight * 4.0F, 2);
            icon = this.textureAtlas.registerIconForBufferedImage("armor " + helmet.getTranslationKey() + (isPiglin ? "_piglin" : ""), armorImage);
            this.newMobs = true;
        }

        if (icon == null && resourceLocation != null) {
            BufferedImage armorTexture = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
            if (armorTexture != null) {
                if (!isPiglin) {
                    armorTexture = ImageUtils.addImages(ImageUtils.loadImage(armorTexture, 8, 8, 8, 8), ImageUtils.loadImage(armorTexture, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
                    float scale = (float) armorTexture.getWidth() / 8.0F;
                    BufferedImage armorImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(armorTexture, 4.0F / scale * 47.0F / 38.0F)), this.options.outlines, true, 37.6F, 37.6F, 2);
                    icon = this.textureAtlas.registerIconForBufferedImage("armor " + resourceLocation.toString(), armorImage);
                } else {
                    armorTexture = ImageUtils.addImages(ImageUtils.loadImage(armorTexture, 8, 8, 8, 8), ImageUtils.loadImage(armorTexture, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
                    float scale = (float) armorTexture.getWidth() / 8.0F;
                    BufferedImage armorImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(armorTexture, 4.0F / scale * 47.0F / 38.0F)), this.options.outlines, true, 47.0F, 37.6F, 2);
                    icon = this.textureAtlas.registerIconForBufferedImage("armor " + resourceLocation.toString() + "_piglin", armorImage);
                }

                this.newMobs = true;
            }
        }

        if (icon == null) {
            System.out.println("can't get texture for custom armor type: " + helmet.getClass());
            this.textureAtlas.registerFailedIcon("armor " + helmet.getTranslationKey() + helmet.getClass().getName());
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
            if (entity instanceof TameableEntity) {
                TameableEntity tameableEntity = (TameableEntity) entity;
                if (tameableEntity.isTamed() && (this.game.isIntegratedServerRunning() || tameableEntity.getOwner().equals(this.game.player))) {
                    return EnumMobs.GENERICTAME;
                }
            }

            return EnumMobs.GENERICNEUTRAL;
        }
    }

    private int getArmorType(ArmorItem helmet) {
        return helmet.getTranslationKey().equals("item.minecraft.leather_helmet") ? 0 : UNKNOWN;
    }

    public void renderMapMobs(MatrixStack matrixStack, int x, int y) {
        double max = this.layoutVariables.zoomScaleAdjusted * 32.0;
        double lastX = GameVariableAccessShim.xCoordDouble();
        double lastZ = GameVariableAccessShim.zCoordDouble();
        int lastY = GameVariableAccessShim.yCoord();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        GLUtils.disp2(this.textureAtlas.getGlId());
        GLShim.glEnable(3042);
        GLShim.glBlendFunc(770, 771);

        for (Contact contact : this.contacts) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            contact.updateLocation();
            double contactX = contact.x;
            double contactZ = contact.z;
            int contactY = contact.y;
            double wayX = lastX - contactX;
            double wayZ = lastZ - contactZ;
            int wayY = lastY - contactY;
            double adjustedDiff = max - (double) Math.max(Math.abs(wayY), 0);
            contact.brightness = (float) Math.max(adjustedDiff / max, 0.0);
            contact.brightness *= contact.brightness;
            contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
            contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ) / this.layoutVariables.zoomScaleAdjusted;
            if (wayY < 0) {
                GLShim.glColor4f(1.0F, 1.0F, 1.0F, contact.brightness);
            } else {
                GLShim.glColor3f(contact.brightness, contact.brightness, contact.brightness);
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
                    matrixStack.push();
                    if (this.options.filtering) {
                        matrixStack.translate(x, y, 0.0);
                        matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-contact.angle));
                        matrixStack.translate(0.0, -contact.distance, 0.0);
                        matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(contact.angle + (float) contact.rotationFactor));
                        matrixStack.translate((-x), (-y), 0.0);
                    } else {
                        wayX = Math.sin(Math.toRadians(contact.angle)) * contact.distance;
                        wayZ = Math.cos(Math.toRadians(contact.angle)) * contact.distance;
                        matrixStack.translate((double) Math.round(-wayX * (double) this.layoutVariables.scScale) / (double) this.layoutVariables.scScale, (double) Math.round(-wayZ * (double) this.layoutVariables.scScale) / (double) this.layoutVariables.scScale, 0.0);
                    }

                    RenderSystem.applyModelViewMatrix();
                    float yOffset = 0.0F;
                    if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                        yOffset = -4.0F;
                    }

                    if (contact.type == EnumMobs.GHAST || contact.type == EnumMobs.GHASTATTACKING || contact.type == EnumMobs.WITHER || contact.type == EnumMobs.WITHERINVULNERABLE || contact.type == EnumMobs.VEX || contact.type == EnumMobs.VEXCHARGING || contact.type == EnumMobs.PUFFERFISH || contact.type == EnumMobs.PUFFERFISHHALF || contact.type == EnumMobs.PUFFERFISHFULL) {
                        if (contact.type != EnumMobs.GHAST && contact.type != EnumMobs.GHASTATTACKING) {
                            if (contact.type != EnumMobs.WITHER && contact.type != EnumMobs.WITHERINVULNERABLE) {
                                if (contact.type != EnumMobs.VEX && contact.type != EnumMobs.VEXCHARGING) {
                                    if (contact.type == EnumMobs.PUFFERFISH || contact.type == EnumMobs.PUFFERFISHHALF || contact.type == EnumMobs.PUFFERFISHFULL) {
                                        int size = ((PufferfishEntity) contact.entity).getPuffState();
                                        switch (size) {
                                            case 0 -> contact.type = EnumMobs.PUFFERFISH;
                                            case 1 -> contact.type = EnumMobs.PUFFERFISHHALF;
                                            case 2 -> contact.type = EnumMobs.PUFFERFISHFULL;
                                        }
                                    }
                                } else {
                                    EntityRenderer render = this.game.getEntityRenderDispatcher().getRenderer(contact.entity);
                                    String path = render.getTexture(contact.entity).getPath();
                                    contact.type = path.endsWith("vex_charging.png") ? EnumMobs.VEXCHARGING : EnumMobs.VEX;
                                }
                            } else {
                                EntityRenderer render = this.game.getEntityRenderDispatcher().getRenderer(contact.entity);
                                String path = render.getTexture(contact.entity).getPath();
                                contact.type = path.endsWith("wither_invulnerable.png") ? EnumMobs.WITHERINVULNERABLE : EnumMobs.WITHER;
                            }
                        } else {
                            EntityRenderer render = this.game.getEntityRenderDispatcher().getRenderer(contact.entity);
                            String path = render.getTexture(contact.entity).getPath();
                            contact.type = path.endsWith("ghast_fire.png") ? EnumMobs.GHASTATTACKING : EnumMobs.GHAST;
                        }

                        this.tryAutoIcon(contact);
                        this.tryCustomIcon(contact);
                        if (this.newMobs) {
                            try {
                                this.textureAtlas.stitchNew();
                            } catch (StitcherException var45) {
                                System.err.println("Stitcher exception in render method!  Resetting mobs texture atlas.");
                                this.loadTexturePackIcons();
                            }

                            GLUtils.disp2(this.textureAtlas.getGlId());
                        }

                        this.newMobs = false;
                    }

                    if (contact.uuid != null && contact.uuid.equals(this.devUUID)) {
                        Sprite icon = this.textureAtlas.getAtlasSprite("glow");
                        this.applyFilteringParameters();
                        GLUtils.drawPre();
                        GLUtils.setMap(icon, (float) x, (float) y + yOffset, (float) ((int) ((float) icon.getIconWidth() / 2.0F)));
                        GLUtils.drawPost();
                    }

                    this.applyFilteringParameters();
                    GLUtils.drawPre();
                    GLUtils.setMap(contact.icon, (float) x, (float) y + yOffset, (float) ((int) ((float) contact.icon.getIconWidth() / 4.0F)));
                    GLUtils.drawPost();
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
                            red = (float) (contact.armorColor >> 16 & 0xFF) / 255.0F;
                            green = (float) (contact.armorColor >> 8 & 0xFF) / 255.0F;
                            blue = (float) (contact.armorColor >> 0 & 0xFF) / 255.0F;
                            if (contact.type == EnumMobs.SHEEP) {
                                SheepEntity sheepEntity = (SheepEntity) contact.entity;
                                if (sheepEntity.hasCustomName() && "jeb_".equals(sheepEntity.getName().getString())) {
                                    int semiRandom = sheepEntity.age / 25 + sheepEntity.getId();
                                    int numDyeColors = DyeColor.values().length;
                                    int colorID1 = semiRandom % numDyeColors;
                                    int colorID2 = (semiRandom + 1) % numDyeColors;
                                    float lerpVal = ((float) (sheepEntity.age % 25) + this.game.getTickDelta()) / 25.0F;
                                    float[] sheepColors1 = SheepEntity.getRgbColor(DyeColor.byId(colorID1));
                                    float[] sheepColors2 = SheepEntity.getRgbColor(DyeColor.byId(colorID2));
                                    red = sheepColors1[0] * (1.0F - lerpVal) + sheepColors2[0] * lerpVal;
                                    green = sheepColors1[1] * (1.0F - lerpVal) + sheepColors2[1] * lerpVal;
                                    blue = sheepColors1[2] * (1.0F - lerpVal) + sheepColors2[2] * lerpVal;
                                }

                                armorScale = 1.04F;
                            }

                            if (wayY < 0) {
                                GLShim.glColor4f(red, green, blue, contact.brightness);
                            } else {
                                GLShim.glColor3f(red * contact.brightness, green * contact.brightness, blue * contact.brightness);
                            }
                        }

                        this.applyFilteringParameters();
                        GLUtils.drawPre();
                        GLUtils.setMap(icon, (float) x, (float) y + yOffset + armorOffset, (float) ((int) ((float) icon.getIconWidth() / 4.0F * armorScale)));
                        GLUtils.drawPost();
                        if (icon == this.clothIcon) {
                            if (wayY < 0) {
                                GLShim.glColor4f(1.0F, 1.0F, 1.0F, contact.brightness);
                            } else {
                                GLShim.glColor3f(contact.brightness, contact.brightness, contact.brightness);
                            }

                            icon = this.textureAtlas.getAtlasSprite("armor " + this.armorNames[2]);
                            this.applyFilteringParameters();
                            GLUtils.drawPre();
                            GLUtils.setMap(icon, (float) x, (float) y + yOffset + armorOffset, (float) icon.getIconWidth() / 4.0F * armorScale);
                            GLUtils.drawPost();
                            if (wayY < 0) {
                                GLShim.glColor4f(red, green, blue, contact.brightness);
                            } else {
                                GLShim.glColor3f(red * contact.brightness, green * contact.brightness, blue * contact.brightness);
                            }

                            icon = this.textureAtlas.getAtlasSprite("armor " + this.armorNames[1]);
                            this.applyFilteringParameters();
                            GLUtils.drawPre();
                            GLUtils.setMap(icon, (float) x, (float) y + yOffset + armorOffset, (float) icon.getIconWidth() / 4.0F * armorScale * 40.0F / 37.0F);
                            GLUtils.drawPost();
                            GLShim.glColor3f(1.0F, 1.0F, 1.0F);
                            icon = this.textureAtlas.getAtlasSprite("armor " + this.armorNames[3]);
                            this.applyFilteringParameters();
                            GLUtils.drawPre();
                            GLUtils.setMap(icon, (float) x, (float) y + yOffset + armorOffset, (float) icon.getIconWidth() / 4.0F * armorScale * 40.0F / 37.0F);
                            GLUtils.drawPost();
                        }
                    } else if (contact.uuid != null && contact.uuid.equals(this.devUUID)) {
                        Sprite icon = this.textureAtlas.getAtlasSprite("crown");
                        this.applyFilteringParameters();
                        GLUtils.drawPre();
                        GLUtils.setMap(icon, (float) x, (float) y + yOffset, (float) icon.getIconWidth() / 4.0F);
                        GLUtils.drawPost();
                    }

                    if (contact.name != null && (this.options.showPlayerNames && contact.type == EnumMobs.PLAYER || this.options.showMobNames && contact.type != EnumMobs.PLAYER)) {
                        float scaleFactor = (float) this.layoutVariables.scScale / this.options.fontScale;
                        matrixStack.scale(1.0F / scaleFactor, 1.0F / scaleFactor, 1.0F);
                        RenderSystem.applyModelViewMatrix();
                        int m = this.chkLen(contact.name) / 2;
                        this.write(contact.name, (float) x * scaleFactor - (float) m, (float) (y + 3) * scaleFactor, 16777215);
                    }
                } catch (Exception e) {
                    System.err.println("Error rendering mob icon! " + e.getLocalizedMessage() + " contact type " + contact.type);
                    logger.log(Level.ERROR, e);
                } finally {
                    matrixStack.pop();
                    RenderSystem.applyModelViewMatrix();
                }
            }
        }

    }

    private void applyFilteringParameters() {
        if (this.options.filtering) {
            GLShim.glTexParameteri(3553, 10241, 9729);
            GLShim.glTexParameteri(3553, 10240, 9729);
            GLShim.glTexParameteri(3553, 10242, 10496);
            GLShim.glTexParameteri(3553, 10243, 10496);
        } else {
            GLShim.glTexParameteri(3553, 10241, 9728);
            GLShim.glTexParameteri(3553, 10240, 9728);
        }

    }

    private boolean isHostile(Entity entity) {
        if (entity instanceof ZombifiedPiglinEntity) {
            ZombifiedPiglinEntity zombifiedPiglinEntity = (ZombifiedPiglinEntity) entity;
            return zombifiedPiglinEntity.isAngryAt(this.game.player);
        } else if (entity instanceof Monster) {
            return true;
        } else if (entity instanceof BeeEntity) {
            BeeEntity beeEntity = (BeeEntity) entity;
            return beeEntity.hasAngerTime();
        } else {
            if (entity instanceof PolarBearEntity) {
                PolarBearEntity polarBearEntity = (PolarBearEntity) entity;

                for (Object object : polarBearEntity.world.getNonSpectatingEntities(PolarBearEntity.class, polarBearEntity.getBoundingBox().expand(8.0, 4.0, 8.0))) {
                    if (((PolarBearEntity) object).isBaby()) {
                        return true;
                    }
                }
            }

            if (entity instanceof RabbitEntity) {
                RabbitEntity rabbitEntity = (RabbitEntity) entity;
                return rabbitEntity.getRabbitType() == 99;
            } else if (entity instanceof WolfEntity) {
                WolfEntity wolfEntity = (WolfEntity) entity;
                return wolfEntity.hasAngerTime();
            } else {
                return false;
            }
        }
    }

    private boolean isPlayer(Entity entity) {
        return entity instanceof OtherClientPlayerEntity;
    }

    private boolean isNeutral(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        } else {
            return !(entity instanceof PlayerEntity) && !this.isHostile(entity);
        }
    }

    private class ModelPartWithResourceLocation {
        ModelPart modelPart;
        Identifier resourceLocation;

        public ModelPartWithResourceLocation(ModelPart modelPart, Identifier resourceLocation) {
            this.modelPart = modelPart;
            this.resourceLocation = resourceLocation;
        }
    }
}
