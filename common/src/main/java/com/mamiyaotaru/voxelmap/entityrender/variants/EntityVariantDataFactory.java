package com.mamiyaotaru.voxelmap.entityrender.variants;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EntityVariantDataFactory {
    private final EntityType<?> type;
    private final Identifier overlay0;
    private final int color0;
    private final Identifier overlay1;
    private final int color1;
    private final Identifier overlay2;
    private final int color2;

    public EntityVariantDataFactory(EntityType<?> type) {
        this(type, null, -1);
    }

    public EntityVariantDataFactory(EntityType<?> type, Identifier overlay0, int color0) {
        this(type, overlay0, color0, null, -1);
    }

    public EntityVariantDataFactory(EntityType<?> type, Identifier overlay0, int color0, Identifier overlay1, int color1) {
        this(type, overlay0, color0, overlay1, color1, null, -1);
    }

    public EntityVariantDataFactory(EntityType<?> type, Identifier overlay0, int color0, Identifier overlay1, int color1, Identifier overlay2, int color2) {
        this.type = type;
        this.overlay0 = overlay0;
        this.color0 = color0;
        this.overlay1 = overlay1;
        this.color1 = color1;
        this.overlay2 = overlay2;
        this.color2 = color2;
    }

    public EntityType<?> getType() {
        return type;
    }

    public Identifier getOverlay0() {
        return overlay0;
    }

    public int getColor0() {
        return color0;
    }

    public Identifier getOverlay1() {
        return overlay1;
    }

    public int getColor1() {
        return color1;
    }

    public Identifier getOverlay2() {
        return overlay2;
    }

    public int getColor2() {
        return color2;
    }

    @SuppressWarnings("rawtypes")
    public EntityVariantData create(Entity entity, EntityRenderer renderer, EntityRenderState state, String id, int size, boolean addBorder) {
        Identifier baseTexture = getBaseTexture(renderer, state);
        return new EntityVariantData(entity.getType(), id, baseTexture, 0xFFFFFFFF, overlay0, color0, overlay1, color1, overlay2, color2, size, addBorder);
    }

    @SuppressWarnings("rawtypes")
    public static EntityVariantData createSimple(Entity entity, EntityRenderer renderer, EntityRenderState state, String id, int size, boolean addBorder) {
        Identifier baseTexture = getBaseTexture(renderer, state);
        return new EntityVariantData(entity.getType(), id, baseTexture, 0xFFFFFFFF, size, addBorder);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Identifier getBaseTexture(EntityRenderer renderer, EntityRenderState state) {
        return ((LivingEntityRenderer) renderer).getTextureLocation((LivingEntityRenderState) state);
    }
}
