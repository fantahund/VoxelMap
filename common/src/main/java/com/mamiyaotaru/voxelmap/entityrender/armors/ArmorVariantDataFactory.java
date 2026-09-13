package com.mamiyaotaru.voxelmap.entityrender.armors;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.SkullBlock;

public class ArmorVariantDataFactory {
    private static final ConcurrentHashMap<UUID, Identifier> SKIN_TEXTURES = new ConcurrentHashMap<>();

    private static final HashMap<SkullBlock.Type, Identifier> SKULL_TEXTURES = Maps.newHashMap(
            Map.ofEntries(
                    Map.entry(SkullBlock.Types.SKELETON, Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png")),
                    Map.entry(SkullBlock.Types.WITHER_SKELETON, Identifier.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png")),
                    Map.entry(SkullBlock.Types.PLAYER, Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png")),
                    Map.entry(SkullBlock.Types.ZOMBIE, Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png")),
                    Map.entry(SkullBlock.Types.CREEPER, Identifier.withDefaultNamespace("textures/entity/creeper.png")),
                    Map.entry(SkullBlock.Types.PIGLIN, Identifier.withDefaultNamespace("textures/entity/piglin/piglin.png")),
                    Map.entry(SkullBlock.Types.DRAGON, Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png"))
            )
    );

    private final Item type;
    private final Identifier tex1;
    private final int col1;
    private final Identifier tex2;
    private final int col2;
    private final Identifier tex3;
    private final int col3;

    public ArmorVariantDataFactory(Item type) {
        this(type, null, -1);
    }

    public ArmorVariantDataFactory(Item type, Identifier tex1, int col1) {
        this(type, tex1, col1, null, -1);
    }

    public ArmorVariantDataFactory(Item type, Identifier tex1, int col1, Identifier tex2, int col2) {
        this(type, tex1, col1, tex2, col2, null, -1);
    }

    public ArmorVariantDataFactory(Item type, Identifier tex1, int col1, Identifier tex2, int col2, Identifier tex3, int col3) {
        this.type = type;
        this.tex1 = tex1;
        this.col1 = col1;
        this.tex2 = tex2;
        this.col2 = col2;
        this.tex3 = tex3;
        this.col3 = col3;
    }

    public Item type() {
        return type;
    }

    public Identifier tex1() {
        return tex1;
    }

    public int col1() {
        return col1;
    }

    public Identifier tex2() {
        return tex2;
    }

    public int col2() {
        return col2;
    }

    public Identifier tex3() {
        return tex3;
    }

    public int col3() {
        return col3;
    }

    public ArmorVariantData create(ItemStack stack, String id, boolean addBorder) {
        Identifier tex0 = loadBaseTexture(stack);
        int col0 = getBaseColor(stack);
        return new ArmorVariantData(stack.getItem(), id, tex0, col0, tex1, col1, tex2, col2, tex3, col3, addBorder);
    }

    public static ArmorVariantData createSimple(ItemStack stack, String id, boolean addBorder) {
        Identifier tex0 = loadBaseTexture(stack);
        int col0 = getBaseColor(stack);
        return new ArmorVariantData(stack.getItem(), id, tex0, col0, addBorder);
    }

    public static Identifier loadBaseTexture(ItemStack stack) {
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile != null) {
            return loadSkinTexture(profile);
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() instanceof SkullBlock skullBlock) {
                return SKULL_TEXTURES.get(skullBlock.getType());
            }
            return VoxelConstants.getMinecraft().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).location();
        }

        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            List<EquipmentClientInfo.Layer> layers = getArmorLayers(equippable, EquipmentClientInfo.LayerType.HUMANOID);
            if (!layers.isEmpty()) {
                return layers.getFirst().getTextureLocation(EquipmentClientInfo.LayerType.HUMANOID);
            }
        }

        return null;
    }

    public static int getBaseColor(ItemStack stack) {
        DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
        if (dyedColor != null) {
            return dyedColor.rgb() | 0xFF000000;
        }

        // I know it's a bit messy, but performance is first.
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            List<EquipmentClientInfo.Layer> layers = getArmorLayers(equippable, EquipmentClientInfo.LayerType.HUMANOID);
            if (!layers.isEmpty()) {
                Optional<EquipmentClientInfo.Dyeable> dyeable = layers.getFirst().dyeable();
                if (dyeable.isPresent()) {
                    Optional<Integer> undyedColor = dyeable.get().colorWhenUndyed();
                    if (undyedColor.isPresent()) {
                        return undyedColor.get() | 0xFF000000;
                    }
                }
            }
        }

        return 0xFFFFFFFF;
    }

    private static List<EquipmentClientInfo.Layer> getArmorLayers(Equippable equippable, EquipmentClientInfo.LayerType layerType) {
        Optional<ResourceKey<EquipmentAsset>> assetId = equippable.assetId();
        if (assetId.isPresent()) {
            EquipmentAssetManager armorManager = VoxelConstants.getMinecraft().getEntityRenderDispatcher().equipmentAssets;
            return armorManager.get(assetId.get()).getLayers(layerType);
        }
        return List.of();
    }

    private static Identifier loadSkinTexture(ResolvableProfile profile) {
        UUID uuid = profile.partialProfile().id();

        if (SKIN_TEXTURES.containsKey(uuid)) {
            return SKIN_TEXTURES.get(uuid);
        }

        Identifier defaultSkin = DefaultPlayerSkin.get(uuid).body().texturePath();
        SKIN_TEXTURES.put(uuid, defaultSkin);

        ProfileResolver profileResolver = VoxelConstants.getMinecraft().services().profileResolver();
        profile.resolveProfile(profileResolver).thenAccept((profile2) -> {
            SkinManager skinManager = VoxelConstants.getMinecraft().getSkinManager();
            skinManager.get(profile2).thenAccept((optionalSkin) -> {
                optionalSkin.ifPresent(skin -> SKIN_TEXTURES.put(uuid, skin.body().texturePath()));
            });
        });

        return defaultSkin;
    }
}
