package com.mamiyaotaru.voxelmap.entityrender.armors;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import com.mamiyaotaru.voxelmap.util.EmptySubmitNodeCollector;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.TropicalFishRenderer;
import net.minecraft.client.renderer.entity.state.TropicalFishRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.fish.TropicalFish;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

public class TropicalFishOverlayHandler extends AbstractArmorHandler {
    private static final EnumMap<TropicalFish.Pattern, Identifier> PATTERN_TEXTURES = Maps.newEnumMap(
            Map.ofEntries(
                    Map.entry(TropicalFish.Pattern.KOB, Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_1.png")),
                    Map.entry(TropicalFish.Pattern.SUNSTREAK, Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_2.png")),
                    Map.entry(TropicalFish.Pattern.SNOOPER, Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_3.png")),
                    Map.entry(TropicalFish.Pattern.DASHER, Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_4.png")),
                    Map.entry(TropicalFish.Pattern.BRINELY, Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_5.png")),
                    Map.entry(TropicalFish.Pattern.SPOTTY, Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_6.png")),
                    Map.entry(TropicalFish.Pattern.FLOPPER, Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_1.png")),
                    Map.entry(TropicalFish.Pattern.STRIPEY, Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_2.png")),
                    Map.entry(TropicalFish.Pattern.GLITTER, Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_3.png")),
                    Map.entry(TropicalFish.Pattern.BLOCKFISH, Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_4.png")),
                    Map.entry(TropicalFish.Pattern.BETTY, Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_5.png")),
                    Map.entry(TropicalFish.Pattern.CLAYFISH, Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_6.png"))
            )
    );
    private final EmptySubmitNodeCollector emptySubmitNodeCollector = new EmptySubmitNodeCollector();

    @Override
    public EntityArmorData getArmorData() {
        if (!(entity instanceof TropicalFish fish) || !(renderer instanceof TropicalFishRenderer)) {
            return null;
        }
        Identifier pattern = PATTERN_TEXTURES.get(fish.getPattern());

        return getOrCreateArmorData(pattern, pattern, size, addBorder);
    }

    @Override
    public void renderArmorModel(EntityMapImageManager.CaptureContext context) {
        PoseStack pose = context.poseStack();
        BufferBuilder bufferBuilder = context.bufferBuilder();

        TropicalFish fish = (TropicalFish) entity;
        TropicalFishRenderer fishRenderer = (TropicalFishRenderer) renderer;

        // Initialize model
        TropicalFishRenderState renderState = fishRenderer.createRenderState(fish, 0.5F);
        fishRenderer.submit(renderState, pose, emptySubmitNodeCollector, VoxelConstants.getMinecraft().gameRenderer.getLevelRenderState().cameraRenderState);

        fishRenderer.getModel().root().render(pose, bufferBuilder, EntityMapImageManager.LIGHT, EntityMapImageManager.OVERLAY, 0xFFFFFFFF);
    }

    @Override
    public BufferedImage postProcessTexture(BufferedImage image) {
        image = customTrim(image);
        return image;
    }

    public static BufferedImage customTrim(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage base = new BufferedImage(width, height, image.getType());
        Graphics2D g = base.createGraphics();
        g.drawImage(image, 0, -86, width, height, null);
        g.dispose();
        base = ImageUtils.trimCentered(base);

        return base;
    }
}
