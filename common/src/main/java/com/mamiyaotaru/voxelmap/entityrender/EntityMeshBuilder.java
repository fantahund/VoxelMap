package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.rendering.EmptySubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.animal.fish.CodModel;
import net.minecraft.client.model.animal.fish.SalmonModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.slime.MagmaCubeModel;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.model.monster.slime.SulfurCubeModel;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.model.monster.zombie.ZombieVillagerModel;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SkullBlock;

public class EntityMeshBuilder {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final RandomSource randomSource = RandomSource.create();
    private final EmptySubmitNodeCollector emptySubmitNodeCollector = new EmptySubmitNodeCollector();
    private final List<BlockStateModelPart> blockModelOutput = new ArrayList<>();
    private final Direction[] allDirections;
    private final Set<Class<?>> fullRenderModels;
    private final ModelPart humanoidModel;
    private final ModelPart slimeOuterModel;
    private final ModelPart sulfurCubeInnerModel;

    private static final int LIGHT = LightCoordsUtil.FULL_BRIGHT;
    private static final int OVERLAY = OverlayTexture.NO_OVERLAY;

    public EntityMeshBuilder() {
        allDirections = new Direction[]{null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

        fullRenderModels = Set.of(CodModel.class, MagmaCubeModel.class, SalmonModel.class, TropicalFishSmallModel.class, TropicalFishLargeModel.class);
        CubeDeformation armorInflate = new CubeDeformation(1.0F);
        LayerDefinition layerDefinition = LayerDefinition.create(HumanoidModel.createMesh(armorInflate, 0.0F), 64, 32);
        humanoidModel = layerDefinition.bakeRoot();
        slimeOuterModel = EntityModelSet.vanilla().bakeLayer(ModelLayers.SLIME_OUTER);
        sulfurCubeInnerModel = EntityModelSet.vanilla().bakeLayer(ModelLayers.SULFUR_CUBE_INNER);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void buildEntityMeshes(PoseStack matrix, VertexConsumer buffer, Entity entity, EntityRenderer renderer) {
        EntityRenderState renderState = renderer.createRenderState(entity, 0.5F);
        renderer.submit(renderState, matrix, emptySubmitNodeCollector, minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState);

        ModelPart[] modelParts = getPartToRender(renderer);
        if (modelParts != null) {
            for (ModelPart part : modelParts) {
                part.xRot = 0;
                part.yRot = 0;
                part.zRot = 0;
                part.render(matrix, buffer, LIGHT, OVERLAY, 0xFFFFFFFF);
            }
        }
    }

    public void buildArmorMeshes(PoseStack matrix, VertexConsumer buffer, ItemStack itemStack) {
        if (itemStack.getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() instanceof SkullBlock skullBlock) {
                matrix.scale(1.1875F, 1.1875F, 1.1875F);
                SkullModelBase skullModel = SkullBlockRenderer.createModel(EntityModelSet.vanilla(), skullBlock.getType());
                if (skullModel != null) {
                    skullModel.renderToBuffer(matrix, buffer, LIGHT, OVERLAY, 0xFFFFFFFF);
                }
            } else {
                matrix.mulPose(Axis.ZP.rotationDegrees(180.0F));
                matrix.scale(0.625F, 0.625F, 0.625F);
                BlockStateModel blockModel = minecraft.getModelManager().getBlockStateModelSet().get(blockItem.getBlock().defaultBlockState());
                renderBlockToBuffer(matrix, buffer, blockModel, LIGHT, OVERLAY, 0xFFFFFFFF);
            }
        } else if (itemStack.get(DataComponents.EQUIPPABLE) != null) {
            ModelPart part = humanoidModel.getChild("head");
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            part.render(matrix, buffer, LIGHT, OVERLAY, 0xFFFFFFFF);
        }
    }

    private void renderBlockToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, BlockStateModel model, int light, int overlay, int color) {
        PoseStack.Pose pose = poseStack.last();
        QuadInstance quadData = new QuadInstance();
        quadData.setLightCoords(light);
        quadData.setOverlayCoords(overlay);
        quadData.setColor(color);
        blockModelOutput.clear();
        model.collectParts(randomSource, blockModelOutput);
        for (BlockStateModelPart part : blockModelOutput) {
            for (Direction direction : allDirections) {
                for (BakedQuad quad : part.getQuads(direction)) {
                    vertexConsumer.putBakedQuad(pose, quad, quadData);
                }
            }
        }
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

        // model type based
        if (fullRenderModels.contains(model.getClass())) {
            return new ModelPart[]{model.root()};
        }

        switch (model) {
            // slimes
            case SlimeModel slimeModel -> {
                return new ModelPart[]{slimeModel.root(), slimeOuterModel};
            }
            case SulfurCubeModel sulfurCubeModel -> {
                return new ModelPart[]{sulfurCubeModel.root(), sulfurCubeInnerModel};
            }

            // wither
            case WitherBossModel witherModel -> {
                return new ModelPart[]{witherModel.root().getChild("left_head"), witherModel.root().getChild("center_head"), witherModel.root().getChild("right_head")};
            }

            // villagers
            case VillagerModel villagerModel -> {
                return new ModelPart[]{villagerModel.root().getChild("head"), villagerModel.root().getChild("head").getChild("hat")};
            }
            case ZombieVillagerModel<?> zombieVillagerModel -> {
                return new ModelPart[]{zombieVillagerModel.root().getChild("head"), zombieVillagerModel.root().getChild("head").getChild("hat")};
            }

            default -> {}
        }

        // model part based
        for (ModelPart part : model.allParts()) {
            // horses
            if (part.hasChild("head_parts")) {
                return new ModelPart[]{part.getChild("head_parts")};
            }

            // most mobs
            if (part.hasChild("head")) {
                if (part.hasChild("body0")) {
                    // spider
                    return new ModelPart[]{part.getChild("head"), part.getChild("body0")};
                }
                return new ModelPart[]{part.getChild("head")};
            }

            // bee, ghast
            if (part.hasChild("body")) {
                return new ModelPart[]{part.getChild("body")};
            }

            // bee, ghast, slime
            if (part.hasChild("cube")) {
                return new ModelPart[]{part.getChild("cube")};
            }

            // silverfish, endermite
            if (part.hasChild("segment0")) {
                return new ModelPart[]{part.getChild("segment0"), part.getChild("segment1")};
            }
        }

        // fallback
        return new ModelPart[]{model.root()};
    }
}
