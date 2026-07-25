package com.mamiyaotaru.voxelmap.entityrender.armors;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    public ArmorVariantData create(ItemStack stack, String id, int size, boolean addBorder) {
        Identifier tex0 = loadBaseTexture(stack);
        int col0 = getBaseColor(stack);
        return new ArmorVariantData(stack.getItem(), id, tex0, col0, tex1, col1, tex2, col2, tex3, col3, size, addBorder);
    }

    public static ArmorVariantData createSimple(ItemStack stack, String id, int size, boolean addBorder) {
        Identifier tex0 = loadBaseTexture(stack);
        int col0 = getBaseColor(stack);
        return new ArmorVariantData(stack.getItem(), id, tex0, col0, size, addBorder);
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

    public static Identifier loadBaseTexture(ItemStack stack) {
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
}
