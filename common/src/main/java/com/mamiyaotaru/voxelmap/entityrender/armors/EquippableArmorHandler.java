package com.mamiyaotaru.voxelmap.entityrender.armors;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SkullBlock;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EquippableArmorHandler extends AbstractArmorHandler {
    private final RandomSource random = RandomSource.create();
    private final HumanoidModel<?> humanoidModel;
    private final Direction[] allDirections;

    private Equippable armor;
    private Block block;
    private SkullBlock skull;

    private static final HashMap<SkullBlock.Type, Identifier> SKULL_TEXTURES = Maps.newHashMap(
            Map.ofEntries(
                    Map.entry(SkullBlock.Types.SKELETON, Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png")),
                    Map.entry(SkullBlock.Types.WITHER_SKELETON, Identifier.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png")),
                    Map.entry(SkullBlock.Types.ZOMBIE, Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png")),
                    Map.entry(SkullBlock.Types.CREEPER, Identifier.withDefaultNamespace("textures/entity/creeper.png")),
                    Map.entry(SkullBlock.Types.PIGLIN, Identifier.withDefaultNamespace("textures/entity/piglin/piglin.png")),
                    Map.entry(SkullBlock.Types.DRAGON, Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png"))
            )
    );

    public EquippableArmorHandler() {
        CubeDeformation armorInflate = new CubeDeformation(1.0F);
        LayerDefinition layerDefinition = LayerDefinition.create(HumanoidModel.createMesh(armorInflate, 0.0F), 64, 32);
        humanoidModel = new HumanoidModel<>(layerDefinition.bakeRoot());

        allDirections = new Direction[Direction.values().length + 1];
        int i = 1;
        for (Direction direction : Direction.values()) {
            allDirections[i++] = direction;
        }
    }

    @Override
    public EntityArmorData getArmorData() {
        if (!(entity instanceof LivingEntity livingEntity) || !(renderer instanceof LivingEntityRenderer livingRenderer) || !(livingRenderer.getModel() instanceof HumanoidModel)) {
            return null;
        }

        ItemStack itemStack = livingEntity.getItemBySlot(EquipmentSlot.HEAD);

        boolean isValid = resolveArmor(itemStack);
        if (!isValid) {
            return null;
        }

        EntityArmorData armorData = getArmorData(itemStack.getItem(), size, addBorder);
        if (armorData == null) {
            Identifier texture = resolveTexture(itemStack);
            if (texture != null) {
                armorData = getOrCreateArmorData(itemStack.getItem(), texture, size, addBorder);
            }
        }

        return armorData;
    }

    private boolean resolveArmor(ItemStack itemStack) {
        armor = null;
        block = null;
        skull = null;

        if (itemStack.isEmpty()) {
            return false;
        }

        boolean isValid = false;

        Equippable tempArmor = itemStack.get(DataComponents.EQUIPPABLE);
        if (tempArmor != null && tempArmor.assetId().isPresent()) {
            armor = tempArmor;
            isValid = true;
        }

        Block tempBlock = (itemStack.getItem() instanceof BlockItem bi) ? bi.getBlock() : null;
        if (tempBlock != null && tempBlock.defaultBlockState().getRenderShape() == RenderShape.MODEL) {
            armor = null;
            block = tempBlock;
            isValid = true;
        }

        if (block instanceof SkullBlock tempSkull) {
            block = null;
            skull = tempSkull;
            isValid = true;
        }

        return isValid;
    }

    private Identifier resolveTexture(ItemStack itemStack) {
        Identifier texture = null;
        if (armor != null) {
            EquipmentAssetManager armorManager = VoxelConstants.getMinecraft().getEntityRenderDispatcher().equipmentAssets;
            EquipmentClientInfo armorInfo = armorManager.get(armor.assetId().get());

            texture = armorInfo.getLayers(LayerType.HUMANOID).getFirst().getTextureLocation(LayerType.HUMANOID);
        }
        if (block != null) {
            texture = VoxelConstants.getMinecraft().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).location();
        }
        if (skull != null) {
            ResolvableProfile profileItem = itemStack.get(DataComponents.PROFILE);
            if (profileItem == null) {
                texture = SKULL_TEXTURES.get(skull.getType());
            } else {
//                FIXME 1.21.11: handle player skull texture
//                This does not work properly.
//
//                GameProfile profile = profileItem.resolveProfile(VoxelConstants.getMinecraft().services().profileResolver()).getNow(profileItem.partialProfile());
//                Optional<PlayerSkin> optionalSkin = VoxelConstants.getMinecraft().getSkinManager().get(profile).getNow(Optional.empty());
//
//                texture = optionalSkin.map(playerSkin -> playerSkin.body().texturePath()).orElseGet(DefaultPlayerSkin::getDefaultTexture);
            }
        }

        return texture;
    }

    @Override
    public void renderArmorModel(EntityMapImageManager.CaptureContext context) {
        PoseStack pose = context.poseStack();
        BufferBuilder bufferBuilder = context.bufferBuilder();

        if (armor != null) {
            ModelPart part = humanoidModel.root().getChild("head");
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            part.render(pose, bufferBuilder, EntityMapImageManager.LIGHT, EntityMapImageManager.OVERLAY, 0xFFFFFFFF);
        }
        if (block != null) {
            pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
            pose.scale(0.625F, 0.625F, 0.625F);

            BlockStateModelSet blockModelSet = VoxelConstants.getMinecraft().getModelManager().getBlockStateModelSet();
            ArrayList<BlockStateModelPart> allQuads = new ArrayList<>();
            blockModelSet.get(block.defaultBlockState()).collectParts(random, allQuads);

            QuadInstance quadInstance = new QuadInstance();
            quadInstance.setLightCoords(EntityMapImageManager.LIGHT);
            quadInstance.setOverlayCoords(EntityMapImageManager.OVERLAY);
            quadInstance.setColor(0xFFFFFFFF);

            for (BlockStateModelPart modelPart : allQuads) {
                for (Direction direction : allDirections) {
                    for (BakedQuad quad : modelPart.getQuads(direction)) {
                        bufferBuilder.putBakedQuad(pose.last(), quad, quadInstance);
                    }
                }
            }
        }
        if (skull != null) {
            pose.scale(1.1875F, 1.1875F, 1.1875F);
            SkullModelBase skullModel = SkullBlockRenderer.createModel(EntityModelSet.vanilla(), skull.getType());
            skullModel.renderToBuffer(pose, bufferBuilder, EntityMapImageManager.LIGHT, EntityMapImageManager.OVERLAY, 0xFFFFFFFF);
        }
    }

    @Override
    public BufferedImage postProcessTexture(BufferedImage image) {
        image = ImageUtils.trim(image);

        if (armor != null) {
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getWidth(), image.getType());
            newImage = ImageUtils.addImages(newImage, image, 0, 0, image.getWidth(), image.getHeight());

            image = newImage;
        }

        image = ImageUtils.fillOutline(ImageUtils.pad(image), addBorder, true, 37.5F, 37.5F, 2);

        return image;
    }
}
