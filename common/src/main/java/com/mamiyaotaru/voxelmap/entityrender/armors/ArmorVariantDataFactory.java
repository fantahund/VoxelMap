package com.mamiyaotaru.voxelmap.entityrender.armors;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.SkullBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ArmorVariantDataFactory {
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
    private final Identifier overlay0;
    private final int color0;
    private final Identifier overlay1;
    private final int color1;
    private final Identifier overlay2;
    private final int color2;

    public ArmorVariantDataFactory(Item type) {
        this(type, null, -1);
    }

    public ArmorVariantDataFactory(Item type, Identifier overlay0, int color0) {
        this(type, overlay0, color0, null, -1);
    }

    public ArmorVariantDataFactory(Item type, Identifier overlay0, int color0, Identifier overlay1, int color1) {
        this(type, overlay0, color0, overlay1, color1, null, -1);
    }

    public ArmorVariantDataFactory(Item type, Identifier overlay0, int color0, Identifier overlay1, int color1, Identifier overlay2, int color2) {
        this.type = type;
        this.overlay0 = overlay0;
        this.color0 = color0;
        this.overlay1 = overlay1;
        this.color1 = color1;
        this.overlay2 = overlay2;
        this.color2 = color2;
    }

    public Item getType() {
        return type;
    }

    public Identifier getOverlay0() {
        return overlay0;
    }

    public int getColor0() {
        return color0;
    }

    public Identifier getOverlay1() {
        return overlay1;
    }

    public int getColor1() {
        return color1;
    }

    public Identifier getOverlay2() {
        return overlay2;
    }

    public int getColor2() {
        return color2;
    }

    public ArmorVariantData create(ItemStack stack, String id, int size, boolean addBorder) {
        Identifier baseTexture = getBaseTexture(stack);
        int baseColor = getBaseColor(stack);
        return new ArmorVariantData(stack.getItem(), id, baseTexture, baseColor, overlay0, color0, overlay1, color1, overlay2, color2, size, addBorder);
    }

    public static ArmorVariantData createSimple(ItemStack stack, String id, int size, boolean addBorder) {
        Identifier baseTexture = getBaseTexture(stack);
        int baseColor = getBaseColor(stack);
        return new ArmorVariantData(stack.getItem(), id, baseTexture, baseColor, size, addBorder);
    }

    public static Identifier getBaseTexture(ItemStack stack) {
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile != null) {
            GameProfile gameProfile = profile.resolveProfile(VoxelConstants.getMinecraft().services().profileResolver()).join();
            Optional<PlayerSkin> optionalSkin = VoxelConstants.getMinecraft().getSkinManager().get(gameProfile).getNow(Optional.empty());
            if (optionalSkin.isPresent()) {
                return optionalSkin.get().body().texturePath();
            }
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() instanceof SkullBlock skullBlock) {
                return SKULL_TEXTURES.get(skullBlock.getType());
            }
            return VoxelConstants.getMinecraft().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).location();
        }

        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            EquipmentAssetManager armorManager = VoxelConstants.getMinecraft().getEntityRenderDispatcher().equipmentAssets;
            EquipmentClientInfo armorInfo = armorManager.get(equippable.assetId().get());

            return armorInfo.getLayers(EquipmentClientInfo.LayerType.HUMANOID).getFirst().getTextureLocation(EquipmentClientInfo.LayerType.HUMANOID);
        }

        return null;
    }

    public static int getBaseColor(ItemStack stack) {
        DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
        if (dyedColor != null) {
            return dyedColor.rgb() | 0xFF000000;
        }

        if (stack.getItem() == Items.LEATHER_HELMET) {
            return DyedItemColor.LEATHER_COLOR;
        }

        return 0xFFFFFFFF;
    }
}
