package com.mamiyaotaru.voxelmap.entityrender.armors;

import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.Items;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class SheepOverlayHandler extends AbstractArmorHandler {
    private static final Identifier SHEEP_FUR = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_wool.png");
    private final SheepFurModel sheepFurModel;

    public SheepOverlayHandler() {
        sheepFurModel = new SheepFurModel(SheepFurModel.createFurLayer().bakeRoot());
    }

    @Override
    public EntityArmorData getArmorData() {
        if (!(entity instanceof Sheep sheep) || !(renderer instanceof SheepRenderer) || sheep.isBaby() || sheep.isSheared()) {
            return null;
        }

        return getOrCreateArmorData(Items.AIR, SHEEP_FUR, 0);
    }

    @Override
    public void renderArmorModel(EntityMapImageManager.CaptureContext context) {
        PoseStack pose = context.poseStack();
        BufferBuilder bufferBuilder = context.bufferBuilder();

        sheepFurModel.root().getChild("head").render(pose, bufferBuilder, EntityMapImageManager.LIGHT, EntityMapImageManager.OVERLAY, 0xFFFFFFFF);
    }

    @Override
    public BufferedImage postProcessTexture(BufferedImage image, EntityArmorData armorData) {
        image = ImageUtils.trim(image);

        Graphics2D g = image.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(2, 2, image.getWidth() - 4, image.getHeight() - 4);
        g.dispose();

        image = ImageUtils.fillOutline(ImageUtils.pad(image), addBorder, true, 28.0F, 28.0F, 2);

        return image;
    }
}
