package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.util.PropertyParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

public abstract class AbstractEntityRenderer {
    protected final Minecraft minecraft = Minecraft.getInstance();
    protected final ArrayList<ModelPart> modelParts = new ArrayList<>();
    protected final ArrayList<BlockModelSet> blockModels = new ArrayList<>();
    protected final PoseStack poseStack = new PoseStack();
    protected final RandomSource random = RandomSource.create();
    protected boolean cullEnabled = false;

    public static final int TEXTURE_SIZE = 512;

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
        modelParts.add(modelPart);
    }

    public void addBlock(BlockState blockState) {
        blockModels.add(new BlockModelSet(blockState, minecraft.getBlockRenderer().getBlockModel(blockState).collectParts(random)));
    }

    public void clearMesh() {
        modelParts.clear();
        blockModels.clear();
    }

    public void enableCull(boolean flag) {
        cullEnabled = flag;
    }

    protected abstract void setupMatrix();

    public abstract void render(TextureSet textureSet, Consumer<BufferedImage> resultConsumer);

    public record TextureSet(Identifier primaryTexture, int primaryColor, Identifier secondaryTexture, int secondaryColor, Identifier tertiaryTexture, int tertiaryColor, Identifier quaternaryTexture, int quaternaryColor) {
    }

    public record BlockModelSet(BlockState blockState, List<BlockModelPart> modelParts) {
    }
}
