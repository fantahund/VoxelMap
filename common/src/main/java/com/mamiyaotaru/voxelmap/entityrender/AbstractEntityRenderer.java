package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.util.PropertyParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractEntityRenderer {
    protected final Minecraft minecraft = Minecraft.getInstance();
    protected final ArrayList<ModelPartRenderTask> modelParts = new ArrayList<>();
    protected final ArrayList<BlockModelSet> blockModels = new ArrayList<>();
    protected final PoseStack poseStack = new PoseStack();
    protected final RandomSource random = RandomSource.create();
    protected boolean cullEnabled = false;

    public static final int TEXTURE_SIZE = 512;
    public static final Direction[] ALL_DIRECTIONS = new Direction[] { null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

    public void setup(Properties iconConfig) {
        poseStack.setIdentity();
        setupMatrix();
        LinkedHashMap<Direction.Axis, Float> rotation = PropertyParser.parseVector(iconConfig.getProperty("rotation", ""));
        if (rotation != null && !rotation.isEmpty()) {
            rotation.forEach((axis, value) -> {
                switch (axis) {
                    case Direction.Axis.X -> poseStack.mulPose(Axis.XP.rotationDegrees(value));
                    case Direction.Axis.Y -> poseStack.mulPose(Axis.YP.rotationDegrees(value));
                    case Direction.Axis.Z -> poseStack.mulPose(Axis.ZP.rotationDegrees(value));
                }
            });
        }
        clearMesh();
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public void addMesh(ModelPart modelPart) {
        addMesh(modelPart, List.of());
    }

    public void addMesh(ModelPart modelPart, List<ModelPart> ancestors) {
        addMesh(modelPart, ancestors, true);
    }

    public void addMesh(ModelPart modelPart, List<ModelPart> ancestors, boolean includeChildren) {
        modelParts.add(new ModelPartRenderTask(modelPart, ancestors, includeChildren));
    }

    public void addBlock(BlockState blockState) {
        ArrayList<BlockStateModelPart> modelParts = new ArrayList<>();
        minecraft.getModelManager().getBlockStateModelSet().get(blockState).collectParts(random, modelParts);

        blockModels.add(new BlockModelSet(blockState, modelParts));
    }

    public void clearMesh() {
        modelParts.clear();
        blockModels.clear();
    }

    public void enableCull(boolean flag) {
        cullEnabled = flag;
    }

    protected abstract void setupMatrix();

    protected void visitModelPart(ModelPartRenderTask renderTask, ModelPart.Visitor visitor) {
        poseStack.pushPose();
        try {
            for (ModelPart ancestor : renderTask.ancestors()) {
                ancestor.translateAndRotate(poseStack);
            }
            visitModelPart(renderTask.modelPart(), "", renderTask.includeChildren(), visitor);
        } finally {
            poseStack.popPose();
        }
    }

    private void visitModelPart(ModelPart modelPart, String path, boolean includeChildren, ModelPart.Visitor visitor) {
        if (!modelPart.visible || modelPart.cubes.isEmpty() && modelPart.children.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        try {
            modelPart.translateAndRotate(poseStack);
            PoseStack.Pose pose = poseStack.last();

            if (!modelPart.skipDraw) {
                for (int i = 0; i < modelPart.cubes.size(); i++) {
                    visitor.visit(pose, path, i, modelPart.cubes.get(i));
                }
            }

            if (includeChildren) {
                String childPath = path + "/";
                modelPart.children.forEach((name, child) -> visitModelPart(child, childPath + name, true, visitor));
            }
        } finally {
            poseStack.popPose();
        }
    }

    public abstract void render(TextureSet textureSet, Consumer<BufferedImage> resultConsumer);

    public record TextureSet(Identifier primaryTexture, int primaryColor, Identifier secondaryTexture, int secondaryColor, Identifier tertiaryTexture, int tertiaryColor, Identifier quaternaryTexture, int quaternaryColor) {
    }

    public record BlockModelSet(BlockState blockState, List<BlockStateModelPart> modelParts) {
    }

    protected record ModelPartRenderTask(ModelPart modelPart, List<ModelPart> ancestors, boolean includeChildren) {
        public ModelPartRenderTask {
            ancestors = List.copyOf(ancestors);
        }
    }
}
