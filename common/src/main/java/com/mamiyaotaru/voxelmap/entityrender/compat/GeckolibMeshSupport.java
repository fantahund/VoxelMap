package com.mamiyaotaru.voxelmap.entityrender.compat;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Touches GeckoLib types directly, so it must only be loaded once GeckoLib is known to be present.
 * Go through {@link GeckolibCompat}, never through this class.
 */
final class GeckolibMeshSupport {
    /**
     * Bone names to try, in order, when looking for something head-shaped. GeckoLib bone names are
     * chosen by the model author, so this is a convention rather than a guarantee; models that use
     * none of these fall back to rendering the whole entity.
     */
    private static final String[] HEAD_BONE_NAMES = {"head", "Head", "head_parts", "neck", "neck1"};

    private static final int LIGHT = LightCoordsUtil.FULL_BRIGHT;
    private static final int OVERLAY = OverlayTexture.NO_OVERLAY;
    private static final int COLOR = 0xFFFFFFFF;

    private GeckolibMeshSupport() {
    }

    @SuppressWarnings("rawtypes")
    static boolean isGeoRenderer(EntityRenderer renderer) {
        return renderer instanceof GeoRenderer;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    static Identifier getTextureLocation(EntityRenderer renderer, EntityRenderState renderState) {
        if (!(renderState instanceof GeoRenderState geoRenderState)) {
            return null;
        }
        return ((GeoRenderer) renderer).getTextureLocation(geoRenderState);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static boolean buildEntityMeshes(PoseStack matrix, VertexConsumer buffer, Entity entity, EntityRenderer renderer) {
        if (!(renderer.createRenderState(entity, 0.5F) instanceof GeoRenderState renderState)) {
            return false;
        }

        RenderPassInfo renderPass = RenderPassInfo.create((GeoRenderer) renderer, renderState, matrix,
                Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState, true);

        BakedGeoModel model = renderPass.model();
        if (model == null || model.isMissingno()) {
            return false;
        }

        matrix.pushPose();

        // GeckoLib geometry is drawn upside down without this. Normally GeoRenderer#adjustRenderPose
        // gets there first and ends with the same flip, but that also applies the entity's yaw, pitch
        // and death angle, which an icon must not have - so apply just the flip by hand.
        matrix.rotateDegrees(Axis.ZP, 180.0F);

        // No animations are applied, so the model renders in its bind pose. That matches what the
        // vanilla path does, which zeroes out the head part's rotations before drawing it.
        GeoBone head = findHeadBone(model);
        if (head != null) {
            head.positionAndRender(renderPass, buffer, LIGHT, OVERLAY, COLOR);
        } else {
            model.render(renderPass, buffer, LIGHT, OVERLAY, COLOR);
        }

        matrix.popPose();

        return true;
    }

    @Nullable
    private static GeoBone findHeadBone(BakedGeoModel model) {
        for (String name : HEAD_BONE_NAMES) {
            GeoBone bone = model.getBone(name).orElse(null);
            if (bone != null) {
                return bone;
            }
        }
        return null;
    }
}
