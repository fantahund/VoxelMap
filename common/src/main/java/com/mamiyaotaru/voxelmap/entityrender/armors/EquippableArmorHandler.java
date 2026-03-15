package com.mamiyaotaru.voxelmap.entityrender.armors;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Optional;

public class EquippableArmorHandler extends AbstractArmorHandler {
    private final RandomSource random = RandomSource.create();
    private final HumanoidModel<?> humanoidModel;
    private final Direction[] allDirections;

    private Equippable equippable;
    private Block block;

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
        if (itemStack.isEmpty()) {
            return null;
        }

        EntityArmorData armorData = null;

        // get item texture
        if ((equippable = itemStack.get(DataComponents.EQUIPPABLE)) != null) {
            Optional<ResourceKey<EquipmentAsset>> assetId = equippable.assetId();
            if (assetId.isPresent()) {
                Identifier material = assetId.get().identifier();
                armorData = getArmorData(material, size, addBorder);
                if (armorData == null) {
                    Identifier texture = Identifier.fromNamespaceAndPath(material.getNamespace(), "textures/entity/equipment/humanoid/" + material.getPath() + ".png");
                    armorData = getOrCreateArmorData(material, texture, size, addBorder);
                }
            }
        } else if (itemStack.getItem() instanceof BlockItem blockItem) {
            Block tempBlock = blockItem.getBlock();
            if (tempBlock.defaultBlockState().getRenderShape() == RenderShape.MODEL) {
                block = tempBlock;
                Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
                armorData = getArmorData(blockId, size, addBorder);
                if (armorData == null) {
                    Identifier texture = VoxelConstants.getMinecraft().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).location();
                    armorData = getOrCreateArmorData(blockId, texture, size, addBorder);
                }
            }
        }

        return armorData;
    }

    @Override
    public void renderArmorModel(EntityMapImageManager.CaptureContext context) {
        PoseStack pose = context.poseStack();
        BufferBuilder bufferBuilder = context.bufferBuilder();

        if (equippable != null) {
            ModelPart part = humanoidModel.root().getChild("head");
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            part.render(pose, bufferBuilder, EntityMapImageManager.LIGHT, EntityMapImageManager.OVERLAY, 0xFFFFFFFF);
        }
        if (block != null) {
            pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
            pose.scale(0.65F, 0.65F, 0.65F);

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
    }

    @Override
    public BufferedImage postProcessTexture(BufferedImage image) {
        image = ImageUtils.trim(image);

        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getWidth(), image.getType());
        newImage = ImageUtils.addImages(newImage, image, 0, 0, image.getWidth(), image.getHeight());
        newImage = ImageUtils.fillOutline(ImageUtils.pad(newImage), addBorder, true, 37.5F, 37.5F, 2);

        return newImage;
    }
}
