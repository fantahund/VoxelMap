package com.mamiyaotaru.voxelmap.entityrender.compat;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Entry point for GeckoLib support. GeckoLib is a compile-only dependency, so this class must not
 * reference any GeckoLib type directly - everything that does lives in {@link GeckolibMeshSupport},
 * which is only ever loaded once {@link #isAvailable()} has confirmed GeckoLib is on the classpath.
 */
public final class GeckolibCompat {
    private static final String GEO_RENDERER_CLASS = "com.geckolib.renderer.base.GeoRenderer";

    private static final boolean AVAILABLE = isClassPresent(GEO_RENDERER_CLASS);

    private GeckolibCompat() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /** True if this renderer draws through GeckoLib rather than through vanilla model parts. */
    @SuppressWarnings("rawtypes")
    public static boolean isGeoRenderer(EntityRenderer renderer) {
        return AVAILABLE && GeckolibMeshSupport.isGeoRenderer(renderer);
    }

    /** The texture GeckoLib would bind for this render state, or null if it cannot be determined. */
    @SuppressWarnings("rawtypes")
    @Nullable
    public static Identifier getTextureLocation(EntityRenderer renderer, EntityRenderState renderState) {
        if (!isGeoRenderer(renderer)) {
            return null;
        }
        try {
            return GeckolibMeshSupport.getTextureLocation(renderer, renderState);
        } catch (Exception e) {
            VoxelConstants.getLogger().warn("Failed to read GeckoLib texture for radar icon", e);
            return null;
        }
    }

    /**
     * Writes the entity's head geometry (or the whole model, if it has no recognisable head bone)
     * into the given buffer. Returns false if nothing was drawn.
     */
    @SuppressWarnings("rawtypes")
    public static boolean buildEntityMeshes(PoseStack matrix, VertexConsumer buffer, Entity entity, EntityRenderer renderer) {
        if (!isGeoRenderer(renderer)) {
            return false;
        }
        try {
            return GeckolibMeshSupport.buildEntityMeshes(matrix, buffer, entity, renderer);
        } catch (Exception e) {
            VoxelConstants.getLogger().warn("Failed to build GeckoLib mesh for radar icon", e);
            return false;
        }
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, GeckolibCompat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
