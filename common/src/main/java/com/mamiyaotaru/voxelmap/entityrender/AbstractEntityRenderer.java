package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.util.PropertyParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.function.Consumer;

public abstract class AbstractEntityRenderer {
    protected final Minecraft minecraft = Minecraft.getInstance();
    protected final ArrayList<ModelPart> modelParts = new ArrayList<>();
    public final PoseStack poseStack = new PoseStack();
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

    public void addMesh(ModelPart modelPart) {
        modelParts.add(modelPart);
    }

    public void clearMesh() {
        modelParts.clear();
    }

    public void enableCull(boolean flag) {
        cullEnabled = flag;
    }

    protected abstract void setupMatrix();

    @SuppressWarnings("rawtypes")
    public abstract void render(TextureSet textureSet, Consumer<BufferedImage> resultConsumer);

    public record TextureSet(Identifier primaryTexture, int primaryColor, Identifier secondaryTexture, int secondaryColor, Identifier tertiaryTexture, int tertiaryColor, Identifier quaternaryTexture, int quaternaryColor) { }
}
